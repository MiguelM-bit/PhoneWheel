#!/usr/bin/env pwsh
# Script de teste: pacote de eixo analógico (AXIS) via UDP
# 1. Envia CONNECT e aguarda CONNECT_ACK (autenticação)
# 2. Envia sequência de AXIS para left_x, left_y, right_x, right_y
#    com valores -1.0, 0.0 e 1.0 (verifica que nenhum eixo interfere em outro)
# 3. Envia STEERING para manter a conexão viva no watchdog

$serverIP = "127.0.0.1"
$serverPort = 5005
$connectTimeoutMs = 3000

Write-Host ""
Write-Host "[TEST] Pacote de Eixo Analógico (UDP)" -ForegroundColor Cyan
Write-Host ""

# Criar cliente UDP
$udpClient = New-Object System.Net.Sockets.UdpClient
$udpClient.Client.ReceiveTimeout = $connectTimeoutMs

$remoteEndPoint = New-Object System.Net.IPEndPoint ([System.Net.IPAddress]::Parse($serverIP)), $serverPort

# --- Passo 1: CONNECT (handshake) ---
Write-Host "[SEND] Enviando CONNECT para $serverIP`:$serverPort ..." -ForegroundColor Gray
$connect = @{
    type = "connect"
    device = "PhoneWheel"
    version = "2.0"
    timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$connectBytes = [System.Text.Encoding]::UTF8.GetBytes($connect)
$udpClient.Send($connectBytes, $connectBytes.Length, $remoteEndPoint) | Out-Null

# Aguardar CONNECT_ACK
$authenticated = $false
$deadline = [System.DateTime]::UtcNow.AddMilliseconds($connectTimeoutMs)

while ([System.DateTime]::UtcNow -lt $deadline) {
    try {
        $response = $udpClient.Receive([ref]$remoteEndPoint)
        $json = [System.Text.Encoding]::UTF8.GetString($response)
        $ack = $json | ConvertFrom-Json

        if ($ack.type -eq "connect_ack") {
            Write-Host "[OK] CONNECT_ACK recebido de $($remoteEndPoint.Address):$($remoteEndPoint.Port)" -ForegroundColor Green
            Write-Host "     $json" -ForegroundColor Gray
            $authenticated = $true
            break
        }
        else {
            Write-Host "[WARN] Pacote recebido com tipo inesperado: $($ack.type)" -ForegroundColor Yellow
        }
    }
    catch [System.Net.Sockets.SocketException] {
        # Timeout: reenviar CONNECT
        $udpClient.Send($connectBytes, $connectBytes.Length, $remoteEndPoint) | Out-Null
    }
}

if (-not $authenticated) {
    Write-Host ""
    Write-Host "[FAIL] Nenhum CONNECT_ACK recebido. Verifique se o servidor esta rodando na porta $serverPort." -ForegroundColor Red
    $udpClient.Close()
    exit 1
}

# --- Passo 2: Sequência de AXIS ---
Write-Host ""
Write-Host "[SEND] Enviando sequência de eixos (left_x, left_y, right_x, right_y)..." -ForegroundColor Gray

$axes = @("left_x", "left_y", "right_x", "right_y")
$values = @(-1.0, 0.0, 1.0)

foreach ($axis in $axes) {
    foreach ($value in $values) {
        $packet = @{
            type = "axis"
            axis = $axis
            value = $value
            timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
        } | ConvertTo-Json

        $bytes = [System.Text.Encoding]::UTF8.GetBytes($packet)
        $udpClient.Send($bytes, $bytes.Length, $remoteEndPoint) | Out-Null
        Write-Host "[SEND] Eixo $axis = $value" -ForegroundColor Gray

        Start-Sleep -Milliseconds 100
    }
}

# --- Passo 3: STEERING para manter a conexão viva ---
Write-Host ""
Write-Host "[SEND] Enviando STEERING (manter conexão)..." -ForegroundColor Gray
$steering = @{
    type = "steering"
    angle = 0.0
    gyro = 0.0
    timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$steeringBytes = [System.Text.Encoding]::UTF8.GetBytes($steering)
$udpClient.Send($steeringBytes, $steeringBytes.Length, $remoteEndPoint) | Out-Null

$udpClient.Close()
Write-Host ""
Write-Host "[OK] Teste de eixos finalizado. Verifique a saída do servidor." -ForegroundColor Green