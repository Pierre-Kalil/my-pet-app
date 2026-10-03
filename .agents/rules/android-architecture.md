# Android Architecture Rule

## Escopo

Use esta rule ao alterar codigo Kotlin em qualquer modulo do MeuPet (`:app`, `:core:*`, `:feature:*`).

## Padrao arquitetural atual

O projeto segue uma arquitetura multimódulo em camadas:

`Compose UI (feature) -> ViewModel da feature -> use cases/contratos de dominio -> implementacao em :core:data -> Room`

### Modulos

| Modulo | Responsabilidade |
|--------|------------------|
| `:app` | Entrada (`MeuPetApplication`, `MainActivity`), tema, navegacao raiz (`AppNavigationState`), composicao das features. Sem regras de negocio. |
| `:core:domain` | Modelos puros, contratos (`PetCareRepository`, stores), use cases. Kotlin/JVM sem Android, Compose ou Room. |
| `:core:data` | Room, entidades, DAO, mappers, repositorio, stores em memoria. Implementa contratos de `:core:domain`. |
| `:core:notifications` | Implementacao de `ReminderScheduler`, AlarmManager, receivers e publicacao de notificacoes locais. |
| `:feature:auth` | Onboarding, login, cadastro, Credential Manager. |
| `:feature:home` | Resumo inicial. |
| `:feature:pets-routine` | Cadastro/selecao de pets e rotina. |
| `:feature:reminders-history` | Lembretes e historico. |
| `:feature:market-services` | Ofertas e servicos. |
| `:feature:profile-settings` | Perfil, preferencias e tema. |

## Limites de dependencia (Gradle)

Direcao permitida:

```
:feature:*            ->  :core:domain  (+ :core:notifications quando publicar alertas)
:core:data           ->  :core:domain
:core:notifications  ->  :core:domain (desvio planejado: contratos/modelos puros para agendamento por ID)
:app                 ->  todas as features + :core:*
```

**Proibido:**

- Features dependendo de `:core:data`, Room, DAO ou implementacoes concretas de repositorio.
- Features dependendo de outras features.
- `:core:domain` dependendo de Android, Compose, Room ou Hilt.
- Ciclos entre modulos.

Verifique com `./gradlew :feature:<nome>:dependencies` ou compilacao independente do modulo.

## Injecao de dependencias (Hilt)

- Hilt e o unico mecanismo de DI para componentes abrangidos.
- Modulos Hilt ficam em `:app/di`, `:core:data/di`, `:core:notifications/di` e `:feature:auth/di` (quando necessario).
- Activities usam `@AndroidEntryPoint`; ViewModels usam `@HiltViewModel`.
- Dependencias sao expostas por contratos de dominio, nunca por implementacoes concretas nas features.
- Nao use `AppContainer`, factories manuais ou singletons globais novos.

## Estado e eventos de UI

- **Estado duravel:** `private MutableStateFlow` + `public StateFlow` imutavel por ViewModel.
- **Eventos pontuais:** `private MutableSharedFlow(replay = 0)` + `public SharedFlow`; consumidos no maximo uma vez por emissao.
- A UI coleta estado com `collectAsStateWithLifecycle()`.
- Coletores de eventos ficam ativos apenas em lifecycle `STARTED` ou superior.
- Navegacao de destino atual permanece em estado (`AppNavigationState`); comandos efemeros sao eventos.
- A UI nao dispara efeitos genericos (ex.: `triggerPushAlert`); comunica intencoes ao ViewModel.

## Stores compartilhados

Estado compartilhado entre features e mediado por contratos em `:core:domain`:

- `OnboardingStore` — conclusao do onboarding inicial
- `ConsentStore` — consentimento opt-in de telemetria
- `SelectedPetStore` — pet selecionado
- `AppPreferencesStore` — preferencias (ex.: tema)

Implementacoes vivem em `:core:data`. Features acessam apenas os contratos.

## Diretrizes

- Preserve o fluxo em camadas; nao pule o dominio para acessar Room a partir da UI.
- Mantenha regras de negocio em use cases ou helpers testaveis; ViewModel coordena estado e eventos.
- Use `viewModelScope` para operacoes assincronas; evite scopes soltos.
- Nao bloqueie a main thread com I/O, Room ou operacoes longas.
- Prefira tipos imutaveis (`data class`) para estado.
- Preserve nomes, textos e fluxos em portugues do Brasil.
- Para mudancas em UI de feature, edite o modulo `:feature:*` correspondente; `:app` concentra apenas composicao.

## Criterios de conformidade (checklist)

Ao revisar PR ou contribuicao:

- [ ] Nenhuma feature importa classes de `:core:data`, Room ou entidades `*Entity`.
- [ ] Nenhuma feature depende de outra feature no Gradle.
- [ ] ViewModels usam `@HiltViewModel` e recebem contratos, nao implementacoes concretas de persistencia.
- [ ] Estado exposto via `StateFlow` somente leitura; eventos via `SharedFlow(replay = 0)`.
- [ ] UI usa `collectAsStateWithLifecycle()` para estado e coleta eventos com lifecycle awareness.
- [ ] `./gradlew test` e `./gradlew assembleDebug` passam.
- [ ] Novas dependencias estao em `gradle/libs.versions.toml`.

## Antes de finalizar

- Confirme que callbacks da UI continuam explicitos.
- Novos modelos exibidos vivem em `:core:domain`, nao como entidades Room.
- Regras de negocio novas possuem teste unitario quando houver risco de regressao.
- Rode testes relevantes conforme `testing-validation.md`.
