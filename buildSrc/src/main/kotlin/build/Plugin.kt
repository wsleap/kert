package build

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.Copy
import java.io.File
import com.google.protobuf.gradle.*
import org.gradle.api.plugins.JavaPluginExtension
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

/**
 * Configure the project as a GRPC plugin.
 * It adds support for compiling the plugin to Linux, Windows and MacOS, and publishing support.
 */
fun Project.grpcPluginSupport(pluginName: String) {
  val testImplementation by configurations
  dependencies {
    testImplementation(kotlin("stdlib-jdk8"))
  }

  // overwrite os & arch if specified by command line
  val os = if (hasProperty("targetOs")) property("targetOs") as String else Consts.os
  val arch = if (hasProperty("targetArch")) property("targetArch") as String else Consts.arch
  val exeSuffix = if(os == "windows") ".exe" else ""

  val pluginPath = "$buildDir/exe/$pluginName${exeSuffix}"
  val artifactStagingPath: File = file("$buildDir/artifacts")

  tasks.register("buildPlugin") {
    doLast {
      exec {
        environment = environment + mapOf("GOOS" to goOs(os), "GOARCH" to goArch(arch))
        commandLine = listOf("go", "build", "-o", pluginPath, "src/main/go/main.go")
      }
    }
  }

  tasks.register("buildArtifacts", Copy::class) {
    dependsOn("buildPlugin")
    from("$buildDir/exe") {
      if (os != "windows") {
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
      id("grpc-kert") {
        path = pluginPath
      }
    }
    generateProtoTasks {
      all().forEach { task ->
        task.dependsOn("buildPlugin")
        task.inputs.file(pluginPath)
      }
      ofSourceSet("test").forEach { task ->
        task.plugins {
          id("grpc-kert") {
            // enable this to write the input request to disk
            // option("write_input=true")
          }
        }
      }
    }
  }

  configure<JavaPluginExtension> {
    sourceSets(closureOf<SourceSetContainer> {
      getByName("test").java.srcDir("$projectDir/gen/test/kotlin")
    })
  }

  configure<PublishingExtension> {
    publications {
      create<MavenPublication>("maven") {
        artifactId = pluginName
        artifact(file("$artifactStagingPath/$pluginName.exe")) {
          classifier = "$os-$arch"
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
