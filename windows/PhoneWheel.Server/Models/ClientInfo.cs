using System.Net;

namespace PhoneWheel.Server.Models;

/// <summary>
/// Informações sobre um cliente conectado.
/// </summary>
public record ClientInfo
{
    public DateTimeOffset ConnectedAt { get; init; }
    public DateTimeOffset LastPacketAt { get; init; }
    public int SteeringPacketCount { get; init; }
    public IPEndPoint? RemoteEndPoint { get; init; }
}

