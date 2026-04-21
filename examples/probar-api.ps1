# Script de prueba del API Check-In (PowerShell / Windows)
# Uso: .\examples\probar-api.ps1

$baseUrl = "http://localhost:8081"

Write-Host ""
Write-Host "=== 1. HEALTH CHECK ===" -ForegroundColor Cyan
Invoke-RestMethod -Uri "$baseUrl/health" -Method Get | ConvertTo-Json


Write-Host ""
Write-Host "=== 2. CHECK-IN EXITOSO - 2 maletas ===" -ForegroundColor Green
$body = Get-Content -Raw -Path ".\examples\checkin-exitoso.json"
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/checkin" `
        -Method Post -ContentType "application/json" -Body $body | ConvertTo-Json
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host ""
Write-Host "=== 3. CHECK-IN CON PESO EXCEDIDO - debe dar 400 ===" -ForegroundColor Yellow
$body = Get-Content -Raw -Path ".\examples\checkin-peso-excedido.json"
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/checkin" `
        -Method Post -ContentType "application/json" -Body $body | ConvertTo-Json
} catch {
    $resp = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($resp)
    Write-Host "Codigo: $($_.Exception.Response.StatusCode)"
    Write-Host "Body:   $($_.ErrorDetails.Message)"
}


Write-Host ""
Write-Host "=== 4. CHECK-IN SIN EQUIPAJES - debe dar 400 ===" -ForegroundColor Yellow
$body = Get-Content -Raw -Path ".\examples\checkin-sin-equipajes.json"
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/checkin" `
        -Method Post -ContentType "application/json" -Body $body | ConvertTo-Json
} catch {
    $resp = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($resp)
    Write-Host "Codigo: $($_.Exception.Response.StatusCode)"
    Write-Host "Body:   $($_.ErrorDetails.Message)"
}

Write-Host ""
Write-Host "=== Pruebas completadas. Revisa Kafka UI en http://localhost:8080 ===" -ForegroundColor Cyan