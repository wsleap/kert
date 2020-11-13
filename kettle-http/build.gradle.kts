import build.*

description = "Kettle HTTP support"

librarySupport()

dependencies {
  api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Deps.kotlinVersion}")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Deps.kotlinCoroutineVersion}")

  api("io.vertx:vertx-web:${Deps.vertxVersion}")
  api("io.vertx:vertx-lang-kotlin-coroutines:${Deps.vertxVersion}")

  api("com.fasterxml.jackson.core:jackson-databind:2.11.1")
}
