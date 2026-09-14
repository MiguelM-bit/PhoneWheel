using System;

namespace PhoneWheel.Server.VirtualController;

/// <summary>
/// Fábrica de controladores virtuais: cria a implementação correta
/// de <see cref="IVirtualController"/> com base no tipo selecionado.
/// </summary>
public static class VirtualControllerFactory
{
    /// <summary>
    /// Cria um controlador virtual do tipo especificado.
    /// </summary>
    /// <param name="type">Tipo de backend (vJoy ou Xbox 360 via ViGEmBus).</param>
    /// <param name="deviceId">ID do dispositivo vJoy (ignorado para Xbox 360).</param>
    /// <returns>Instância de <see cref="IVirtualController"/>.</returns>
    public static IVirtualController Create(VirtualControllerType type, uint deviceId = 1)
    {
        return type switch
        {
            VirtualControllerType.VJoy => new VJoyController(deviceId),
            VirtualControllerType.Xbox360 => new ViGEmController(),
            _ => throw new ArgumentOutOfRangeException(
                nameof(type), type, "Tipo de controle virtual desconhecido.")
        };
    }
}