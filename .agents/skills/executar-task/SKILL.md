---
name: executar-task
description: Executa uma tarefa identificada, com validação e atualização de status.
disable-model-invocation: true
---

Voce e o executor de tasks.

Sua responsabilidade e receber um identificador explicito no formato `/executar-task <id>`, localizar a task correspondente, implementar o escopo solicitado e devolver um resultado claro para o orquestrador ou para o usuario.

<critical>ESTE COMANDO IMPLEMENTA A TASK DIRETAMENTE. NAO DELEGUE A IMPLEMENTACAO PARA O ORQUESTRADOR.</critical>
<critical>O ID DA TASK E OBRIGATORIO. NAO ESCOLHA "A PROXIMA TASK" POR CONTA PROPRIA.</critical>
<critical>SE NAO ENCONTRAR A TASK OU HOUVER AMBIGUIDADE, PARE E PECA ESCLARECIMENTO.</critical>
<critical>ANTES DE EDITAR, LEIA O ARQUIVO DA TASK, O `tasks.md`, O `prd.md` E O `techspec.md` DA MESMA PASTA.</critical>
<critical>AO INICIAR, ATUALIZE O FRONTMATTER DA TASK PARA `status: doing`.</critical>
<critical>AO FINALIZAR, ATUALIZE O FRONTMATTER DA TASK PARA `status: done` OU `status: blocked`.</critical>
<critical>NAO CONSIDERE A TASK CONCLUIDA SEM VALIDAR O ESCOPO ALTERADO COM O MENOR CONJUNTO DE VERIFICACOES ADEQUADO.</critical>
<critical>NAO EXECUTE TESTES DE INTEGRACAO AUTOMATICAMENTE, EXCETO QUANDO A TASK EXIGIR OU O USUARIO SOLICITAR.</critical>
<critical>SE HOUVER `git add`, COMMIT OU PREPARACAO DE PR, INCLUA APENAS ALTERACOES DE IMPLEMENTACAO. IGNORE `tasks/`, `templates/` E ARTEFATOS DE REVIEW, QA OU PLANEJAMENTO.</critical>

## Modelo Esperado

- Quando este comando for disparado pelo orquestrador, use `gpt-5.6-luna` com `reasoning_effort: medium`.
- Nao assuma `Auto`.

## Entrada Esperada

Use sempre o identificador informado na chamada, por exemplo:

```text
/executar-task 3.0
```

Se o identificador nao for fornecido:

1. pare imediatamente;
2. informe que o comando exige um `id`;
3. peca ao usuario ou ao orquestrador para reenviar a chamada corretamente.

## Localizacao Dos Artefatos

Para a task informada:

1. localize `tasks/prd-*/[id]_task.md`;
2. se houver mais de um resultado, nao escolha arbitrariamente;
3. use a pasta da task encontrada como base para ler:
   - `prd.md`
   - `techspec.md`
   - `tasks.md`
   - snapshots `prd-vN.md`, `techspec-vN.md` ou `tasks-vN.md` se a task mandar ler versoes especificas;
4. leia tambem `AGENTS.md`, as rules relevantes em `.agents/rules/` e skills aplicaveis a area alterada.

## Higiene De Commit

Se esta task envolver staging, commit, preparacao de branch ou apoio a PR:

- inclua apenas alteracoes de implementacao relacionadas a codigo, configuracao de runtime ou testes que facam parte da entrega;
- nao faca `git add` de `tasks/`;
- nao faca `git add` de `templates/`;
- nao faca `git add` de artefatos gerados durante planejamento, review, QA ou evidencia manual;
- trate como artefatos a ignorar, por exemplo, arquivos `*.review.md`, pastas `qa-evidence/`, screenshots, notas de bug, PRDs, techspecs, task lists e documentos auxiliares;
- se houver duvida sobre algum arquivo, aplique a regra: so entra no commit o que for alteracao de implementacao.

## Sequencia Obrigatoria

### 1. Preparacao

1. Confirmar qual arquivo `[id]_task.md` sera usado.
2. Ler por completo:
   - a task
   - o `tasks.md`
   - o `prd.md`
   - o `techspec.md`
3. Identificar:
   - objetivo da task
   - dependencias declaradas em `bloqueado_por` ou em `tasks.md`
   - criterios de sucesso
   - testes e Definition of Done especificos da task
4. Verificar se existe bloqueio real que impede a execucao.
5. Se houver bloqueio, marcar `status: blocked`, explicar o motivo e parar.

### 2. Resumo Inicial

Antes de implementar, apresente um resumo curto com:

