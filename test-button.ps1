#!/usr/bin/env pwsh
# Script de teste: pacote de botão (BUTTON) via UDP
# 1. Envia CONNECT e aguarda CONNECT_ACK (autenticação)
# 2. Envia sequência de BUTTON (press/release) para os botões 0..3
# 3. Envia STEERING para manter a conexão viva no watchdog

$serverIP = "127.0.0.1"
$serverPort = 5005
$connectTimeoutMs = 3000

Write-Host ""
Write-Host "[TEST] Pacote de Botão (UDP)" -ForegroundColor Cyan
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

# --- Passo 2: Sequência de BUTTON ---
Write-Host ""
Write-Host "[SEND] Enviando sequência de botões (press/release)..." -ForegroundColor Gray

$buttons = @(0, 1, 2, 3)

foreach ($button in $buttons) {
    # Pressionar
    $press = @{
        type = "button"
        button = $button
        pressed = $true
        timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    } | ConvertTo-Json

    $pressBytes = [System.Text.Encoding]::UTF8.GetBytes($press)
    $udpClient.Send($pressBytes, $pressBytes.Length, $remoteEndPoint) | Out-Null
    Write-Host "[SEND] Botão $button pressionado" -ForegroundColor Gray

    Start-Sleep -Milliseconds 150

    # Liberar
    $release = @{
        type = "button"
        button = $button
        pressed = $false
        timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    } | ConvertTo-Json

    $releaseBytes = [System.Text.Encoding]::UTF8.GetBytes($release)
    $udpClient.Send($releaseBytes, $releaseBytes.Length, $remoteEndPoint) | Out-Null
    Write-Host "[SEND] Botão $button liberado" -ForegroundColor Gray

    Start-Sleep -Milliseconds 150
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
Write-Host "[OK] Teste de botões finalizado. Verifique a saída do servidor." -ForegroundColor Green