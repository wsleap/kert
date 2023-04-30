plugins {
  `kotlin-dsl`      // use Gradle Kotlin DSL for build files
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.plugin.kotlin)
  implementation(libs.plugin.os.detector)
  implementation(libs.plugin.protobuf)
  implementation(libs.plugin.dokka)
}
