
# probar-api.ps1
# Dispara un flujo end-to-end contra Check-in.

$ErrorActionPreference = "Continue"

$baseUrl = "http://localhost:8081"

$stamp = Get-Date -Format "yyyyMMddHHmmssfff"

function Show-Response {
    param($response, $label)
    Write-Host ""
    Write-Host "-> $label" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 10
}

Write-Host ""
Write-Host "== 1. HEALTH CHECK ==" -ForegroundColor Yellow
try {
    $r = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
    $r | ConvertTo-Json
}
catch {
    Write-Host "[X] Check-in no responde en $baseUrl/health" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== 2. CHECK-IN EXITOSO - 2 maletas (dispara la cadena Check-in -> Security -> Dispatcher) ===" -ForegroundColor Yellow
$bodyExitoso = @{
    pasajeroId     = "PAX-$stamp"
    nombrePasajero = "Vale Prueba"
    documento      = "DOC-$stamp"
    email          = "vale$stamp@test.com"
    vueloId        = "VUELO-001"
    equipajes      = @(
        @{ codigoRFID = "RFID-A-$stamp"; peso = 18.5 },
        @{ codigoRFID = "RFID-B-$stamp"; peso = 20.0 }
    )
} | ConvertTo-Json -Depth 5

try {
    $r = Invoke-RestMethod -Uri "$baseUrl/api/v1/checkin" -Method Post `
         -Body $bodyExitoso -ContentType "application/json"
    Show-Response $r "201 Created (esperado)"
    $pasajeroCreado = "PAX-$stamp"
}
catch {
    Write-Host "[X] Fallo el check-in exitoso:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($_.ErrorDetails) { Write-Host $_.ErrorDetails.Message -ForegroundColor Red }
}

Write-Host ""
Write-Host "=== 3. CHECK-IN CON PESO EXCEDIDO - debe dar 400 ===" -ForegroundColor Yellow
$bodyPeso = @{
    pasajeroId     = "PAX-PESO-$stamp"
    nombrePasajero = "Test Peso"
    documento      = "DOC-PESO-$stamp"
    email          = "peso$stamp@test.com"
    vueloId        = "VUELO-001"
    equipajes      = @(
        @{ codigoRFID = "RFID-PESO-$stamp"; peso = 35.0 }
    )
} | ConvertTo-Json -Depth 5

try {
    $r = Invoke-RestMethod -Uri "$baseUrl/api/v1/checkin" -Method Post `
         -Body $bodyPeso -ContentType "application/json"
    Write-Host "[!] Inesperado: no fallo" -ForegroundColor Yellow
    $r | ConvertTo-Json
}
catch {
    Write-Host "Codigo:" $_.Exception.Response.StatusCode -ForegroundColor Green
    if ($_.ErrorDetails) { Write-Host "Body:" $_.ErrorDetails.Message }
}

Write-Host ""
Write-Host "=== 4. CHECK-IN SIN EQUIPAJES - debe dar 400 ===" -ForegroundColor Yellow
$bodySinEq = @{
    pasajeroId     = "PAX-VACIO-$stamp"
    nombrePasajero = "Test Vacio"
    documento      = "DOC-VACIO-$stamp"
    email          = "vacio$stamp@test.com"
    vueloId        = "VUELO-001"
    equipajes      = @()
} | ConvertTo-Json -Depth 5

try {
    $r = Invoke-RestMethod -Uri "$baseUrl/api/v1/checkin" -Method Post `
         -Body $bodySinEq -ContentType "application/json"
    Write-Host "[!] Inesperado: no fallo" -ForegroundColor Yellow
}
catch {
    Write-Host "Codigo:" $_.Exception.Response.StatusCode -ForegroundColor Green
    if ($_.ErrorDetails) { Write-Host "Body:" $_.ErrorDetails.Message }
}

if ($pasajeroCreado) {
    Write-Host ""
    Write-Host "=== 5. GET equipajes del pasajero recien creado ===" -ForegroundColor Yellow
    try {
        $r = Invoke-RestMethod -Uri "$baseUrl/api/v1/equipajes?pasajeroId=$pasajeroCreado" -Method Get
        Show-Response $r "Equipajes registrados"
    }
    catch {
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " Pruebas completadas." -ForegroundColor Green
Write-Host " Ahora abri http://localhost:8080 y mira los 3 topicos:" -ForegroundColor Green
Write-Host "   - registro.pasajero    (publicado por Check-in)" -ForegroundColor Green
Write-Host "   - equipaje.bodega      (publicado por Security)" -ForegroundColor Green
Write-Host "   - equipaje.despacho    (publicado por Dispatcher)" -ForegroundColor Green
Write-Host ""
Write-Host " equipajeId que acabas de crear: (buscalo en los 3 topicos)" -ForegroundColor Green
Write-Host "   pasajeroId = PAX-$stamp" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""