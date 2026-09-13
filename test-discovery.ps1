#!/usr/bin/env pwsh
# Script de teste: descoberta de servidor via broadcast UDP
# Envia "discover" para 255.255.255.255:5005 e valida a resposta "discover_ack"

$serverPort = 5005
$broadcastIP = "255.255.255.255"
$discoveryTimeoutMs = 2000

Write-Host ""
Write-Host "[TEST] Descoberta de Servidor (UDP Broadcast)" -ForegroundColor Cyan
Write-Host ""

# Criar socket UDP com broadcast habilitado
$udpClient = New-Object System.Net.Sockets.UdpClient
$udpClient.EnableBroadcast = $true
$udpClient.Client.ReceiveTimeout = $discoveryTimeoutMs

$remoteEndPoint = New-Object System.Net.IPEndPoint ([System.Net.IPAddress]::Parse($broadcastIP)), $serverPort

# Montar pacote discover
$discover = @{
    type = "discover"
    device = "PhoneWheel"
    version = "2.0"
    timestamp = [System.DateTimeOffset]::Now.ToUnixTimeMilliseconds()
} | ConvertTo-Json

$bytes = [System.Text.Encoding]::UTF8.GetBytes($discover)

Write-Host "[SEND] Enviando DISCOVER para $broadcastIP`:$serverPort ..." -ForegroundColor Gray
Write-Host "       $discover" -ForegroundColor Gray

# Enviar broadcast algumas vezes durante a janela de descoberta
$deadline = [System.DateTime]::UtcNow.AddMilliseconds($discoveryTimeoutMs)
$found = $false

while ([System.DateTime]::UtcNow -lt $deadline) {
    $udpClient.Send($bytes, $bytes.Length, $remoteEndPoint) | Out-Null

    while ([System.DateTime]::UtcNow -lt $deadline) {
        try {
            $response = $udpClient.Receive([ref]$remoteEndPoint)
            $json = [System.Text.Encoding]::UTF8.GetString($response)
            $ack = $json | ConvertFrom-Json

            if ($ack.type -eq "discover_ack") {
                Write-Host ""
                Write-Host "[OK] DISCOVER_ACK recebido de $($remoteEndPoint.Address):$($remoteEndPoint.Port)" -ForegroundColor Green
                Write-Host "     $json" -ForegroundColor Gray
                Write-Host ""

                # Validações
                $valid = $true
                if ($ack.device -ne "PhoneWheel") { Write-Host "[FAIL] device incorreto: $($ack.device)" -ForegroundColor Red; $valid = $false }
                if ($ack.version -ne "2.0") { Write-Host "[FAIL] version incorreta: $($ack.version)" -ForegroundColor Red; $valid = $false }
                if ([string]::IsNullOrWhiteSpace($ack.server_ip)) { Write-Host "[FAIL] server_ip ausente" -ForegroundColor Red; $valid = $false }
                if ($ack.server_port -ne $serverPort) { Write-Host "[FAIL] server_port incorreto: $($ack.server_port)" -ForegroundColor Red; $valid = $false }
                if ($ack.timestamp -le 0) { Write-Host "[FAIL] timestamp invalido" -ForegroundColor Red; $valid = $false }

                if ($valid) {
                    Write-Host "[OK] Servidor encontrado: $($ack.server_ip):$($ack.server_port)" -ForegroundColor Green
                    Write-Host "[OK] Todas as validacoes passaram!" -ForegroundColor Green
                    $found = $true
                    break
                }
            }
            else {
                Write-Host "[WARN] Pacote recebido com tipo inesperado: $($ack.type)" -ForegroundColor Yellow
            }
        }
        catch [System.Net.Sockets.SocketException] {
            # Timeout: nenhuma resposta, reenviar broadcast
            break
        }
    }

    if ($found) { break }
    Start-Sleep -Milliseconds 300
}

if (-not $found) {
    Write-Host ""
    Write-Host "[FAIL] Nenhum servidor respondeu ao DISCOVER." -ForegroundColor Red
    Write-Host "       Verifique se o servidor Windows esta rodando na porta $serverPort." -ForegroundColor Yellow
}

$udpClient.Close()
Write-Host ""
Write-Host "[TEST] Descoberta finalizada." -ForegroundColor Green