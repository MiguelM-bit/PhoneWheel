# Estado do projeto

**Fase:** 9 melhorias Android (LT/RT, settings, sensibilidade, preview, gyro)
**Última atualização:** 2026-09-28

## Concluído
- Item 1 — LT/RT via UDP: triggers nunca pulam amostragem (`GamepadManager`), `ButtonSender` com fila FIFO estrita, latch condicionado à entrega, `releaseAll()` sincronizado com `GamepadNetworkBridge.resetButtons()`.
- Item 2 — Menu de settings em tamanho médio (escala dp corrigida), ✕ redundante removido, "← Voltar" top-left; voltar do sistema agora fecha o overlay e retoma os inputs (corrigia inputs presos).
- Item 3 — Scroll por toque no overlay (drag vs tap via touchSlop, clamp, toques bloqueados atrás).
- Item 4 — Slider contínuo de sensibilidade 0.5x..3.0x (default 2.0x, snap 0.1) persistido em `SettingsManager.wheelSensitivity`; curva de resposta em `SteeringProcessor` (ref 90 deg/s, gama 0.85).
- Item 5 — Preview compatível com o novo contrato; layout volante do preview agora configura todos os controles.
- Item 6 — Lifecycle do giroscópio: pause para o listener sem desligar o modo volante; Preview deixou de zerar `wheelModeEnabled` no stop; `cleanup()` no destroy.

## Não executado (instrução do usuário)
- Item 7 — testes automatizados (`gradlew test`): **pendente, usuário vai rodar**.
- Item 8 — APK debug (`gradlew assembleDebug`): **pendente, usuário vai rodar**.

## Testes novos escritos (não executados)
- `network/ButtonSenderTest.kt` (4 testes), `input/GamepadNetworkBridgeTest` (+1), `sensor/SteeringProcessorTest.kt` (6 testes).

## Problemas conhecidos / contexto Windows (para o teste físico)
- Binário do servidor em `windows/.../bin/Debug/net10.0` (16/09 02:05) é **anterior** ao commit `219c365` (16/09 11:30) que adicionou o mapeamento ViGEm 14/15 → triggers. Servidor desatualizado descarta LT/RT com "Botão 14 não existe...". Rebuildar o servidor antes de testar.
- Backend **vJoy** (padrão do `AppConfig`) mapeia 14/15 para botões 15/16 e exige **16 botões** configurados (TESTING.md documenta ≥14). ViGEm (usado no GTA V) trata 14/15 corretamente como triggers.

## Próximos passos
1. Usuário: `cd android && .\gradlew.bat test` — corrigir eventuais erros.
2. Usuário: `.\gradlew.bat assembleDebug` → APK em `android/app/build/outputs/apk/debug/app-debug.apk`.
3. Rebuildar servidor Windows e testar LT/RT no GameSir X5 Lite (ControllerTest.io / GTA V).
