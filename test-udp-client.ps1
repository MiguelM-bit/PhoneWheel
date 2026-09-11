#!/usr/bin/env pwsh
# Script de teste: simula o envio de pacotes UDP para o servidor Windows

# Configurações
$serverIP = "127.0.0.1"
$serverPort = 5005

# Criar cliente UDP
$udpClient = New-Object System.Net.Sockets.UdpClient

$remoteEndPoint = New-Object System.Net.IPEndPoint ([System.Net.IPAddress]::Parse($serverIP)), $serverPort

# Testar 1: Pacote válido
Write-Host "[OK] Enviando pacote valido..." -ForegroundColor Green
$packet1 = @{
    type = "steering"
    angle = 45.5
    gyro = -0.234
    timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$bytes1 = [System.Text.Encoding]::UTF8.GetBytes($packet1)
$udpClient.Send($bytes1, $bytes1.Length, $remoteEndPoint) | Out-Null

Start-Sleep -Milliseconds 100

# Testar 2: Pacote com ângulo negativo
Write-Host "[OK] Enviando pacote com angulo negativo..." -ForegroundColor Green
$packet2 = @{
    type = "steering"
    angle = -127.3
    gyro = 0.521
    timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$bytes2 = [System.Text.Encoding]::UTF8.GetBytes($packet2)
$udpClient.Send($bytes2, $bytes2.Length, $remoteEndPoint) | Out-Null

Start-Sleep -Milliseconds 100

# Testar 3: Pacote inválido (tipo errado)
Write-Host "[WARN] Enviando pacote invalido (tipo errado)..." -ForegroundColor Yellow
$packet3 = @{
    type = "invalid"
    angle = 0
    gyro = 0
    timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$bytes3 = [System.Text.Encoding]::UTF8.GetBytes($packet3)
$udpClient.Send($bytes3, $bytes3.Length, $remoteEndPoint) | Out-Null

Start-Sleep -Milliseconds 100

# Testar 4: JSON malformado
Write-Host "[WARN] Enviando JSON malformado..." -ForegroundColor Yellow
$packet4 = "{tipo: steering, angulo: 90}"
$bytes4 = [System.Text.Encoding]::UTF8.GetBytes($packet4)
$udpClient.Send($bytes4, $bytes4.Length, $remoteEndPoint) | Out-Null

$udpClient.Close()
Write-Host "`n[OK] Testes finalizados. Verifique a saida do servidor." -ForegroundColor Green
