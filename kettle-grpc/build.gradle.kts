import build.*
import com.google.protobuf.gradle.*

description = "Kettle GRPC support"

librarySupport()

dependencies {
  api(project(":kettle-http"))
  api("com.google.protobuf:protobuf-java:${Deps.protobufVersion}")
  api("io.grpc:grpc-protobuf:${Deps.grpcJavaVersion}")

  api("javax.annotation:javax.annotation-api:1.3.2")

  testImplementation("io.grpc:grpc-stub:${Deps.grpcJavaVersion}")
  testImplementation("io.grpc:grpc-netty:${Deps.grpcJavaVersion}")
}

protobuf {
  generatedFilesBaseDir = "$projectDir/gen"
  protoc {
    artifact = "com.google.protobuf:protoc:${Deps.protobufVersion}"
  }
  plugins {
    id("grpc-kettle") {
      path = "$rootDir/kettle-compiler/build/exe/protoc-gen-grpc-kettle${Consts.exeSuffix}"
    }
    // generate java version for performance comparison
    id("grpc-java") {
      artifact = "io.grpc:protoc-gen-grpc-java:${Deps.grpcJavaVersion}"
    }
  }
  generateProtoTasks {
    ofSourceSet("test").forEach {
      it.plugins {
        id("grpc-kettle")
        id("grpc-java")
      }
    }
  }
}

sourceSets {
  test {
    java {
      srcDir("$projectDir/gen/test/grpc-kettle")
      srcDir("$projectDir/gen/test/grpc-java")
    }
  }
}
