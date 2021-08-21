import build.*
import com.google.protobuf.gradle.*

description = "Kert GRPC support"

librarySupport()

dependencies {
  api(project(":kert-http"))
  api("com.google.protobuf:protobuf-java:${Deps.protobufVersion}")
  api("io.grpc:grpc-protobuf:${Deps.grpcJavaVersion}")
  api("com.google.protobuf:protobuf-kotlin:${Deps.protobufVersion}")

  api("javax.annotation:javax.annotation-api:1.3.2")

  // generateTestProto needs compiler binary
  compileOnly(project(":kert-grpc-compiler"))

  testImplementation("io.grpc:grpc-stub:${Deps.grpcJavaVersion}")
  testImplementation("io.grpc:grpc-netty:${Deps.grpcJavaVersion}")
}

protobuf {
  generatedFilesBaseDir = "$projectDir/gen"
  protoc {
    artifact = "com.google.protobuf:protoc:${Deps.protobufVersion}"
  }
  plugins {
    id("grpc-kert") {
      path = "$rootDir/kert-grpc-compiler/build/exe/protoc-gen-grpc-kert${Consts.exeSuffix}"
    }
    // generate java version for performance comparison
    id("grpc-java") {
      artifact = "io.grpc:protoc-gen-grpc-java:${Deps.grpcJavaVersion}"
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
