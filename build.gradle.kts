plugins {
    kotlin("multiplatform") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
}

allprojects {
    group = "com.helico-tech.compose"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "maven-publish")

    afterEvaluate {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/helico-tech/compose-html-multiplatform")
                    credentials {
                        username = project.findProperty("gpr.user") as String?
                            ?: System.getenv("GITHUB_ACTOR")
                        password = project.findProperty("gpr.key") as String?
                            ?: System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}
