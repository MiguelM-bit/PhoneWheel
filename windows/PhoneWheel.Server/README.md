# PhoneWheel.Server

Servidor Windows em C#/.NET responsável por receber os dados de direção enviados pelo aplicativo Android via UDP (ver `../../protocol/protocol.md`) e, futuramente, controlar um dispositivo virtual.

## Requisitos

- .NET SDK 10.0+

## Estrutura do Projeto

```
windows/
├── PhoneWheel.sln
└── PhoneWheel.Server/
    ├── Program.cs           # Ponto de entrada da aplicação console
    ├── Network/              # Recepção de pacotes UDP (a implementar)
    ├── Input/                 # Processamento dos dados recebidos (a implementar)
    ├── VirtualController/    # Integração com dispositivo virtual, ex. vJoy (a implementar)
    ├── Models/
    │   └── SteeringPacket.cs # Modelo do pacote definido no protocolo
    └── PhoneWheel.Server.csproj
```

## Compilar e Executar

```bash
cd windows
dotnet build PhoneWheel.sln
dotnet run --project PhoneWheel.Server/PhoneWheel.Server.csproj
```

## Status Atual

Aplicação console mínima e compilável, com a arquitetura preparada para separar responsabilidades:

- **Network**: futura recepção de pacotes UDP na porta `5005`
- **Input**: futuro processamento dos dados recebidos
- **VirtualController**: futura integração com controle virtual (ex. vJoy)
- **Models**: `SteeringPacket`, espelhando o protocolo (`type`, `angle`, `gyro`, `timestamp`)

Nenhuma lógica de rede, controle virtual ou interface gráfica foi implementada ainda.

---

**Versão**: 0.1.0 | **Status**: Estrutura Inicial
