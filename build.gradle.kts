import build.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  idea
  `kotlin-dsl`
  kotlin("jvm") apply false  // Enables Kotlin Gradle plugin
  signing
  `maven-publish`
  id("com.github.ben-manes.versions").version("0.38.0")
  id("com.adarshr.test-logger").version("3.0.0")
  id("io.kotest") version "0.3.8"
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
    implementation("io.github.microutils:kotlin-logging:2.0.6")

    testImplementation("io.kotest:kotest-framework-engine-jvm:${Deps.kotestVersion}")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:${Deps.kotestVersion}")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${Deps.kotestVersion}")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    // testImplementation("org.slf4j:slf4j-simple:1.7.25")
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
        freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
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
}
