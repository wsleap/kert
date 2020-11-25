package ws.leap.kert.grpc

class ServiceRegistry {
  private val services = mutableMapOf<String, ServerServiceDefinition>()
  private val methods = mutableMapOf<String, ServerMethodDefinition<*, *>>()

  fun addService(service: ServerServiceDefinition) {
    services[service.serviceDescriptor.name] = service
    for(method in service.methods()) {
      methods[method.methodDescriptor.fullMethodName] = method
    }
  }

  fun addService(service: BindableService) {
    addService(service.bindService())
  }

  fun services(): List<ServerServiceDefinition> = services.values.toList()
  fun lookupMethod(methodName: String): ServerMethodDefinition<*, *>? = methods[methodName]
}
