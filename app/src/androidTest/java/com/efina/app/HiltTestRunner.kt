package com.efina.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * The runner named in `testInstrumentationRunner`.
 *
 * It swaps in Hilt's test application so an instrumented test can build the
 * app's graph without a real Activity. Without this class the configured runner
 * does not exist and `connectedAndroidTest` cannot start at all.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
