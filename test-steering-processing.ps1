#!/usr/bin/env pwsh
# Script de teste aprimorado: simula diferentes cenarios de steering

$serverIP = "127.0.0.1"
$serverPort = 5005

$udpClient = New-Object System.Net.Sockets.UdpClient
$remoteEndPoint = New-Object System.Net.IPEndPoint ([System.Net.IPAddress]::Parse($serverIP)), $serverPort

function Send-SteeringPacket {
    param(
        [double]$angle,
        [double]$gyro,
        [string]$description
    )
    
    $timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    $packet = @{
        type = "steering"
        angle = $angle
        gyro = $gyro
        timestamp = $timestamp
    } | ConvertTo-Json

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($packet)
    $udpClient.Send($bytes, $bytes.Length, $remoteEndPoint) | Out-Null
    
    Write-Host "[OK] $description" -ForegroundColor Green
    Write-Host "     angle=$angle, gyro=$gyro" -ForegroundColor Gray
    Start-Sleep -Milliseconds 100
}

Write-Host ""
Write-Host "[TEST] Cenarios de Direção:" -ForegroundColor Cyan
Write-Host ""

# Teste 1: Centro (0°)
Send-SteeringPacket -angle 0.0 -gyro 0.0 -description "Volante centralizado"

# Teste 2: Virada à direita (45°)
Send-SteeringPacket -angle 45.0 -gyro 0.5 -description "Virada suave a direita"

# Teste 3: Virada à esquerda (-45°)
Send-SteeringPacket -angle -45.0 -gyro -0.5 -description "Virada suave a esquerda"

# Teste 4: Virada extrema à direita (200°)
Send-SteeringPacket -angle 200.0 -gyro 1.2 -description "Virada extrema a direita"

# Teste 5: Virada extrema à esquerda (-200°)
Send-SteeringPacket -angle -200.0 -gyro -1.2 -description "Virada extrema a esquerda"

# Teste 6: Limite máximo (450°)
Send-SteeringPacket -angle 450.0 -gyro 0.0 -description "Limite maximo a direita"

# Teste 7: Limite máximo negativo (-450°)
Send-SteeringPacket -angle -450.0 -gyro 0.0 -description "Limite maximo a esquerda"

# Teste 8: Acima do limite (500° sera clamped)
Send-SteeringPacket -angle 500.0 -gyro 0.1 -description "Acima do limite (sera limitado)"

# Teste 9: Dentro da deadzone (2°)
Send-SteeringPacket -angle 2.0 -gyro 0.01 -description "Dentro da deadzone (sera 0)"

# Teste 10: Sequencia suave (simula movimento)
Write-Host ""
Write-Host "[TEST] Sequencia de movimento suave:" -ForegroundColor Cyan
for ($i = 0; $i -le 10; $i++) {
    $angle = $i * 20
    $gyro = ($i * 20) / 450.0
    Send-SteeringPacket -angle $angle -gyro $gyro -description "Movimento $i"
    Start-Sleep -Milliseconds 150
}

$udpClient.Close()
Write-Host ""
Write-Host "[OK] Testes finalizados." -ForegroundColor Green
