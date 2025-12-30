#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

extract_token() {
    echo "$1" | grep -o '"token":"[^"]*' | cut -d'"' -f4
}

GATEWAY_URL="http://localhost:8080"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       TEST CIRCUIT BREAKER - ANALISI DETTAGLIATA          ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

print_step "Step 1: Restart Gateway (pulisce sliding window)..."
docker-compose restart api-gateway > /dev/null 2>&1
sleep 12
echo -e "${GREEN}✓${NC} Gateway riavviato"

print_step "Step 2: Login (DOPO il restart)..."
RESPONSE=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"admin","password":"admin123"}')
TOKEN=$(extract_token "$RESPONSE")

if [ -z "$TOKEN" ]; then
    echo "Errore: token vuoto. Response: $RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓${NC} Token ottenuto: ${TOKEN:0:20}..."

print_step "Step 3: Test richiesta normale (dovrebbe essere 200)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "${GATEWAY_URL}/api/products/1" \
    -H "Authorization: Bearer ${TOKEN}")
echo "Status: $STATUS"

print_step "Step 4: Stoppo Product Service..."
docker-compose stop product-service > /dev/null 2>&1
sleep 2
echo -e "${GREEN}✓${NC} Product Service fermato"

echo ""
print_step "Step 5: Faccio 10 richieste e misuro i tempi..."
echo "─────────────────────────────────────────────────────────────"

for i in {1..10}; do
    START=$(date +%s%N)
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        "${GATEWAY_URL}/api/products/1" \
        -H "Authorization: Bearer ${TOKEN}")
    END=$(date +%s%N)
    DURATION=$(( (END - START) / 1000000 ))
    
    echo "Request $i/10: Status $STATUS - Duration: ${DURATION}ms"
    sleep 0.3
done

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  ANALISI: Guarda quando inizia a rispondere < 1000ms      ║"
echo "╚════════════════════════════════════════════════════════════╝"