# Testing and Validation Rule

## Escopo

Use esta rule antes de finalizar qualquer mudanca de codigo, UI, dados, build, seguranca ou comportamento.

## Quando adicionar ou ajustar testes

- Mudancas em regras de negocio em ViewModels de feature.
- Mudancas em persistencia Room, DAO, queries, ordenacao ou filtros em `:core:data`.
- Mudancas em use cases ou contratos em `:core:domain`.
- Correcoes de bug que podem regredir.
- Mudancas em derivacao de listas, estados vazios, datas, notificacoes ou permissoes.
- Alteracoes visuais relevantes com cobertura Roborazzi em `:app`.

## Comandos uteis

```bash
./gradlew test                          # todos os testes JVM
./gradlew assembleDebug                 # build debug
./gradlew connectedAndroidTest          # testes instrumentados (requer dispositivo/emulador)
./gradlew :core:data:test               # Room, repositorio, stores
./gradlew :feature:auth:test            # ViewModel e smoke de auth
./gradlew test --tests "com.example.feature.auth.AuthViewModelTest"
./gradlew :app:testDebugUnitTest        # Roborazzi e testes do app
```

## Suite de validacao arquitetural

Apos mudancas estruturais ou em fronteiras modulares, verifique:

| Verificacao | Comando / local |
|-------------|-----------------|
| Testes JVM completos | `./gradlew test` |
| Build debug | `./gradlew assembleDebug` |
| Room e repositorio | `:core:data:test` |
| ViewModels e smoke | `:feature:*:test` |
| Bindings Hilt | `connectedAndroidTest` → `HiltBindingsInstrumentedTest` |
| Snapshots visuais | `:app:testDebugUnitTest` → `GreetingScreenshotTest` |
| Fronteiras Gradle | features nao listam `:core:data` em `build.gradle.kts` |

## Smoke tests JVM (fluxos criticos)

Sem emulador, estes testes validam coordenacao ViewModel → contratos:

- `AuthFlowSmokeTest` — autenticacao ate sessao ativa
- `PetsRoutineFlowSmokeTest` — cadastro de pet
- `RemindersHistoryFlowSmokeTest` — criacao de lembrete

## Diretrizes

- Para mudancas pequenas de logica, rode testes focados do modulo afetado.
- Para mudancas amplas ou compartilhadas, rode `./gradlew test`.
- Para build, manifest, dependencias ou codigo Android, rode `./gradlew assembleDebug`.
- Para UI Compose com impacto visual, rode Roborazzi em `:app`.
- Robolectric usa SDK maximo suportado (atualmente 35); nao force API acima do suportado.
- Se nao for possivel rodar um comando, registre o motivo claramente.

## Checklist final

- Testes relevantes passaram ou falhas foram explicadas.
- Nenhum segredo foi adicionado.
- Novas dependencias estao no version catalog.
- Permissoes novas sao necessarias e tratadas em runtime.
- Texto e experiencia continuam em portugues do Brasil.
- Fronteiras modulares preservadas (consulte `android-architecture.md`).
