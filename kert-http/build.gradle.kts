import build.*

description = "Kert HTTP support"

configureLibrary()

dependencies {
  api(libs.bundles.kotlin)

  api(libs.kotlinx.coroutines)
  api(libs.kotlinx.coroutines.slf4j)

  api(libs.vertx.web)
  api(libs.vertx.lang.kotlin.coroutines)

  testImplementation(libs.vertx.web.client)
}
