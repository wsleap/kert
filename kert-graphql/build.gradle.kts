import build.*

plugins {
  id("com.expediagroup.graphql") version "5.3.2"
}

description = "Kert GraphQL support"

configureLibrary()

dependencies {
  api(project(":kert-http"))
  api(libs.graphql.kotlin.server)
  api(libs.graphql.kotlin.client)

  implementation(libs.jackson.databind)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.jackson.module.afterburner)
}

//graphql {
//  client {
//    // Gradle build fails if server is not running
//    endpoint = "http://localhost:8500/graphql"
//    packageName = "ws.leap.kert.graphql.example"
//    queryFiles = listOf(file("${project.projectDir}/src/test/resources/ExampleQuery.graphql"))
//  }
//}
