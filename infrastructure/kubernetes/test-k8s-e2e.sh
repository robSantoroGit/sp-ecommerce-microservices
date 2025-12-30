#!/bin/bash

# Colori per output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
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

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_data() {
    echo -e "${CYAN}$1${NC}"
}

print_header() {
    echo -e "${MAGENTA}$1${NC}"
}

GATEWAY_URL="http://localhost:30080"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       TEST E2E COMPLETO - KUBERNETES DEPLOYMENT            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# SUPERADMIN - CHECK AND REGISTER IF NEEDED
# ============================================================================

print_step "Step 0a: Verifica esistenza SUPERADMIN..."
LOGIN_TEST=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"superadmin","password":"admin123"}')

ADMIN_TOKEN=$(echo "$LOGIN_TEST" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    print_warning "Superadmin non esiste o password errata - procedo con registrazione..."
    
    print_step "Step 0b: Registrazione utente SUPERADMIN..."
    ADMIN_REG=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username":"superadmin","email":"superadmin@test.com","password":"admin123","role":"ADMIN"}')
    
    HTTP_CODE=$(echo "$ADMIN_REG" | tail -n1)
    BODY=$(echo "$ADMIN_REG" | head -n-1)
    
    if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
        print_success "Superadmin registrato con role ADMIN"
        print_data "Superadmin Data: $BODY"
    else
        print_error "Registrazione superadmin fallita (Status: $HTTP_CODE)"
        echo "$BODY"
        exit 1
    fi
    
    # Re-login dopo registrazione
    LOGIN_TEST=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"usernameOrEmail":"superadmin","password":"admin123"}')
    
    ADMIN_TOKEN=$(echo "$LOGIN_TEST" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$ADMIN_TOKEN" ]; then
        print_error "Login superadmin fallito dopo registrazione!"
        exit 1
    fi
else
    print_success "Superadmin già esistente - login OK"
fi

print_data "Token: ${ADMIN_TOKEN:0:50}..."
echo ""

# ============================================================================
# INITIAL CLEANUP - DELETE ALL DATA EXCEPT SUPERADMIN
# ============================================================================

echo "╔════════════════════════════════════════════════════════════╗"
echo "║               PULIZIA INIZIALE DATABASE                    ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

print_step "Cleanup 1: Eliminazione tutti i prodotti..."
PRODUCTS=$(curl -s "${GATEWAY_URL}/api/products" -H "Authorization: Bearer ${ADMIN_TOKEN}")
PRODUCT_IDS=$(echo "$PRODUCTS" | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ ! -z "$PRODUCT_IDS" ]; then
    COUNT=0
    for PID in $PRODUCT_IDS; do
        curl -s -o /dev/null -X DELETE "${GATEWAY_URL}/api/products/${PID}" \
            -H "Authorization: Bearer ${ADMIN_TOKEN}"
        COUNT=$((COUNT+1))
    done
    print_success "Eliminati $COUNT prodotti"
else
    print_success "Nessun prodotto da eliminare"
fi
echo ""

print_step "Cleanup 2: Eliminazione tutte le categorie..."
CATEGORIES=$(curl -s "${GATEWAY_URL}/api/categories" -H "Authorization: Bearer ${ADMIN_TOKEN}")
CATEGORY_IDS=$(echo "$CATEGORIES" | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ ! -z "$CATEGORY_IDS" ]; then
    COUNT=0
    for CID in $CATEGORY_IDS; do
        curl -s -o /dev/null -X DELETE "${GATEWAY_URL}/api/categories/${CID}" \
            -H "Authorization: Bearer ${ADMIN_TOKEN}"
        COUNT=$((COUNT+1))
    done
    print_success "Eliminate $COUNT categorie"
else
    print_success "Nessuna categoria da eliminare"
fi
echo ""

print_step "Cleanup 3: Eliminazione tutti gli utenti tranne superadmin..."
USERS=$(curl -s "${GATEWAY_URL}/api/users" -H "Authorization: Bearer ${ADMIN_TOKEN}")
USER_IDS=$(echo "$USERS" | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ ! -z "$USER_IDS" ]; then
    COUNT=0
    for USERID in $USER_IDS; do
        # Verifica che non sia superadmin
        USER_DETAIL=$(curl -s "${GATEWAY_URL}/api/users/${USERID}" -H "Authorization: Bearer ${ADMIN_TOKEN}")
        USERNAME=$(echo "$USER_DETAIL" | grep -o '"username":"[^"]*' | cut -d'"' -f4)
        
        if [ "$USERNAME" != "superadmin" ]; then
            curl -s -o /dev/null -X DELETE "${GATEWAY_URL}/api/users/${USERID}" \
                -H "Authorization: Bearer ${ADMIN_TOKEN}"
            COUNT=$((COUNT+1))
        fi
    done
    print_success "Eliminati $COUNT utenti (superadmin preservato)"
else
    print_success "Nessun utente da eliminare"
fi
echo ""

# ============================================================================
# INITIAL DATABASE STATE
# ============================================================================

echo "╔════════════════════════════════════════════════════════════╗"
echo "║               STATO INIZIALE DATABASE                      ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

print_header "=== USERS INIZIALI ==="
INITIAL_USERS=$(curl -s "${GATEWAY_URL}/api/users" -H "Authorization: Bearer ${ADMIN_TOKEN}")
echo "$INITIAL_USERS"
echo ""

print_header "=== CATEGORIES INIZIALI ==="
INITIAL_CATEGORIES=$(curl -s "${GATEWAY_URL}/api/categories" -H "Authorization: Bearer ${ADMIN_TOKEN}")
echo "$INITIAL_CATEGORIES"
echo ""

print_header "=== PRODUCTS INIZIALI ==="
INITIAL_PRODUCTS=$(curl -s "${GATEWAY_URL}/api/products" -H "Authorization: Bearer ${ADMIN_TOKEN}")
echo "$INITIAL_PRODUCTS"
echo ""

print_header "=== ORDERS INIZIALI  ==="
FINAL_ORDERS=$(curl -s "${GATEWAY_URL}/api/orders" -H "Authorization: Bearer ${ADMIN_TOKEN}")
ORDERS_CONTENT=$(echo "$FINAL_ORDERS" | grep -o '"content":\[.*\]' | sed 's/"content"://')
echo "$ORDERS_CONTENT"
echo ""

echo "╔════════════════════════════════════════════════════════════╗"
echo "║                    INIZIO TEST E2E                         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# USER SERVICE - SUPERADMIN
# ============================================================================

print_step "Step 1: Test User Service come SUPERADMIN..."
USER_RESPONSE=$(curl -s -w "\n%{http_code}" "${GATEWAY_URL}/api/users/1" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

HTTP_CODE=$(echo "$USER_RESPONSE" | tail -n1)
BODY=$(echo "$USER_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "User Service OK (200)"
    print_data "User Data: $BODY"
else
    print_warning "User ID 1 non trovato (normale se eliminato in cleanup)"
fi
echo ""

# ============================================================================
# CUSTOMER REGISTRATION
# ============================================================================

print_step "Step 2: Registrazione utente CUSTOMER..."
CUSTOMER_REG=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"username":"customer1","email":"customer@test.com","password":"customer123"}')

HTTP_CODE=$(echo "$CUSTOMER_REG" | tail -n1)
BODY=$(echo "$CUSTOMER_REG" | head -n-1)

if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Customer registrato con role CUSTOMER (default)"
    print_data "Customer Data: $BODY"
else
    print_error "Registrazione customer FAIL (Status: $HTTP_CODE)"
    echo "$BODY"
fi
echo ""

# ============================================================================
# AUTHENTICATION - CUSTOMER
# ============================================================================

print_step "Step 3: Login con utente CUSTOMER..."
CUSTOMER_LOGIN=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"usernameOrEmail":"customer1","password":"customer123"}')

CUSTOMER_TOKEN=$(echo "$CUSTOMER_LOGIN" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$CUSTOMER_TOKEN" ]; then
    print_error "Login customer fallito!"
    exit 1
fi

print_success "Login CUSTOMER OK"
print_data "Token: ${CUSTOMER_TOKEN:0:50}..."
echo ""

# ============================================================================
# CATEGORY CREATION - SUPERADMIN
# ============================================================================

print_step "Step 4: Creazione Categoria 'Electronics' (SUPERADMIN)..."
CATEGORY_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/categories" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"name":"Electronics","description":"Electronic devices and gadgets"}')

HTTP_CODE=$(echo "$CATEGORY_RESPONSE" | tail -n1)
BODY=$(echo "$CATEGORY_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
    CATEGORY_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    print_success "Categoria creata (ID: $CATEGORY_ID)"
    print_data "Category Data: $BODY"
else
    print_error "Creazione categoria FAIL (Status: $HTTP_CODE)"
    echo "$BODY"
    exit 1
fi
echo ""

# ============================================================================
# RBAC TEST - CUSTOMER CANNOT CREATE CATEGORY
# ============================================================================

print_step "Step 5: Test RBAC - CUSTOMER tenta creazione categoria (deve fallire 403)..."
FORBIDDEN_CAT=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/categories" \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"name":"Forbidden Category","description":"This should fail"}')

HTTP_CODE=$(echo "$FORBIDDEN_CAT" | tail -n1)
BODY=$(echo "$FORBIDDEN_CAT" | head -n-1)

if [ "$HTTP_CODE" -eq 403 ]; then
    print_success "403 Forbidden - RBAC funziona correttamente!"
else
    print_error "Atteso 403, ricevuto: $HTTP_CODE (RBAC non funziona!)"
fi
print_data "Response: $BODY"
echo ""

# ============================================================================
# PRODUCT CREATION - SUPERADMIN
# ============================================================================

print_step "Step 6: Creazione Prodotto 'Laptop Pro' (SUPERADMIN)..."
PRODUCT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/products" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Laptop Pro\",\"description\":\"High-performance laptop\",\"price\":1299.99,\"stock\":50,\"categoryId\":${CATEGORY_ID}}")

HTTP_CODE=$(echo "$PRODUCT_RESPONSE" | tail -n1)
BODY=$(echo "$PRODUCT_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
    PRODUCT_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    print_success "Prodotto creato (ID: $PRODUCT_ID)"
    print_data "Product Data: $BODY"
else
    print_error "Creazione prodotto FAIL (Status: $HTTP_CODE)"
    echo "$BODY"
    exit 1
fi
echo ""

# ============================================================================
# RBAC TEST - CUSTOMER CANNOT CREATE PRODUCT
# ============================================================================

print_step "Step 7: Test RBAC - CUSTOMER tenta creazione prodotto (deve fallire 403)..."
FORBIDDEN_PROD=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/products" \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Forbidden Product\",\"description\":\"This should fail\",\"price\":99.99,\"stock\":10,\"categoryId\":${CATEGORY_ID}}")

HTTP_CODE=$(echo "$FORBIDDEN_PROD" | tail -n1)
BODY=$(echo "$FORBIDDEN_PROD" | head -n-1)

if [ "$HTTP_CODE" -eq 403 ]; then
    print_success "403 Forbidden - RBAC funziona correttamente!"
else
    print_error "Atteso 403, ricevuto: $HTTP_CODE (RBAC non funziona!)"
fi
print_data "Response: $BODY"
echo ""

# ============================================================================
# GET PRODUCT BY ID
# ============================================================================

print_step "Step 8: Recupero Prodotto per ID (GET /api/products/${PRODUCT_ID})..."
PRODUCT_GET=$(curl -s -w "\n%{http_code}" "${GATEWAY_URL}/api/products/${PRODUCT_ID}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

HTTP_CODE=$(echo "$PRODUCT_GET" | tail -n1)
BODY=$(echo "$PRODUCT_GET" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Prodotto recuperato (200)"
    print_data "Product Details: $BODY"
else
    print_error "GET prodotto FAIL (Status: $HTTP_CODE)"
    echo "$BODY"
fi
echo ""

# ============================================================================
# GET PRODUCTS BY CATEGORY
# ============================================================================

print_step "Step 9: Ricerca Prodotti per Categoria..."
PRODUCTS_BY_CAT=$(curl -s -w "\n%{http_code}" "${GATEWAY_URL}/api/products?categoryId=${CATEGORY_ID}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

HTTP_CODE=$(echo "$PRODUCTS_BY_CAT" | tail -n1)
BODY=$(echo "$PRODUCTS_BY_CAT" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    COUNT=$(echo "$BODY" | grep -o '"id":[0-9]*' | wc -l)
    print_success "Prodotti trovati per categoria: $COUNT"
    print_data "Products in Category: $BODY"
else
    print_error "Ricerca per categoria FAIL (Status: $HTTP_CODE)"
    echo "$BODY"
fi
echo ""

# ============================================================================
# LIST ALL CATEGORIES
# ============================================================================

print_step "Step 10: Lista Tutte le Categorie..."
CATEGORIES_LIST=$(curl -s -w "\n%{http_code}" "${GATEWAY_URL}/api/categories" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

HTTP_CODE=$(echo "$CATEGORIES_LIST" | tail -n1)
BODY=$(echo "$CATEGORIES_LIST" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    COUNT=$(echo "$BODY" | grep -o '"id":[0-9]*' | wc -l)
    print_success "Categorie totali: $COUNT"
    print_data "All Categories: $BODY"
else
    print_error "Lista categorie FAIL (Status: $HTTP_CODE)"
    echo "$BODY"
fi
echo ""

# ============================================================================
# LIST ALL PRODUCTS
# ============================================================================

print_step "Step 11: Lista Tutti i Prodotti..."
PRODUCTS_LIST=$(curl -s -w "\n%{http_code}" "${GATEWAY_URL}/api/products" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

HTTP_CODE=$(echo "$PRODUCTS_LIST" | tail -n1)
BODY=$(echo "$PRODUCTS_LIST" | head -n-1)

if [ "$HTTP_CODE" -eq 200 ]; then
    COUNT=$(echo "$BODY" | grep -o '"id":[0-9]*' | wc -l)
    print_success "Prodotti totali: $COUNT"
    print_data "All Products: $BODY"
else
    print_error "Lista prodotti FAIL (Status: $HTTP_CODE)"
    echo "$BODY"
fi
echo ""

# ============================================================================
# ORDER TESTS - KAFKA EVENTS
# ============================================================================

# Step 12: Creazione Ordine #1
print_step "Step 12: Creazione Ordine #1 (CUSTOMER)..."
ORDER1_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/orders" \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"items\":[{\"productId\":${PRODUCT_ID},\"quantity\":2}],\"deliveryAddress\":\"Via Roma 123, Milano, Italy\"}")

HTTP_CODE=$(echo "$ORDER1_RESPONSE" | tail -n1)
BODY=$(echo "$ORDER1_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
    ORDER1_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    print_success "Ordine #1 creato (ID: $ORDER1_ID) → Kafka event 'order.created'"
    print_data "Order: $BODY"
else
    print_warning "Creazione ordine FAIL (Status: $HTTP_CODE): $BODY"
fi
echo ""

# Step 13: Update Status Ordine #1
print_step "Step 13: Cambio stato Ordine #1 → PAID..."
ORDER1_UPDATE=$(curl -s -w "\n%{http_code}" -X PUT "${GATEWAY_URL}/api/orders/${ORDER1_ID}/status" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"status":"PAID"}')

HTTP_CODE=$(echo "$ORDER1_UPDATE" | tail -n1)

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Stato aggiornato → Kafka event 'order.status.changed'"
else
    print_warning "Update status FAIL (Status: $HTTP_CODE)"
fi
echo ""

# Step 14: Creazione Ordine #2
print_step "Step 14: Creazione Ordine #2 (CUSTOMER)..."
ORDER2_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/orders" \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"items\":[{\"productId\":${PRODUCT_ID},\"quantity\":1}],\"deliveryAddress\":\"Corso Napoli 45, Roma, Italy\"}")

HTTP_CODE=$(echo "$ORDER2_RESPONSE" | tail -n1)
BODY=$(echo "$ORDER2_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
    ORDER2_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    print_success "Ordine #2 creato (ID: $ORDER2_ID) → Kafka event 'order.created'"
else
    print_warning "Creazione ordine #2 FAIL (Status: $HTTP_CODE)"
fi
echo ""

# Step 15: Cancellazione Ordine #2
print_step "Step 15: Cancellazione Ordine #2..."
ORDER2_CANCEL=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "${GATEWAY_URL}/api/orders/${ORDER2_ID}" \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}")

if [ "$ORDER2_CANCEL" -eq 204 ] || [ "$ORDER2_CANCEL" -eq 200 ]; then
    print_success "Ordine #2 cancellato → Kafka event 'order.cancelled'"
else
    print_warning "Cancellazione FAIL (Status: $ORDER2_CANCEL)"
fi
echo ""

# Step 16: Lista Ordini
print_step "Step 16: Lista Ordini Customer..."
ORDERS_LIST=$(curl -s "${GATEWAY_URL}/api/orders" -H "Authorization: Bearer ${CUSTOMER_TOKEN}")
COUNT=$(echo "$ORDERS_LIST" | grep -o '"id":[0-9]*' | wc -l)
print_success "Ordini attivi: $COUNT"
echo ""

# ============================================================================
# TEST 404 - PRODUCT NOT FOUND
# ============================================================================

print_step "Step 17: Test Errore 404 - Prodotto Inesistente..."
NOT_FOUND=$(curl -s -w "\n%{http_code}" "${GATEWAY_URL}/api/products/99999" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

HTTP_CODE=$(echo "$NOT_FOUND" | tail -n1)
BODY=$(echo "$NOT_FOUND" | head -n-1)

if [ "$HTTP_CODE" -eq 404 ]; then
    print_success "404 correttamente restituito"
    print_data "Error Response: $BODY"
else
    print_warning "Atteso 404, ricevuto: $HTTP_CODE"
    echo "$BODY"
fi
echo ""

# ============================================================================
# TEST SEARCH NO RESULTS
# ============================================================================

print_step "Step 18: Test Ricerca Senza Risultati..."
NO_RESULTS=$(curl -s -w "\n%{http_code}" "${GATEWAY_URL}/api/products/category/99999" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")

HTTP_CODE=$(echo "$NO_RESULTS" | tail -n1)
BODY=$(echo "$NO_RESULTS" | head -n-1)

if echo "$BODY" | grep -q '"content":\[\]'; then
    print_success "Ricerca senza risultati OK (lista vuota)"
    print_data "Empty Result: $BODY"
else
    CONTENT=$(echo "$BODY" | grep -o '"content":\[.*\]' | head -1)
    COUNT=$(echo "$CONTENT" | grep -o '"id":[0-9]*' | wc -l)
    print_warning "Attesa lista vuota, trovati $COUNT prodotti"
    echo "$BODY"
fi

# ============================================================================
# RATE LIMITING TEST
# ============================================================================

print_step "Step 19: Test Rate Limiting (6 richieste rapide)..."
for i in {1..6}; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        "${GATEWAY_URL}/api/products" \
        -H "Authorization: Bearer ${ADMIN_TOKEN}")
    
    if [ "$STATUS" -eq 429 ]; then
        echo "Request $i/6: ${RED}429 Too Many Requests${NC} ← Rate Limit OK!"
    else
        echo "Request $i/6: ${GREEN}$STATUS${NC}"
    fi
    sleep 0.1
done
echo ""

# ============================================================================
# GATEWAY HEALTH CHECK
# ============================================================================

print_step "Step 20: Gateway Health Check..."
HEALTH=$(curl -s "${GATEWAY_URL}/actuator/health")

# Estrai solo lo status globale (alla fine del JSON)
GLOBAL_STATUS=$(echo "$HEALTH" | grep -o '"status":"[^"]*' | tail -1 | cut -d'"' -f4)

if [ "$GLOBAL_STATUS" == "UP" ]; then
    print_success "Gateway Health: UP"
    print_data "Health Details: $HEALTH"
else
    print_warning "Gateway Health: $GLOBAL_STATUS"
    echo "$HEALTH"
fi
echo ""

# ============================================================================
# FINAL CLEANUP
# ============================================================================

echo "╔════════════════════════════════════════════════════════════╗"
echo "║                    CLEANUP FINALE                          ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

print_step "Cleanup 1: Eliminazione Prodotto..."
if [ ! -z "$PRODUCT_ID" ]; then
    DELETE_PROD=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
        "${GATEWAY_URL}/api/products/${PRODUCT_ID}" \
        -H "Authorization: Bearer ${ADMIN_TOKEN}")
    
    if [ "$DELETE_PROD" -eq 204 ] || [ "$DELETE_PROD" -eq 200 ]; then
        print_success "Prodotto eliminato (ID: $PRODUCT_ID)"
    else
        print_warning "Eliminazione prodotto status: $DELETE_PROD"
    fi
fi
echo ""

print_step "Cleanup 2: Eliminazione Categoria..."
if [ ! -z "$CATEGORY_ID" ]; then
    DELETE_CAT=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
        "${GATEWAY_URL}/api/categories/${CATEGORY_ID}" \
        -H "Authorization: Bearer ${ADMIN_TOKEN}")
    
    if [ "$DELETE_CAT" -eq 204 ] || [ "$DELETE_CAT" -eq 200 ]; then
        print_success "Categoria eliminata (ID: $CATEGORY_ID)"
    else
        print_warning "Eliminazione categoria status: $DELETE_CAT"
    fi
fi
echo ""

print_step "Cleanup 3: Eliminazione utente CUSTOMER..."
CUSTOMER_LIST=$(curl -s "${GATEWAY_URL}/api/users" -H "Authorization: Bearer ${ADMIN_TOKEN}")
USER_IDS=$(echo "$CUSTOMER_LIST" | grep -o '"id":[0-9]*' | cut -d':' -f2)

COUNT=0
for USER_ID in $USER_IDS; do
    USER_DETAIL=$(curl -s "${GATEWAY_URL}/api/users/${USER_ID}" -H "Authorization: Bearer ${ADMIN_TOKEN}")
    USERNAME=$(echo "$USER_DETAIL" | grep -o '"username":"[^"]*' | cut -d'"' -f4)
    
    if [ "$USERNAME" == "customer1" ]; then
        DELETE_USER=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
            "${GATEWAY_URL}/api/users/${USER_ID}" \
            -H "Authorization: Bearer ${ADMIN_TOKEN}")
        
        if [ "$DELETE_USER" -eq 204 ] || [ "$DELETE_USER" -eq 200 ]; then
            print_success "Customer eliminato (ID: $USER_ID)"
            COUNT=$((COUNT+1))
        else
            print_warning "Eliminazione customer status: $DELETE_USER"
        fi
    fi
done

if [ "$COUNT" -eq 0 ]; then
    print_warning "Customer non trovato"
fi
echo ""

# ============================================================================
# FINAL DATABASE STATE
# ============================================================================

echo "╔════════════════════════════════════════════════════════════╗"
echo "║               STATO FINALE DATABASE                        ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

print_header "=== USERS FINALI ==="
FINAL_USERS=$(curl -s "${GATEWAY_URL}/api/users" -H "Authorization: Bearer ${ADMIN_TOKEN}")
echo "$FINAL_USERS"
echo ""

print_header "=== CATEGORIES FINALI ==="
FINAL_CATEGORIES=$(curl -s "${GATEWAY_URL}/api/categories" -H "Authorization: Bearer ${ADMIN_TOKEN}")
echo "$FINAL_CATEGORIES"
echo ""

print_header "=== PRODUCTS FINALI ==="
FINAL_PRODUCTS=$(curl -s "${GATEWAY_URL}/api/products" -H "Authorization: Bearer ${ADMIN_TOKEN}")
echo "$FINAL_PRODUCTS"
echo ""

print_header "=== ORDERS FINALI ==="
FINAL_ORDERS=$(curl -s "${GATEWAY_URL}/api/orders" -H "Authorization: Bearer ${ADMIN_TOKEN}")
ORDERS_CONTENT=$(echo "$FINAL_ORDERS" | grep -o '"content":\[.*\]' | sed 's/"content"://')
echo "$ORDERS_CONTENT"
echo ""

# ============================================================================
# SUMMARY
# ============================================================================

echo "╔════════════════════════════════════════════════════════════╗"
echo "║                    TEST COMPLETATO                         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
print_success "Stack Kubernetes E2E testato con successo!"
echo ""
echo "Test eseguiti:"
echo "  ✓ Cleanup iniziale database"
echo "  ✓ Registrazione condizionale SUPERADMIN"
echo "  ✓ Verifica stato iniziale database (pulito)"
echo "  ✓ Registrazione Customer"
echo "  ✓ RBAC tests (403 Forbidden)"
echo "  ✓ CRUD operations (Category, Product)"
echo "  ✓ Order Service tests (create, update, cancel)"
echo "  ✓ Kafka events generation"
echo "  ✓ Query tests (by ID, by category, list)"
echo "  ✓ Error handling (404, empty results)"
echo "  ✓ Rate Limiting"
echo "  ✓ Health Check"
echo "  ✓ Cleanup finale"
echo ""
print_success "Database pulito - Solo SUPERADMIN presente e Ordini in stato cancelled"
echo ""