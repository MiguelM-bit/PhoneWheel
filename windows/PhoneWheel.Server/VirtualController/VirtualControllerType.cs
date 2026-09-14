namespace PhoneWheel.Server.VirtualController;

/// <summary>
/// Tipos de backend de controle virtual suportados.
/// </summary>
public enum VirtualControllerType
{
    /// <summary>vJoy — joystick DirectInput (clássico para volantes).</summary>
    VJoy = 0,

    /// <summary>ViGEmBus — emula um controle Xbox 360 (XInput).</summary>
    Xbox360 = 1
}