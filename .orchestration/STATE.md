# Estado do projeto

**Fase:** Modo controle Android
**Última atualização:** 2026-09-20

## Progresso
- Kit de orquestração instalado.
- Plano aprovado: landscape atual travado, imersivo moderno, pause sem desligar o volante.

## Em andamento
- TASK-001 ControllerWindowHelper + tema + Manifest.

## Concluído
- Pesquisa da arquitetura e decisão de orientação.

## Bloqueios
- Nenhum.

## Próximos passos
1. TASK-001 helper/tema/Manifest/testes.
2. TASK-002 e TASK-003 em paralelo.
3. Integrar e validar.

## Problemas conhecidos
- `releaseAllControls()` zera `wheelModeEnabled`, então o resume não retoma o giroscópio.
- Release de rede na ControllerActivity não zera o visual.
- Imersivo não é reaplicado após troca de layout.
