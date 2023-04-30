package build

import com.google.gradle.osdetector.OsDetector
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.Copy
import java.io.File
import com.google.protobuf.gradle.*
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.signing.SigningExtension
import org.gradle.kotlin.dsl.*

// os/arch from osDetector https://github.com/trustin/os-maven-plugin
// os/arch for golang https://go.dev/doc/install/source#environment
private fun goArch(arch: String): String {
  return when(arch) {
    "x86_64" -> "amd64"
    "x86_32" -> "386"
    "aarch_64" -> "arm64"
    else -> throw RuntimeException("Unsupported arch $arch")
  }
}

private fun goOs(os: String): String {
  return when(os) {
    "osx" -> "darwin"
    "linux" -> "linux"
    "windows" -> "windows"
    else -> throw RuntimeException("Unsupported os $os")
  }
}

/**
 * Configure the project as a GRPC plugin.
 * It adds support for compiling the plugin to Linux, Windows and MacOS, and publishing support.
 */
fun Project.configureGrpcPlugin(pluginName: String) {
  val osDetector = extensions.getByType(OsDetector::class)

  val testImplementation by configurations
  val libs = versionCatalog("libs")
  dependencies {
    testImplementation(libs.library("kotlin-stdlib"))
  }

  // overwrite os & arch if specified by command line
  val os = if (hasProperty("targetOs")) property("targetOs") as String else osDetector.os
  val arch = if (hasProperty("targetArch")) property("targetArch") as String else osDetector.arch
  val exeSuffix = if(os == "windows") ".exe" else ""

  val pluginPath = "$buildDir/exe/$pluginName${exeSuffix}"
  val artifactStagingPath: File = file("$buildDir/artifacts")

  tasks.register("buildPlugin", Exec::class) {
    workingDir = file("src/main/go")
    environment = environment + mapOf("GOOS" to goOs(os), "GOARCH" to goArch(arch))
    commandLine = listOf("go", "build", "-o", pluginPath, "main.go")
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

  configure<ProtobufExtension> {
    // generatedFilesBaseDir = "$projectDir/gen"
    protoc {
      artifact = "com.google.protobuf:protoc:${libs.version("protobuf")}"
    }
    plugins {
      id("grpc-kert") {
        path = pluginPath
      }
    }
    generateProtoTasks {
      all().forEach { task ->
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
