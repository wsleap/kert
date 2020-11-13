package build

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.Copy
import java.io.File
import com.google.protobuf.gradle.*
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.signing.SigningExtension

private fun goArch(arch: String): String {
  return when(arch) {
    "x86_64" -> "amd64"
    "x86_32" -> "386"
    else -> throw RuntimeException("Unsupported arch $arch")
  }
}

private fun goOs(os: String): String {
  return when(os) {
    "osx" -> "darwin"
    else -> os
  }
}

fun Project.pluginSupport(pluginName: String) {
  dependencies {
    "testImplementation"(kotlin("stdlib-jdk8"))
  }

  val pluginPath = "$buildDir/exe/$pluginName${Consts.exeSuffix}"
  val artifactStagingPath: File = file("$buildDir/artifacts")

  tasks.register("buildPlugin") {
    doLast {
      exec {
        environment = environment + mapOf("GOOS" to goOs(Consts.os), "GOARCH" to goArch(Consts.arch))
        commandLine = listOf("go", "build", "-o", pluginPath, "src/main/go/main.go")
      }
    }
  }

  tasks.register("buildArtifacts", Copy::class) {
    dependsOn("buildPlugin")
    from("$buildDir/exe") {
      if (Consts.os != "windows") {
        rename("(.+)", "$1.exe")
      }
    }
    into(artifactStagingPath)
  }

  protobuf {
    generatedFilesBaseDir = "$projectDir/gen"
    protoc {
      artifact = "com.google.protobuf:protoc:${Deps.protobufVersion}"
    }
    plugins {
      id("kotlin") {
        path = pluginPath
      }
    }
    generateProtoTasks {
      all().forEach { task ->
        task.dependsOn("buildPlugin")
        task.inputs.file(pluginPath)
      }
      ofSourceSet("test").forEach {
        it.plugins {
          id("kotlin") {
            // enable this to write the input request to disk
            // option("write_input=true")
          }
        }
      }
    }
  }

  configure<JavaPluginConvention> {
    sourceSets(closureOf<SourceSetContainer> {
      getByName("test").java.srcDir("$projectDir/gen/test/kotlin")
    })
  }

  configure<PublishingExtension> {
    publications {
      create<MavenPublication>("maven") {
        artifactId = pluginName
        artifact(file("$artifactStagingPath/$pluginName.exe")) {
          classifier = Consts.os + "-" + Consts.arch
          extension = "exe"
          builtBy(tasks.named("buildArtifacts"))
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
