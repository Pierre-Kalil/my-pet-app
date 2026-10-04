package com.example

import android.app.Application
import com.example.data.onboarding.DataStoreOnboardingStore
import com.example.data.onboarding.OnboardingStore
import com.example.data.onboarding.onboardingDataStore

/** Composição raiz dos stores locais compartilhados pela aplicação. */
class MeuPetApplication : Application() {
    val onboardingStore: OnboardingStore by lazy {
        DataStoreOnboardingStore(onboardingDataStore)
    }
}
