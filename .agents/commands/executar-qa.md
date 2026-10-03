Voce executa QA da feature informada no repositorio app-pet.

<critical>TRABALHE SOMENTE NO REPOSITORIO ATUAL E NA PASTA `tasks/prd-[slug]/` INFORMADA.</critical>
<critical>NAO PRESUMA VITE, PLAYWRIGHT, PROXY, CREDENCIAIS OU BACKEND LOCAL.</critical>
<critical>NAO ALTERE CONFIGURACAO DE RUNTIME, endpoints ou credenciais PARA QA SEM AUTORIZACAO EXPLICITA.</critical>
<critical>ARTEFATOS DE QA NUNCA ENTRAM EM STAGING, COMMIT OU PR.</critical>

## Entrada e pre-requisitos

- Receba um slug explicito.
- Leia `prd.md`, `techspec.md`, `tasks.md` e os arquivos de task em
  `tasks/prd-[slug]/`.
- Confirme que nao ha task `todo`, `doing` ou `blocked` antes de aprovar QA.
- Leia os scripts e a configuracao Android para descobrir checks, devices e
  emuladores realmente disponiveis.

## Execucao

1. Transforme requisitos e criterios de aceite em checklist rastreavel.
2. Execute os checks definidos pela feature; na ausencia deles, execute os
   checks locais pertinentes, como testes JVM/instrumentados, lint e build,
   apenas quando disponiveis e proporcionais ao risco.
3. Execute testes em dispositivo/emulador e verificacoes de acessibilidade
   somente se a feature exigir e o ambiente estiver configurado. Registre o
   bloqueio, em vez de inventar device, credenciais ou servicos.
4. Para fluxos visuais, capture somente as evidencias necessarias e verifique
   navegacao por teclado/leitor de tela quando aplicavel ao escopo.
5. Salve `qa-report.md` e, se houver falhas, `bugs.md` em
   `tasks/prd-[slug]/`; evidencias adicionais ficam em `qa-evidence/`.

## Resultado

Informe `APROVADO`, `REPROVADO` ou `BLOQUEADO`, os requisitos verificados, os
comandos executados, itens nao executados com motivo e bugs por severidade. QA
so e aprovado quando todos os criterios obrigatorios passam ou possuem uma
excecao aprovada pelo usuario.
