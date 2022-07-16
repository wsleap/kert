import build.*

description = "The protoc plugin for Kert"

configureGrpcPlugin("protoc-gen-grpc-kert")

dependencies {
  testImplementation(project(":kert-grpc"))
  testImplementation(libs.protobuf.kotlin)
}

tasks {
  register("setup") {
    exec {
      commandLine = listOf(
        "go", "get", "-t",
        "github.com/golang/protobuf/proto",
        "github.com/golang/protobuf/protoc-gen-go/descriptor",
        "github.com/golang/protobuf/protoc-gen-go/plugin"
      )
    }
  }
}
