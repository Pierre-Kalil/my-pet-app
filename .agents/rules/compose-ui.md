# Compose UI Rule

## Escopo

Use esta rule ao alterar telas Compose, tema, navegacao, imagens, acessibilidade, previews ou comportamento visual.

## Diretrizes

- Use Material 3 e os componentes ja adotados pelo projeto.
- Prefira composables pequenos quando um trecho ficar dificil de ler, mas evite refatoracoes amplas sem necessidade.
- Observe estado com APIs Compose/Lifecycle apropriadas e mantenha eventos como callbacks explicitos.
- Telas Compose devem consumir modelos de `com.example.domain.model` e eventos do `PetViewModel`; nao importe entidades Room, DAO, banco ou repositorio concreto na UI.
- Filtros, selecao de dados, montagem de objetos e regras reutilizaveis devem ficar no dominio/use cases ou no ViewModel, nao dentro de composables.
- Evite texto instrucional dentro do app explicando a propria interface; a tela deve ser autoexplicativa pelo fluxo e pelos controles.
- Garanta que textos longos em portugues nao estourem em telas pequenas; use `maxLines`, `overflow`, pesos e constraints responsivas quando necessario.
- Use botoes para acoes, switches para booleanos, chips/filtros para opcoes curtas e dialogs/sheets apenas quando fizerem sentido no fluxo.
- Preserve acessibilidade: labels claros, contraste adequado, areas de toque confortaveis e conteudo que funcione com fonte aumentada.
- Ao usar imagens com Coil ou recursos locais, defina fallback/placeholder quando a ausencia de imagem prejudicar a experiencia.
- Evite adicionar dependencias de UI sem necessidade; se precisar, registre no version catalog.

## Validacao visual

- Para mudancas simples, rode pelo menos testes JVM existentes.
- Para mudancas visuais relevantes, verifique snapshots Roborazzi quando houver cobertura aplicavel.
- Teste mentalmente telas pequenas e estados vazios, carregando, erro e lista cheia.
