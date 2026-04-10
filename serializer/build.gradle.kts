plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(project(":core"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
