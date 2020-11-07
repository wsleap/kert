import build.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  idea
  `kotlin-dsl`
  kotlin("jvm") apply false  // Enables Kotlin Gradle plugin
  signing
  `maven-publish`
  id("com.github.ben-manes.versions").version("0.20.0")
  id("com.adarshr.test-logger").version("2.1.1")
}

allprojects {
  group = "ws.leap.kettle"
  version = "0.0.1-SNAPSHOT"

  apply {
    plugin("idea")
    plugin("java")
    plugin("kotlin")
    plugin("com.google.protobuf")

    plugin("maven-publish")
    plugin("signing")
    plugin("com.adarshr.test-logger")
  }

  repositories {
    mavenLocal()
    mavenCentral()
  }

  dependencies {
    implementation("io.github.microutils:kotlin-logging:1.12.0")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:${Deps.kotestVersion}")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${Deps.kotestVersion}")
    testImplementation("org.slf4j:slf4j-simple:1.7.25")
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

  publishing {
    repositories {
      maven {
        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        val ossrhUsername: String? by project
        val ossrhPassword: String? by project
        credentials {
          username = ossrhUsername
          password = ossrhPassword
        }
      }
    }
  }

  publishing
  signing {
    isRequired = false
  }
}
