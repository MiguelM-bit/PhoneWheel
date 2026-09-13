using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace PhoneWheel.Server.Network;

/// <summary>
/// Utilitários para a descoberta de servidor na rede local.
/// </summary>
public static class ServerDiscovery
{
    /// <summary>
    /// Obtém o endereço IPv4 local do servidor, preferindo uma interface
    /// na mesma subnet do cliente. Retorna "127.0.0.1" como último recurso.
    /// </summary>
    /// <param name="clientAddress">Endereço IP do cliente que enviou o DISCOVER.</param>
    public static string GetLocalIPv4(IPAddress clientAddress)
    {
        var candidates = GetCandidates();

        // Preferir interface na mesma subnet do cliente
        foreach (var candidate in candidates)
        {
            if (IsSameSubnet(clientAddress, candidate.Address, candidate.Mask))
            {
                return candidate.Address.ToString();
            }
        }

        // Fallback: primeira interface IPv4 não-loopback
        if (candidates.Count > 0)
        {
            return candidates[0].Address.ToString();
        }

        // Último recurso
        return "127.0.0.1";
    }

    /// <summary>
    /// Obtém o primeiro endereço IPv4 não-loopback do servidor.
    /// Retorna "127.0.0.1" como último recurso.
    /// </summary>
    public static string GetLocalIPv4()
    {
        var candidates = GetCandidates();

        if (candidates.Count > 0)
        {
            return candidates[0].Address.ToString();
        }

        return "127.0.0.1";
    }

    /// <summary>
    /// Enumera as interfaces de rede ativas e coleta endereços IPv4 não-loopback.
    /// </summary>
    private static List<(IPAddress Address, IPAddress Mask)> GetCandidates()
    {
        var candidates = new List<(IPAddress Address, IPAddress Mask)>();

        foreach (var networkInterface in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (networkInterface.OperationalStatus != OperationalStatus.Up)
            {
                continue;
            }

            if (networkInterface.NetworkInterfaceType == NetworkInterfaceType.Loopback)
            {
                continue;
            }

            foreach (var unicastAddress in networkInterface.GetIPProperties().UnicastAddresses)
            {
                if (unicastAddress.Address.AddressFamily != AddressFamily.InterNetwork)
                {
                    continue;
                }

                if (IPAddress.IsLoopback(unicastAddress.Address))
                {
                    continue;
                }

                candidates.Add((unicastAddress.Address, unicastAddress.IPv4Mask));
            }
        }

        return candidates;
    }

    /// <summary>
    /// Verifica se dois endereços IPv4 estão na mesma subnet usando a máscara.
    /// </summary>
    private static bool IsSameSubnet(IPAddress clientAddress, IPAddress serverAddress, IPAddress mask)
    {
        if (clientAddress.AddressFamily != AddressFamily.InterNetwork)
        {
            return false;
        }

        var clientBytes = clientAddress.GetAddressBytes();
        var serverBytes = serverAddress.GetAddressBytes();
        var maskBytes = mask.GetAddressBytes();

        for (int i = 0; i < clientBytes.Length; i++)
        {
            if ((clientBytes[i] & maskBytes[i]) != (serverBytes[i] & maskBytes[i]))
            {
                return false;
            }
        }

        return true;
    }
}