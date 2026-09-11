// PhoneWheel.Server
// Aplicação de console responsável por futuramente receber, via UDP, os
// pacotes de direção enviados pelo aplicativo Android (ver protocol/protocol.md).
//
// Nesta etapa o projeto apenas inicializa e exibe uma mensagem de status.
// A recepção de pacotes UDP, o processamento de entrada e o controle
// virtual (vJoy) serão implementados em etapas posteriores, respectivamente
// nos namespaces PhoneWheel.Server.Network, PhoneWheel.Server.Input e
// PhoneWheel.Server.VirtualController.

Console.WriteLine("PhoneWheel.Server");
Console.WriteLine("Aguardando implementação da recepção de pacotes UDP...");
