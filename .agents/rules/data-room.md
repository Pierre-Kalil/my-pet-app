# Data and Room Rule

## Escopo

Use esta rule ao alterar entidades Room, DAO, banco, repositorio, queries, dados de exemplo ou regras de persistencia.

## Fluxo esperado

1. Ajuste modelos de dominio em `com.example.domain.model` se a informacao fizer parte do contrato da app.
2. Ajuste entidades Room em `PetEntities.kt` usando sufixo `Entity`.
3. Atualize mappers em `PetMappers.kt`.
4. Atualize queries e operacoes em `PetDao.kt`.
5. Atualize `PetDatabase.kt` e incremente a versao do banco quando o schema mudar.
6. Exponha a operacao pelo contrato `PetCareRepository` e pela implementacao `PetRepository.kt`.
7. Consuma no `PetViewModel.kt` via use case quando houver regra de negocio.
8. Renderize ou acione pela UI Compose.

## Diretrizes

- Prefira `Flow` para listas e dados observaveis.
- Mantenha entidades persistidas separadas dos modelos de dominio e do estado de tela.
- Entidades Room devem ficar restritas ao pacote `data`; UI e use cases nao devem importar `*Entity`, `PetDao` ou `PetDatabase`.
- `PetRepository` deve mapear entidades para modelos de dominio antes de expor dados ao restante do app.
- Use queries explicitas e ordenacao deterministica para listas exibidas ao usuario.
- Evite datas fixas antigas em dados de exemplo; prefira datas relativas ou atuais quando fizer sentido.
- Ao mudar schema, considere o impacto de `fallbackToDestructiveMigration()`: aceitavel no prototipo, inadequado para producao com dados reais.
- Nao armazene segredos, tokens, chaves de API ou credenciais em Room.
- Se armazenar dados sensiveis de usuario ou pet, documente o risco e avalie criptografia, backup e minimizacao de dados.

## Testes

- Adicione ou ajuste testes quando mudar queries, filtros, ordenacao, insercao, delecao, atualizacao ou derivacao de listas.
- Cubra pelo menos o caso feliz e um caso limite relevante para regras novas.
