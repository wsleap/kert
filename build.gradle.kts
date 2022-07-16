import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  idea
  `kotlin-dsl`
  kotlin("jvm") apply false  // Enables Kotlin Gradle plugin
  signing
  `maven-publish`
  alias(libs.plugins.versions)
  alias(libs.plugins.test.logger)
  alias(libs.plugins.kotest)
}

allprojects {
  group = "ws.leap.kert"

  apply {
    plugin("idea")
    plugin("java")
    plugin("kotlin")
    plugin("com.google.protobuf")

    plugin("maven-publish")
    plugin("signing")
    plugin("com.adarshr.test-logger")
    plugin("com.github.ben-manes.versions")

    plugin("org.jetbrains.dokka")
  }

  repositories {
    mavenLocal()
    mavenCentral()
  }

  dependencies {
    // "libs" not working, must use "rootProject.libs" here
    // https://github.com/gradle/gradle/issues/16634
    implementation(rootProject.libs.kotlin.logging)

    testImplementation(rootProject.libs.bundles.kotest)
    testImplementation(rootProject.libs.logback.classic)
  }

  // set target jvm version, otherwise gradle will use the jdk version during compiling for "org.gradle.jvm.version" in module file
  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  sourceSets {
    main {
      java {
        srcDir("$projectDir/gen/main/java")
      }
    }

    test {
      java {
        srcDir("$projectDir/gen/test/java")
      }
    }
  }

  tasks {
    named("clean").configure {
      doFirst {
        delete("gen")
      }
    }

    withType<Test> {
      useJUnitPlatform()
    }

    withType<KotlinCompile> {
      kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", /*"-Xuse-k2"*/)
      }
    }

    withType<Test> {
      testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        events("passed", "skipped", "failed")
      }
    }
  }

  val isSnapshot = (version as String).endsWith("SNAPSHOT", true)
  publishing {
    repositories {
      maven {
        url = if (isSnapshot) {
          uri("https://oss.sonatype.org/content/repositories/snapshots")
        } else {
          uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
        }

        val ossrhUsername: String? by project
        val ossrhPassword: String? by project
        credentials {
          username = ossrhUsername
          password = ossrhPassword
        }
      }
    }
  }

  /**
   * require these properties defined
   * signing.keyId
   * signing.password
   * signing.secretKeyRingFile
   */
  signing {
    isRequired = !isSnapshot
  }

  // configure the test log output
  configure<com.adarshr.gradle.testlogger.TestLoggerExtension> {
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA
    showExceptions = true
    showStandardStreams = false
  }
}
