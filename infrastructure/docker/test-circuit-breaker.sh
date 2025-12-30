#!/bin/bash

# Script per testare Circuit Breaker di Order Service
# Testa il flow completo: CLOSED → OPEN → HALF_OPEN → CLOSED

set -e

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurazione
GATEWAY_URL="http://localhost:8080"
SERVICE_NAME="product-service"
CIRCUIT_BREAKER_NAME="productService"

# Funzione per stampare con colore
print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Funzione per estrarre token dal JSON (senza jq)
extract_token() {
    echo "$1" | grep -o '"token":"[^"]*' | cut -d'"' -f4
}

# Funzione per ottenere il token JWT
get_jwt_token() {
    print_step "Login per ottenere JWT token..."
    
    # Prova con utente esistente
    RESPONSE=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "usernameOrEmail": "admin",
            "password": "admin123"
        }')
    
    TOKEN=$(extract_token "$RESPONSE")
    
    if [ -z "$TOKEN" ]; then
        print_error "Login fallito. Creo nuovo utente admin..."
        
        # Registra nuovo admin (se non esiste)
        curl -s -X POST "${GATEWAY_URL}/api/auth/register" \
            -H "Content-Type: application/json" \
            -d '{
                "username": "testadmin",
                "email": "testadmin@test.com",
                "password": "password123",
                "firstName": "Test",
                "lastName": "Admin"
            }' > /dev/null
        
        # Login con nuovo utente
        RESPONSE=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
            -H "Content-Type: application/json" \
            -d '{
                "usernameOrEmail": "testadmin",
                "password": "password123"
            }')
        
        TOKEN=$(extract_token "$RESPONSE")
    fi
    
    if [ -z "$TOKEN" ]; then
        print_error "Impossibile ottenere token JWT"
        exit 1
    fi
    
    print_success "Token JWT ottenuto"
}

# Funzione per fare una richiesta
make_request() {
    local description=$1
    local expected_status=$2
    
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        -X GET "${GATEWAY_URL}/api/orders/1" \
        -H "Authorization: Bearer ${TOKEN}")
    
    if [ "$STATUS" -eq "$expected_status" ]; then
        print_success "$description - Status: $STATUS"
        return 0
    else
        print_error "$description - Expected: $expected_status, Got: $STATUS"
        return 1
    fi
}

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       TEST CIRCUIT BREAKER - ORDER SERVICE                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Login e ottenimento token
get_jwt_token

echo ""
print_step "FASE 1: Verifica stato CLOSED (normale funzionamento)"
echo "─────────────────────────────────────────────────────────────"

# Verifica che i servizi siano UP
print_step "Verifica che tutti i servizi siano running..."
if ! docker compose ps | grep -q "product-service.*Up"; then
    print_warning "Product service non è running, lo avvio..."
    docker compose start product-service
    sleep 3
fi
print_success "Servizi verificati"

# Test richieste normali
print_step "Invio 3 richieste normali (dovrebbero andare a buon fine)..."
for i in {1..3}; do
    make_request "Richiesta $i/3" 200
    sleep 0.5
done

echo ""
print_step "FASE 2: Trigger Circuit Breaker OPEN"
echo "─────────────────────────────────────────────────────────────"

# Stoppa il Product Service per causare failures
print_step "Fermo il Product Service per simulare failures..."
docker compose stop product-service
print_success "Product Service fermato"

sleep 2

# Fai 6 richieste che falliranno (slidingWindow=10, failureRate=50%)
print_step "Invio 6 richieste che falliranno (trigger Circuit Breaker)..."
FAILED=0
for i in {1..6}; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        -X GET "${GATEWAY_URL}/api/orders/1" \
        -H "Authorization: Bearer ${TOKEN}")
    
    if [ "$STATUS" -eq 500 ] || [ "$STATUS" -eq 503 ]; then
        FAILED=$((FAILED + 1))
        echo "  Richiesta $i/6 - Status: $STATUS (failure atteso)"
    else
        print_warning "Richiesta $i/6 - Status: $STATUS (inaspettato)"
    fi
    sleep 0.5
done

print_success "Completate $FAILED/6 richieste fallite"

sleep 1

echo ""
print_step "FASE 3: Verifica Circuit Breaker OPEN"
echo "─────────────────────────────────────────────────────────────"

# Verifica che il Circuit Breaker sia OPEN
print_step "Verifica che le richieste falliscano IMMEDIATAMENTE..."
START=$(date +%s%N)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "${GATEWAY_URL}/api/orders/1" \
    -H "Authorization: Bearer ${TOKEN}")
END=$(date +%s%N)
DURATION=$(( (END - START) / 1000000 )) # Converti a millisecondi

if [ "$STATUS" -eq 503 ]; then
    print_success "Circuit Breaker OPEN confermato (Status: 503, Duration: ${DURATION}ms)"
    if [ "$DURATION" -lt 500 ]; then
        print_success "Risposta immediata (< 500ms) - Circuit Breaker funziona!"
    else
        print_warning "Risposta lenta (${DURATION}ms) - potrebbe non essere OPEN"
    fi
else
    print_warning "Status: $STATUS - Circuit Breaker potrebbe non essere OPEN"
fi

echo ""
print_step "FASE 4: Attesa transizione HALF_OPEN"
echo "─────────────────────────────────────────────────────────────"

print_step "Aspetto 10 secondi per transizione automatica a HALF_OPEN..."
for i in {10..1}; do
    echo -ne "  Attesa: ${i}s rimanenti...\r"
    sleep 1
done
echo ""
print_success "Periodo di attesa completato"

# Riavvia Product Service
print_step "Riavvio Product Service..."
docker compose start product-service
print_success "Product Service avviato"

print_step "Attendo che il servizio sia pronto (10s)..."
sleep 10

echo ""
print_step "FASE 5: Test HALF_OPEN → CLOSED"
echo "─────────────────────────────────────────────────────────────"

# permittedNumberOfCallsInHalfOpenState: 3
print_step "Invio 3 richieste (permittedNumberOfCallsInHalfOpenState=3)..."
SUCCESS=0
for i in {1..3}; do
    if make_request "Richiesta HALF_OPEN $i/3" 200; then
        SUCCESS=$((SUCCESS + 1))
    fi
    sleep 0.5
done

if [ "$SUCCESS" -eq 3 ]; then
    print_success "Tutte e 3 le richieste OK → Circuit Breaker dovrebbe essere CLOSED"
else
    print_warning "Solo $SUCCESS/3 richieste OK - Circuit Breaker potrebbe non chiudersi"
fi

echo ""
print_step "FASE 6: Verifica ritorno a CLOSED"
echo "─────────────────────────────────────────────────────────────"

print_step "Test richieste normali..."
for i in {1..3}; do
    make_request "Richiesta CLOSED $i/3" 200
    sleep 0.5
done

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                    TEST COMPLETATO                         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
print_success "Circuit Breaker testato attraverso tutti gli stati:"
echo "  • CLOSED → OPEN (dopo 50% failures)"
echo "  • OPEN → HALF_OPEN (dopo 10s automatico)"
echo "  • HALF_OPEN → CLOSED (dopo 3 success)"
echo ""
print_step "Verifica i log con:"
echo "  docker logs order-service -f | grep -i circuit"
echo ""
