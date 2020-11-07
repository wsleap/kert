package build

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension

fun Project.librarySupport() {
  dependencies {
    "api"(kotlin("stdlib-jdk8"))
    "api"("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Deps.kotlinCoroutineVersion}")
    "implementation"("org.slf4j:slf4j-api:1.7.25")
  }

  val sourceSets = extensions.getByName("sourceSets") as SourceSetContainer
  tasks {
    register<Jar>("sourcesJar") {
      from(sourceSets["main"].allJava)
      archiveClassifier.set("sources")
    }

//    register<Jar>("javadocJar") {
//      from(tasks.javadoc)
//      archiveClassifier.set("javadoc")
//    }
  }

  configure<PublishingExtension> {
    publications {
      create<MavenPublication>("maven") {
        from(components["java"])
        artifact(tasks["sourcesJar"])
        //artifact(tasks["javadocJar"])

        versionMapping {
          usage("java-api") {
            fromResolutionOf("runtimeClasspath")
          }
          usage("java-runtime") {
            fromResolutionResult()
          }
        }

        pom(Consts.pom)
      }
    }
  }

  val publishing = extensions.getByName("publishing") as PublishingExtension
  configure<SigningExtension> {
    sign(publishing.publications["maven"])
  }
}
