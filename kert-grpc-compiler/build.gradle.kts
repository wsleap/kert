import build.*

description = "The protoc plugin for Kert"

configureGrpcPlugin("protoc-gen-grpc-kert")

dependencies {
  testImplementation(project(":kert-grpc"))
  testImplementation(libs.protobuf.kotlin)
}
