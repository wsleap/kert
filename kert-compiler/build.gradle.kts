import build.*

description = "The protoc plugin for Kert"

pluginSupport("protoc-gen-grpc-kert")

dependencies {
  testImplementation(project(":kert-grpc"))
}
