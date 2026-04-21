#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  Script de prueba del API Check-In (Bash / Mac / Linux)
#
#  Uso: desde la raíz del proyecto, con el servicio corriendo:
#       bash examples/probar-api.sh
# ═══════════════════════════════════════════════════════════════

BASE_URL="http://localhost:8081"

echo ""
echo "━━━ 1. HEALTH CHECK ━━━"
curl -s "$BASE_URL/health" | jq . 2>/dev/null || curl -s "$BASE_URL/health"

echo ""
echo ""
echo "━━━ 2. CHECK-IN EXITOSO (2 maletas) ━━━"
curl -s -X POST "$BASE_URL/api/v1/checkin" \
  -H "Content-Type: application/json" \
  -d @examples/checkin-exitoso.json | jq . 2>/dev/null \
  || curl -s -X POST "$BASE_URL/api/v1/checkin" \
       -H "Content-Type: application/json" \
       -d @examples/checkin-exitoso.json

echo ""
echo ""
echo "━━━ 3. CHECK-IN CON PESO EXCEDIDO (debe dar 400) ━━━"
curl -s -w "\nHTTP: %{http_code}\n" -X POST "$BASE_URL/api/v1/checkin" \
  -H "Content-Type: application/json" \
  -d @examples/checkin-peso-excedido.json

echo ""
echo "━━━ 4. CHECK-IN SIN EQUIPAJES (debe dar 400) ━━━"
curl -s -w "\nHTTP: %{http_code}\n" -X POST "$BASE_URL/api/v1/checkin" \
  -H "Content-Type: application/json" \
  -d @examples/checkin-sin-equipajes.json

echo ""
echo "✓ Pruebas completadas. Revisa los eventos en http://localhost:8080"