import build.*

description = "The protoc plugin for Kert"

grpcPluginSupport("protoc-gen-grpc-kert")

dependencies {
  testImplementation(project(":kert-grpc"))
}
