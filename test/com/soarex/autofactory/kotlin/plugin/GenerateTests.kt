package com.soarex.autofactory.kotlin.plugin

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import com.soarex.autofactory.kotlin.plugin.runners.AbstractBoxTest
import com.soarex.autofactory.kotlin.plugin.runners.AbstractDiagnosticTest

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {
            testClass<AbstractDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractBoxTest> {
                model("box")
            }
        }
    }
}
