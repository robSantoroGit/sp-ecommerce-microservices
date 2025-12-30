#!/bin/bash
# Login
TOKEN=$(curl -s -X POST http://localhost:30080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"superadmin","password":"admin123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# Fai richieste multiple
for i in {1..50}; do
  curl -s http://localhost:30080/api/orders \
    -H "Authorization: Bearer $TOKEN" > /dev/null
  echo "Request $i"
  sleep 0.1
done