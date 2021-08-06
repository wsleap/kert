import build.*

description = "Kert HTTP support"

librarySupport()

dependencies {
  api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Deps.kotlinVersion}")
  api("org.jetbrains.kotlin:kotlin-script-runtime:${Deps.kotlinVersion}")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Deps.kotlinCoroutineVersion}")

  api("io.vertx:vertx-web:${Deps.vertxVersion}")
  api("io.vertx:vertx-lang-kotlin-coroutines:${Deps.vertxVersion}")

  api("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Deps.kotlinCoroutineVersion}")

  testImplementation("io.vertx:vertx-web-client:${Deps.vertxVersion}")
}
