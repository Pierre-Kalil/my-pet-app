---
name: research-exploration
description: Orquestra leituras, pesquisas e explorações extensas com subagentes especializados para reunir evidências antes de análises, decisões ou artefatos como PRDs e Tech Specs. Use quando a tarefa exigir compreender muitas fontes, mapear um repositório, investigar alternativas ou reduzir incertezas relevantes; não use para consultas pontuais resolvidas com uma leitura simples.
---

# Pesquisa e exploração

Reúna contexto amplo e verificável sem sobrecarregar o agente principal. Esta skill apoia qualquer tarefa de alta carga informacional; PRDs e Tech Specs são casos prioritários, não limites de escopo.

## Regra de delegação

Quando esta skill for acionada, delegue a exploração a subagentes. Para **todo** subagente criado por este fluxo, configure obrigatoriamente:

```yaml
model: gpt-5.6-luna
reasoning_effort: high
```

Como a seleção explícita de modelo não é compatível com `fork_turns: all`, use `fork_turns: none` ou um número positivo de turnos. Inclua no pedido todo o contexto mínimo para a investigação ser autônoma.

Não selecione o papel embutido `explorer`, pois ele fixa outro nível de esforço. Crie um subagente comum e descreva no pedido que sua missão é exploração somente leitura. Exemplo de configuração:

```yaml
task_name: explore_architecture
agent_type: default
fork_turns: none
model: gpt-5.6-luna
reasoning_effort: high
```

Não delegue a decisão final nem a redação integral do artefato. O agente principal continua responsável por cruzar evidências, resolver conflitos, identificar lacunas e produzir a síntese adequada ao pedido.

## Fluxo

### 1. Delimitar a investigação

- Defina quais decisões ou seções do resultado dependem de informação ainda não confirmada.
- Separe fatos conhecidos, hipóteses e perguntas em aberto.
- Escolha fontes compatíveis com a tarefa: repositório, documentação interna, artefatos existentes, documentação oficial, papers ou web.

### 2. Dividir em frentes independentes

- Crie uma frente por eixo com resultado próprio e pouca sobreposição.
- Em explorações amplas, use ao menos dois subagentes e execute frentes independentes em paralelo quando houver capacidade.
- Exemplos de eixos: arquitetura e fluxo de dados; UI e jornada; persistência e integrações; testes e observabilidade; regras de negócio; bibliotecas e alternativas externas.
- Evite vários subagentes varrendo o repositório inteiro sem foco. Dê a cada um diretórios, perguntas e limites claros.

### 3. Encomendar evidências úteis

Cada pedido a um subagente deve informar:

- objetivo e perguntas que ele precisa responder;
- escopo de arquivos, fontes ou domínio sob sua responsabilidade;
- formato esperado: achados, evidências, incertezas, riscos e recomendações;
- obrigação de citar arquivos e linhas, quando a fonte for local, ou links diretos, quando for externa;
- proibição de editar arquivos, salvo autorização explícita do usuário e atribuição clara de propriedade.

Para fatos atuais, regras externas ou decisões de dependência, solicite pesquisa na web e priorize fontes primárias ou documentação oficial. Não imponha uma quantidade artificial de buscas: busque até haver evidência suficiente e fontes independentes quando a afirmação for controversa ou de alto impacto.

### 4. Sintetizar e fechar lacunas

- Compare os relatórios e diferencie evidência de inferência.
- Resolva contradições consultando diretamente as fontes ou enviando uma pergunta de acompanhamento ao subagente responsável.
- Faça uma segunda rodada apenas para lacunas que possam alterar o resultado.
- Registre o que permanece desconhecido e transforme somente decisões realmente dependentes disso em perguntas ao usuário.
- Entregue uma síntese orientada ao artefato ou decisão, sem despejar relatórios brutos no resultado final.

## Aplicação em artefatos

### PRD

Use as frentes para levantar contexto do produto, comportamento atual, usuários, regras de negócio, restrições e evidências externas. Preserve a fronteira do PRD: os achados sustentam **o quê** e **por quê**, sem antecipar design de implementação.

### Tech Spec

Use as frentes para mapear arquitetura, módulos, contratos, fluxo de dados, persistência, integrações, concorrência, tratamento de erros, testes, observabilidade, regras aplicáveis e alternativas técnicas. Explore antes dos esclarecimentos técnicos para que as perguntas ao usuário sejam apenas sobre decisões não inferíveis do projeto.

## Critérios de conclusão

A exploração está suficiente quando:

- cada pergunta relevante possui resposta apoiada por evidência ou está marcada como lacuna;
- arquivos, símbolos, fontes e relações importantes estão identificados;
- conflitos entre fontes foram resolvidos ou explicitados;
- a síntese distingue fatos, inferências, riscos e recomendações;
- novas leituras provavelmente não mudariam as decisões do artefato.
