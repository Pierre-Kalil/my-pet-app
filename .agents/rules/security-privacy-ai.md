# Security, Privacy and AI Rule

## Escopo

Use esta rule ao alterar `AndroidManifest.xml`, rede, Firebase, Gemini/Firebase AI, App Check, secrets, backup, intents, permissoes, notificacoes, autenticacao ou dados sensiveis.

## Segredos e configuracao

- Nunca commite chaves reais, tokens, senhas, keystores privados ou valores de producao.
- Use `.env` local para segredos e mantenha `.env.example` sem valores reais.
- Continue usando o Secrets Gradle Plugin para expor configuracoes ao build quando necessario.
- Senhas de assinatura release devem vir de variaveis de ambiente ou armazenamento seguro, nao de codigo fonte.

## Permissoes e componentes Android

- Adicione permissoes no `AndroidManifest.xml` apenas quando forem estritamente necessarias.
- Para Android 13+, trate `POST_NOTIFICATIONS` com fluxo de permissao em runtime antes de notificar.
- Para activities, services e receivers exportados, valide intent filters, entrada externa e superficie de ataque.
- Use a skill `security/android-intent-security` ao revisar ou adicionar componentes que recebem intents externas.
- Reavalie `allowBackup`, `dataExtractionRules` e `backup_rules` quando dados sensiveis forem adicionados.

## Rede e logging

- Nao registre dados sensiveis em logs, interceptors, exceptions ou mensagens de erro.
- Use HTTPS para APIs remotas e valide modelos de erro sem vazar payloads.
- Ao habilitar OkHttp logging, limite o nivel por build type e evite BODY em producao.

## Firebase AI / Gemini

- Prefira Firebase AI Logic SDK para Android/Kotlin (`com.google.firebase:firebase-ai`) em vez de REST direto com API key no app.
- Use Firebase BoM para controlar versoes Firebase.
- Configure App Check corretamente: debug/local com provedor de debug adequado; producao com Play Integrity quando aplicavel.
- Encapsule chamadas de IA em servico/repositorio proprio, com interface testavel.
- Nao envie dados pessoais desnecessarios para modelos de IA; minimize prompt, contexto e logs.
- Trate falhas, indisponibilidade de rede e respostas inseguras sem quebrar a UI.

## Privacidade

- Colete e persista apenas dados necessarios para a funcionalidade.
- Ao introduzir conta, sincronizacao ou dados remotos, atualize documentacao e avalie Play Data Safety.
- Use a skill `play/play-policy-insights` para auditorias de permissoes, dados e declaracoes de privacidade.
