#!/bin/bash

# Prima ottieni un token valido
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"admin123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo "Token ottenuto, testing rate limit..."

# Test con 30 richieste GET /api/users (query DB)
for i in {1..20}; do
  curl -s -o /dev/null -w "Request $i: %{http_code}\n" \
    http://localhost:8080/api/orders \
    -H "Authorization: Bearer $TOKEN"
done