```text
ID da task: [id]
Titulo: [titulo]
Pasta da feature: [tasks/prd-...]
Dependencias: [lista]
Objetivo: [resumo]
Validacoes esperadas: [lint/testes/build/manual]
Riscos principais: [lista curta]
```

### 3. Planejamento Da Abordagem

Defina um plano curto e concreto antes de editar, por exemplo:

```text
1. Revisar os arquivos da feature e localizar o fluxo atual.
2. Implementar as mudancas minimas necessarias no ponto correto da arquitetura.
3. Validar com checks proporcionais ao risco e atualizar o status da task.
```

### 4. Implementacao

Implemente a task seguindo estes principios:

- leia arquivos vizinhos antes de alterar padroes;
- reutilize componentes, hooks, enums, modelos, helpers e services existentes;
- mantenha a logica perto da feature, salvo quando a base ja usar compartilhamento claro;
- preserve contratos entre UI, `services`, `model`, `reducer`, `reducer-bco`, `reducer-brs`, `reducer-ccb` e sagas;
- evite refatoracoes amplas sem necessidade direta da task;
- nao use gambiarras nem crie abstracoes novas sem necessidade clara;
- respeite i18n, toasts, validacao de formularios, Sass e padroes locais da pasta alterada.

Se a task mencionar skills especificas, siga-as. Quando aplicavel, use:

- `project-context`
- `redux-saga-endpoint`
- `form-validation`
- `sass-styling`
- `dependency-security-review` se `package.json` ou `yarn.lock` mudarem

### 5. Validacao

A validacao deve ser proporcional ao risco e guiada primeiro pela propria task.

Ordem de decisao:

1. seguir os testes e checks exigidos no `[id]_task.md`;
2. depois complementar com o menor conjunto de verificacoes adequado;
3. explicitar o que nao foi executado e por que.

Checks recomendados neste projeto:

- `ReadLints` nos arquivos alterados
- `yarn lint` quando houver edicao relevante de codigo
- testes focados quando existirem ou quando a task exigir
- `yarn build` quando a mudanca tocar tipos, imports amplos, rotas, integracoes ou comportamento com risco maior
- validacao manual quando o comportamento depender fortemente de interface ou fluxo visual

Nao trate coverage como obrigatoria por padrao do comando. So cobre coverage quando:

- a task exigir explicitamente; ou
- a infraestrutura da area permitir validar isso de forma confiavel.

Nao rode testes de integracao automaticamente em toda task.

### 6. Review

Nao dependa de `@executar-review`, porque esse comando nao faz parte deste repositorio.

Se houver uma etapa de review solicitada pelo usuario, pelo orquestrador ou pela propria task:

1. prefira usar o agente `reviewer` quando existir um artefato de planejamento compativel;
2. se esse review nao puder ser executado, explique a limitacao no resumo final;
3. nao invente um comando inexistente para satisfazer essa etapa.

### 7. Atualizacao De Status

Ao iniciar a task:

- atualizar o frontmatter de `[id]_task.md` para `status: doing`.

Ao concluir com sucesso:

- atualizar o frontmatter de `[id]_task.md` para `status: done`;
- marcar subtarefas internas como concluidas, se existirem;
- atualizar `tasks.md` somente se o indice geral da pasta precisar refletir o novo status.
- se houver preparacao de staging, garantir que apenas arquivos de implementacao sejam incluidos.

Se houver impedimento real:

- atualizar o frontmatter de `[id]_task.md` para `status: blocked`;
- registrar de forma objetiva o motivo do bloqueio;
- nao continuar para outra task.

## Regras De Saida

Ao final, sempre devolva:

```text
Task: [id]
Status final: [done|blocked]
Arquivos principais alterados: [lista]
Resumo da implementacao: [curto]
Validacoes executadas: [lista]
Validacoes nao executadas: [lista + motivo]
Riscos residuais: [lista curta ou "nenhum relevante"]
```

## O Que Nao Fazer

- nao escolher outra task diferente da que foi informada;
- nao assumir paralelismo;
- nao finalizar alterando apenas `tasks.md` e esquecendo o `[id]_task.md`;
- nao afirmar que revisou algo que nao revisou;
- nao incluir em staging ou commit artefatos de `tasks/`, `templates/`, review ou QA;
- nao rodar suites caras por padrao sem necessidade;
- nao remover codigo legado sem indicacao explicita da task ou do usuario;
- nao prosseguir quando houver ambiguidade sobre o arquivo da task ou sobre dependencias bloqueantes.
