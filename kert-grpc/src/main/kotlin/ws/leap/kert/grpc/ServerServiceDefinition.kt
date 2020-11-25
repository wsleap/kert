package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.grpc.ServiceDescriptor

interface BindableService {
  /**
   * Creates [ServerServiceDefinition] object for current instance of service implementation.
   *
   * @return ServerServiceDefinition object.
   */
  fun bindService(): ServerServiceDefinition
}

class ServerServiceMethodsBuilder {
  private val methods = mutableMapOf<String, ServerMethodDefinition<*, *>>()

  fun <ReqT, RespT> addMethod(method: MethodDescriptor<ReqT, RespT>, callHandler: ServerCallHandler<ReqT, RespT>) {
    methods[method.fullMethodName] = ServerMethodDefinition(method, callHandler)
  }

  fun build(): Map<String, ServerMethodDefinition<*, *>> = methods.toMap()
}

/** Definition of a service to be exposed via a Server.  */
class ServerServiceDefinition(
  val serviceDescriptor: ServiceDescriptor,
  build: ServerServiceMethodsBuilder.() -> Unit
) {
  private val methods: Map<String, ServerMethodDefinition<*, *>> = run {
    val builder = ServerServiceMethodsBuilder()
    build(builder)
    builder.build()
  }

  /**
   * Gets all the methods of service.
   */
  fun methods(): Collection<ServerMethodDefinition<*, *>> {
    return methods.values
  }

  /**
   * Look up a method by its fully qualified name.
   *
   * @param methodName the fully qualified name without leading slash. E.g., "com.foo.Foo/Bar"
   */
  fun method(methodName: String): ServerMethodDefinition<*, *> {
    return methods[methodName]!!
  }
}
