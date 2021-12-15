import build.*

plugins {
  id("com.expediagroup.graphql") version "5.3.1"
}

description = "Kert GraphQL support"

librarySupport()

dependencies {
  val graphqlKotlinVersion = "5.3.1"
  val jacksonVersion = "2.13.0"

  api(project(":kert-http"))
  api("com.expediagroup:graphql-kotlin-server:${graphqlKotlinVersion}")
  api("com.expediagroup:graphql-kotlin-client:${graphqlKotlinVersion}")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
  implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
  implementation("com.fasterxml.jackson.module:jackson-module-afterburner:${jacksonVersion}")
}

//graphql {
//  client {
//    // Gradle build fails if server is not running
//    endpoint = "http://localhost:8500/graphql"
//    packageName = "ws.leap.kert.graphql.example"
//    queryFiles = listOf(file("${project.projectDir}/src/test/resources/ExampleQuery.graphql"))
//  }
//}
