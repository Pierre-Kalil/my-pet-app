# Build and Dependencies Rule

## Escopo

Use esta rule ao alterar `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, plugins, SDK, build types ou dependencias.

## Diretrizes

- Use sempre o Gradle wrapper do repositorio.
- Declare novas versoes em `gradle/libs.versions.toml` e use aliases em `app/build.gradle.kts`.
- Prefira BoMs ja existentes para Compose e Firebase quando a biblioteca pertencer ao respectivo ecossistema.
- Evite dependencias novas para problemas pequenos resolviveis com AndroidX/Kotlin/Compose ja presentes.
- Mantenha dependencias comentadas apenas quando houver motivo claro de reativacao futura.
- Ao alterar AGP, Kotlin, KSP, compileSdk ou targetSdk, valide compatibilidade entre plugins.
- Nao adicione repositorios remotos desconhecidos sem necessidade e justificativa.
- Preserve configuracao de secrets sem expor valores reais.

## Release e otimizacao

- Mudancas em signing configs devem manter senhas fora do repositorio.
- Se alterar minify, R8 ou ProGuard, use a skill `performance/r8-analyzer` quando houver risco de regras amplas, reflexao ou aumento de APK.
- Ao mexer em build, manifest ou codigo Android, rode `./gradlew assembleDebug` quando viavel.

## Skills relacionadas

- `build/agp/agp-9-upgrade`: upgrades de Android Gradle Plugin.
- `performance/r8-analyzer`: analise de R8/ProGuard e otimizacao.
- `devtools/android-cli`: instalacao de SDK, emuladores e interacao com dispositivos.
