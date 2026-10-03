<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# MeuPet

Aplicativo Android local-first para organizar cuidados, histórico e lembretes de pets.

## Run Locally

**Pré-requisitos:** [Android Studio](https://developer.android.com/studio) com o Android SDK configurado. O projeto inclui o Gradle Wrapper e baixa automaticamente o Gradle 9.3.1 na primeira execução.

1. Abra este diretório no Android Studio e aguarde a sincronização.
2. Para recursos de IA, crie `.env` a partir de `.env.example` e informe `GEMINI_API_KEY`.
3. Compile a versão de depuração:

   ```bash
   ./gradlew :app:assembleDebug
   ```

4. Execute em um emulador ou dispositivo pelo Android Studio, ou instale o APK gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Comandos úteis

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:lintDebug
```
