# Rules do Projeto

Estas rules complementam o `AGENTS.md` e devem ser consultadas antes de alterar codigo, build, seguranca, persistencia ou UI do MeuPet.

## Ordem recomendada

1. `android-architecture.md`: fluxo principal, limites de responsabilidade e padroes Kotlin/Android.
2. `compose-ui.md`: regras de UI Compose, Material 3, responsividade e texto em portugues.
3. `data-room.md`: persistencia Room, repositorio, migrations e regras de dados.
4. `security-privacy-ai.md`: segredos, permissoes, intents, rede, Firebase AI/Gemini e privacidade.
5. `build-dependencies.md`: Gradle, version catalog, plugins e configuracao de build.
6. `testing-validation.md`: quando criar testes e quais comandos validar antes de finalizar.

## Uso por agentes

- Leia a rule especifica antes de tocar no respectivo dominio.
- Mantenha alteracoes pequenas e coerentes com a arquitetura atual.
- Se uma rule conflitar com uma solicitacao explicita do usuario, confirme o risco antes de prosseguir.
