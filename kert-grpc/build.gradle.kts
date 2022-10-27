import build.*
import com.google.protobuf.gradle.*

description = "Kert GRPC support"

configureLibrary()

dependencies {
  api(project(":kert-http"))
  api(libs.protobuf.java)
  api(libs.protobuf.kotlin)
  api(libs.grpc.protobuf)

  api(libs.javax.annotation.api)

  // generateTestProto needs compiler binary
  compileOnly(project(":kert-grpc-compiler"))

  testImplementation(libs.grpc.stub)
  testImplementation(libs.grpc.netty)
}

protobuf {
  generatedFilesBaseDir = "$projectDir/gen"
  protoc {
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }
  plugins {
    id("grpc-kert") {
      val osDetector = extensions.getByType(com.google.gradle.osdetector.OsDetector::class)
      val exeSuffix = if(osDetector.os == "windows") ".exe" else ""

      path = "$rootDir/kert-grpc-compiler/build/exe/protoc-gen-grpc-kert${exeSuffix}"
    }
    // generate java version for performance comparison
    id("grpc-java") {
      artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
    }
  }
  generateProtoTasks {
    // protos used by library self
    ofSourceSet("main").forEach { task ->
      task.builtins {
        id("kotlin")
      }
      task.plugins {
        id("grpc-kert")
      }
    }

    // protos used in test (grpc-java is enabled for comparison)
    ofSourceSet("test").forEach { task ->
      task.builtins {
        id("kotlin")
      }
      task.plugins {
        id("grpc-kert")
        id("grpc-java")
      }
    }
  }
}

sourceSets {
  test {
    java {
      srcDir("$projectDir/gen/test/grpc-kert")
      srcDir("$projectDir/gen/test/grpc-java")
    }
  }
}
