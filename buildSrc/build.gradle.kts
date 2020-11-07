plugins {
  `kotlin-dsl`      // use Gradle Kotlin DSL for build files
}

repositories {
  jcenter()
  maven("https://plugins.gradle.org/m2/")
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")
  implementation("com.google.gradle:osdetector-gradle-plugin:1.6.2")
  implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
}
