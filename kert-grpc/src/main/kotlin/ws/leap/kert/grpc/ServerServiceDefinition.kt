package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.grpc.ServiceDescriptor
import ws.leap.kert.core.combine
import java.lang.IllegalArgumentException

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

  fun <REQ, RESP> addMethod(method: MethodDescriptor<REQ, RESP>, handler: GrpcHandler<REQ, RESP>) {
    methods[method.fullMethodName] = ServerMethodDefinition(method, handler)
  }

  fun build(): Map<String, ServerMethodDefinition<*, *>> = methods.toMap()
}

/** Definition of a service to be exposed via a Server.  */
class ServerServiceDefinition(
  val serviceDescriptor: ServiceDescriptor,
  private val methods: Map<String, ServerMethodDefinition<*, *>>
) {
  constructor(serviceDescriptor: ServiceDescriptor, configure: ServerServiceMethodsBuilder.() -> Unit) :
    this(serviceDescriptor, kotlin.run {
      val builder = ServerServiceMethodsBuilder()
      configure(builder)
      builder.build()
    })

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
    return methods[methodName] ?: throw IllegalArgumentException("Method $methodName is not found")
  }

  fun intercepted(interceptor: GrpcInterceptor): ServerServiceDefinition {
    val interceptedMethods = methods.mapValues { it.value.intercepted(interceptor) }
    return ServerServiceDefinition(serviceDescriptor, interceptedMethods)
  }

  fun intercepted(vararg interceptors: GrpcInterceptor): ServerServiceDefinition {
    if (interceptors.isEmpty()) return this

    val combinedInterceptor = combine(*interceptors)!!
    return intercepted(combinedInterceptor)
  }
}
