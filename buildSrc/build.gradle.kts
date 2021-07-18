plugins {
  `kotlin-dsl`      // use Gradle Kotlin DSL for build files
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.20")
  implementation("com.google.gradle:osdetector-gradle-plugin:1.6.2")
  implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.32")
}
