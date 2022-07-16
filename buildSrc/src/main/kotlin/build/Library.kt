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
fun Project.configureLibrary() {
  val api by configurations
  val implementation by configurations
  val dokkaHtmlPlugin by configurations
  val libs = versionCatalog("libs")

  dependencies {
    api(libs.library("kotlin.stdlib"))
    api(libs.library("kotlinx.coroutines"))
    implementation(libs.library("slf4j.api"))

    dokkaHtmlPlugin(libs.library("dokka.kotlin.as.java.plugin"))
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
