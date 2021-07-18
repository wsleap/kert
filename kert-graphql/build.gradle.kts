import build.*

plugins {
  id("com.expediagroup.graphql") version "4.1.1"
}

description = "Kert GraphQL support"

librarySupport()

dependencies {
  api(project(":kert-http"))
  api("com.expediagroup:graphql-kotlin-server:4.1.1")
  api("com.expediagroup:graphql-kotlin-client:4.1.1")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
  implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.12.3")
}

//graphql {
//  client {
//    // Gradle build fails if server is not running
//    endpoint = "http://localhost:8500/graphql"
//    packageName = "ws.leap.kert.graphql.example"
//    queryFiles = listOf(file("${project.projectDir}/src/test/resources/ExampleQuery.graphql"))
//  }
//}
