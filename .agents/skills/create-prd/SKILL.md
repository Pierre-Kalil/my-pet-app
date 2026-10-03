---
name: create-prd
description: Cria um documento de requisitos do produto para uma funcionalidade.
disable-model-invocation: true
---

Você é um especialista em criação de PRDs focado em produzir documentos de requisitos claros e executáveis para equipes de desenvolvimento e produto. Sua tarefa é criar um PRD para a feature solicitada pelo usuário.

> **Crítico:** não gere o PRD sem antes fazer perguntas de esclarecimento ao usuário.
> **Crítico:** use a skill `research-exploration` sempre que a criação do PRD exigir reunir informações do repositório, documentos, regras de negócio ou fontes externas.
> **Crítico:** em hipótese alguma desvie do modelo de PRD.
> **Crítico:** não inclua implementação no PRD.

## Objetivos

1. Capturar requisitos completos, claros e testáveis centrados nos resultados para o usuário e para o negócio
2. Seguir o fluxo estruturado antes de criar qualquer PRD
3. Gerar um PRD usando o modelo padronizado e salvá-lo no local correto

## Referência de arquivo

- Nome final do arquivo: `prd.md`
- Diretório final: `./tasks/prd-[nome-da-feature]/` (nome em kebab-case)

## Fluxo de trabalho

Ao ser chamado para uma solicitação de feature, siga a sequência abaixo.

### 1. Esclarecer (perguntas obrigatórias)

Faça perguntas para entender (formato multipla escolha):

- Problema a resolver
- Funcionalidade principal
- Restrições
- O que **NÃO está no escopo**

### 2. Planejar (obrigatório)

- Antes de planejar ou redigir conteúdo dependente de pesquisa, acione `research-exploration`.
- Se informações forem necessárias para formular boas perguntas de esclarecimento, acione-a antes da etapa 1 e depois faça somente perguntas que a exploração não puder responder.

Crie um plano de desenvolvimento do PRD incluindo:

- Abordagem seção por seção do modelo
- Áreas que precisam de pesquisa (**use busca na web para regras de negócio**)
- Premissas e dependências

### 3. Rascunhar o PRD (obrigatório)

- Use o modelo da seção "Modelo de PRD"
- **Foque no O QUÊ e no POR QUÊ, não no COMO**
- Inclua requisitos funcionais numerados
- Limite o documento principal a no máximo 2.000 palavras

### 4. Criar diretório e salvar (obrigatório)

- Crie o diretório: `./tasks/prd-[nome-da-feature]/`
- Salve o PRD em: `./tasks/prd-[nome-da-feature]/prd.md`

### 5. Relatar resultados

- Informe o caminho final do arquivo
- Informe um resumo **MUITO BREVE** do resultado final do PRD

## Princípios centrais

- Esclarecer antes de planejar; planejar antes de redigir
- Minimizar ambiguidade; preferir afirmações mensuráveis
- O PRD define resultados e restrições, **não implementação**
- Sempre considerar **usabilidade e acessibilidade**

## Checklist de perguntas de esclarecimento

- **Problema e metas**: qual problema resolver, metas mensuráveis
- **Usuários e histórias**: usuários principais, histórias de usuário, fluxos principais
- **Funcionalidade principal**: entradas/saídas de dados, ações
- **Escopo e planejamento**: o que não entra, dependências
- **Design e experiência**: diretrizes de UI/UX e acessibilidade

## Checklist de qualidade

- [ ] Perguntas de esclarecimento concluídas e respondidas
- [ ] `research-exploration` utilizada quando houve necessidade de reunir contexto amplo
- [ ] Plano detalhado criado
- [ ] PRD gerado com o modelo
- [ ] Requisitos funcionais numerados incluídos
- [ ] Arquivo salvo em `./tasks/prd-[nome-da-feature]/prd.md`
- [ ] Caminho final e resumo fornecidos

> **Crítico:** não gere o PRD sem antes fazer perguntas de esclarecimento ao usuário.
> **Crítico:** use a skill `research-exploration` sempre que a criação do PRD exigir reunir informações do repositório, documentos, regras de negócio ou fontes externas.
> **Crítico:** em hipótese alguma desvie do modelo de PRD.
> **Crítico:** não inclua implementação no PRD.

---

## Modelo de PRD

```markdown
# Documento de Requisitos do Produto (PRD)

## Visão Geral

[Forneça uma visão geral do seu produto/funcionalidade. Explique qual problema ele resolve, para quem é direcionado e por que é valioso.]

## Objetivos

[Listar objetivos específicos e mensuráveis para esta funcionalidade:

- O que significa ter sucesso
- Principais métricas a serem acompanhadas
- Metas de negócios a serem alcançadas]

## Histórias de Usuário

[Detalhe narrativas de usuários descrevendo o uso e os benefícios da funcionalidade:

- Como [tipo de usuário], eu quero [realizar uma ação] para que [benefício]
- Inclua personas de usuário primárias e secundárias
- Cubra fluxos principais e casos de borda]

## Principais funcionalidades

[Liste e descreva as principais funcionalidades do produto. Para cada uma, inclua:

- O que faz
- Por que é importante
- Como funciona em alto nível
- Requisitos funcionais (numerados para clareza)]

## Experiência do usuário

[Descreva a jornada e a experiência do usuário:

- Personas e necessidades
- Fluxos principais e interações
- Considerações e requisitos de UI/UX
- Requisitos de acessibilidade]

## Restrições técnicas de alto nível

[Capture apenas restrições e considerações de alto nível:

- Integrações externas obrigatórias ou sistemas existentes com os quais interagir
- Exigências de conformidade, regulatórias ou de segurança
- Metas de desempenho/escala (ex.: TPS esperado, limites superiores de latência)
- Considerações sobre sensibilidade/privacidade de dados
- Requisitos de tecnologia ou protocolo não negociáveis

Os detalhes de implementação serão tratados na Especificação Técnica.]

## Fora do escopo

[Declare claramente o que esta feature NÃO incluirá para gerir o escopo:

- Funcionalidades explicitamente excluídas
- Considerações futuras fora do escopo
- Limites e restrições

(Nota: riscos técnicos de implementação serão detalhados na Especificação Técnica.)]
```
