#!/bin/bash

# Colori
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Funzione per estrarre token dal JSON (senza jq)
extract_token() {
    echo "$1" | grep -o '"token":"[^"]*' | cut -d'"' -f4
}

GATEWAY_URL="http://localhost:8080"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       TEST CIRCUIT BREAKER - API GATEWAY                   ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Ottieni token
print_step "Step 1: Login per ottenere JWT token..."

RESPONSE=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
        "usernameOrEmail": "admin",
        "password": "admin123"
    }')

TOKEN=$(extract_token "$RESPONSE")

if [ -z "$TOKEN" ]; then
    print_error "Login fallito. Impossibile ottenere token JWT"
    exit 1
fi

print_success "Token JWT ottenuto"

echo ""
print_step "Step 2: Verifica che Product Service sia running..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "${GATEWAY_URL}/api/products/1" \
    -H "Authorization: Bearer ${TOKEN}")

if [ "$STATUS" -eq 200 ]; then
    print_success "Product Service è UP (Status: 200)"
else
    print_error "Product Service non risponde correttamente (Status: $STATUS)"
    exit 1
fi

echo ""
print_step "Step 3: Fermiamo Product Service..."
docker-compose stop product-service
print_success "Product Service fermato"

sleep 2

echo ""
print_step "Step 4: Triggering Circuit Breaker (7 richieste)..."
echo "─────────────────────────────────────────────────────────────"

for i in {1..7}; do
    START=$(date +%s%N)
    
    RESPONSE=$(curl -s -w "\n%{http_code}" \
        "${GATEWAY_URL}/api/products/1" \
        -H "Authorization: Bearer ${TOKEN}")
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n-1)
    
    END=$(date +%s%N)
    DURATION=$(( (END - START) / 1000000 ))
    
    if [ "$STATUS" -eq 503 ]; then
        echo -e "${GREEN}Request $i/7: 503 Service Unavailable (${DURATION}ms)${NC} ← Circuit OPEN!"
        if [ $DURATION -lt 1000 ]; then
            print_success "  Risposta immediata - Circuit Breaker funziona!"
        fi
    else
        echo "Request $i/7: Status $STATUS (${DURATION}ms)"
    fi
    
    sleep 0.5
done

echo ""
print_step "Step 5: Riavviamo Product Service..."
docker-compose start product-service
print_success "Product Service avviato"

print_step "Attendo che il servizio sia pronto (5s)..."
sleep 5

echo ""
print_step "Step 6: Aspetto transizione a HALF_OPEN (10s)..."
for i in {10..1}; do
    echo -ne "  Attesa: ${i}s rimanenti...\r"
    sleep 1
done
echo ""
print_success "Periodo di attesa completato"

echo ""
print_step "Step 7: Test HALF_OPEN → CLOSED (3 richieste)..."
echo "─────────────────────────────────────────────────────────────"

SUCCESS=0
for i in {1..3}; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        "${GATEWAY_URL}/api/products/1" \
        -H "Authorization: Bearer ${TOKEN}")
    
    if [ "$STATUS" -eq 200 ]; then
        echo -e "${GREEN}Request $i/3: 200 OK${NC}"
        SUCCESS=$((SUCCESS + 1))
    else
        echo -e "${RED}Request $i/3: $STATUS${NC}"
    fi
    sleep 0.5
done

if [ "$SUCCESS" -eq 3 ]; then
    print_success "Tutte e 3 le richieste OK → Circuit Breaker dovrebbe essere CLOSED"
else
    print_error "Solo $SUCCESS/3 richieste OK"
fi

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                    TEST COMPLETATO                         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
print_success "Circuit Breaker Gateway testato attraverso tutti gli stati"
echo ""