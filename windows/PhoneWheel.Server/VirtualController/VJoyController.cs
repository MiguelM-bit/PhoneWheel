using System;

namespace PhoneWheel.Server.VirtualController;

/// <summary>
/// Implementação de IVirtualController usando vJoy.
///
/// Responsabilidades:
/// - Conectar ao dispositivo virtual vJoy
/// - Assumir o dispositivo
/// - Enviar valores de direção (eixo X)
/// - Liberar o dispositivo ao desconectar
///
/// Esta classe é específica da implementação vJoy e não deve ser
/// usada diretamente pelo resto da aplicação. Use IVirtualController.
///
/// Notas:
/// - vJoy deve estar instalado no sistema
/// - Pelo menos um dispositivo virtual deve estar criado
/// - Este código assume o dispositivo 1 por padrão
///
/// FASE 1 (ATUAL): Implementação simplificada que simula vJoy
/// Permite testar a arquitetura sem dependência de hardware/driver.
/// A integração real com vJoy será feita quando a API correta estiver disponível.
/// </summary>
public class VJoyController : IVirtualController
{
    private readonly uint _deviceId;
    private bool _connected;
    private bool _disposed;
    private VirtualControllerStatus _status;
    private double _lastSteeringValue = double.NaN;
    private const int VJoyAxisMin = 0;
    private const int VJoyAxisMax = 32767; // Valor típico para vJoy

    /// <summary>
    /// Inicializa a implementação vJoy.
    /// </summary>
    /// <param name="deviceId">
    /// ID do dispositivo vJoy (padrão: 1).
    /// Deve ser um dispositivo virtual existente.
    /// </param>
    public VJoyController(uint deviceId = 1)
    {
        _deviceId = deviceId;
        _connected = false;
        _status = VirtualControllerStatus.Disconnected;
    }

    public VirtualControllerStatus Status => _status;

    public void Connect()
    {
        if (_disposed)
        {
            throw new ObjectDisposedException(nameof(VJoyController));
        }

        if (_connected)
        {
            return; // Já conectado
        }

        try
        {
            // Simulação de verificação de vJoy
            // Em produção, isso verificaria:
            // - Se vJoy está instalado
            // - Versão do driver
            // - Status do dispositivo
            // - Capacidade de adquirir o dispositivo
            
            Console.WriteLine($"[vJoy] Conectando ao dispositivo {_deviceId}...");
            
            // Simular sucesso
            _connected = true;
            _status = VirtualControllerStatus.Connected;
            
            Console.WriteLine($"[vJoy] Dispositivo {_deviceId} conectado com sucesso.");
        }
        catch (Exception ex)
        {
            _status = VirtualControllerStatus.Error;
            throw new VirtualControllerException(
                $"Erro ao conectar ao vJoy: {ex.Message}", ex);
        }
    }

    public void SetSteering(double value)
    {
        if (_disposed)
        {
            throw new ObjectDisposedException(nameof(VJoyController));
        }

        if (!_connected)
        {
            throw new VirtualControllerException(
                "Não conectado ao dispositivo vJoy. Chame Connect() primeiro.");
        }

        // Validar valor normalizado
        if (double.IsNaN(value) || double.IsInfinity(value))
        {
            throw new VirtualControllerException(
                $"Valor de direção inválido: {value}");
        }

        // Limitar a [-1.0, 1.0]
        var clampedValue = Math.Clamp(value, -1.0, 1.0);

        // Evitar atualizações desnecessárias
        if (Math.Abs(_lastSteeringValue - clampedValue) < 0.0001)
        {
            return;
        }

        try
        {
            // Mapear [-1.0, 1.0] para [VJoyAxisMin, VJoyAxisMax]
            // -1.0 → VJoyAxisMin
            //  0.0 → (VJoyAxisMin + VJoyAxisMax) / 2
            // +1.0 → VJoyAxisMax
            var center = (VJoyAxisMin + VJoyAxisMax) / 2.0;
            var range = (VJoyAxisMax - VJoyAxisMin) / 2.0;
            var mappedValue = (int)(center + (clampedValue * range));

            // Garantir que está dentro dos limites
            mappedValue = (int)Math.Clamp(mappedValue, VJoyAxisMin, VJoyAxisMax);

            // Simular envio ao vJoy
            Console.WriteLine(
                $"[vJoy] Dispositivo {_deviceId} - Eixo X: {mappedValue} " +
                $"(valor normalizado: {clampedValue:F3})");

            _lastSteeringValue = clampedValue;
        }
        catch (Exception ex)
        {
            _status = VirtualControllerStatus.Error;
            throw new VirtualControllerException(
                $"Erro ao definir direção: {ex.Message}", ex);
        }
    }

    public void SetButton(int button, bool pressed)
    {
        if (_disposed)
        {
            throw new ObjectDisposedException(nameof(VJoyController));
        }

        if (!_connected)
        {
            throw new VirtualControllerException(
                "Não conectado ao dispositivo vJoy. Chame Connect() primeiro.");
        }

        // Implementação futura
        throw new NotImplementedException(
            "Controle de botões será implementado em uma fase posterior.");
    }

    public void Disconnect()
    {
        if (!_connected)
        {
            return; // Já desconectado
        }

        try
        {
            Console.WriteLine($"[vJoy] Desconectando do dispositivo {_deviceId}...");
            
            _connected = false;
            _status = VirtualControllerStatus.Disconnected;
            _lastSteeringValue = double.NaN;
            
            Console.WriteLine($"[vJoy] Dispositivo {_deviceId} desconectado.");
        }
        catch (Exception ex)
        {
            Console.WriteLine(
                $"[ERR] Erro ao desconectar do vJoy: {ex.Message}");
            _status = VirtualControllerStatus.Error;
        }
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        Disconnect();
        _disposed = true;
    }
}


