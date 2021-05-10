package build

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension

/**
 * Config the project as a library.
 * It adds Kotlin dependency, and publishing support.
 */
fun Project.librarySupport() {
  val api by configurations
  val implementation by configurations
  val dokkaHtmlPlugin by configurations

  dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Deps.kotlinCoroutineVersion}")
    implementation("org.slf4j:slf4j-api:1.7.25")

    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.32")
  }

  val sourceSets = extensions.getByName("sourceSets") as SourceSetContainer
  tasks {
    register<Jar>("sourcesJar") {
      from(sourceSets["main"].allJava)
      archiveClassifier.set("sources")
      from(sourceSets.getByName("main").allSource)
    }

    register<Jar>("javadocJar") {
      archiveClassifier.set("javadoc")
      from(tasks["dokkaHtml"])
      dependsOn(tasks["dokkaHtml"])
    }
  }

  configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
  }

  configure<PublishingExtension> {
    publications {
      create<MavenPublication>("maven") {
        artifactId = project.name
        from(components["java"])

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
