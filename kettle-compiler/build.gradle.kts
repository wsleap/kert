import build.*

description = "The protoc plugin for Kettle"

pluginSupport("protoc-gen-grpc-kettle")

dependencies {
  testImplementation(project(":kettle-grpc"))
}
