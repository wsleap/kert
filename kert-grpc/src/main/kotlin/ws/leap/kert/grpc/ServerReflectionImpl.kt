package ws.leap.kert.grpc

import com.google.protobuf.Descriptors.*
import grpc.reflection.v1alpha.*
import grpc.reflection.v1alpha.Reflection.*
import io.grpc.Status
import io.grpc.protobuf.ProtoFileDescriptorSupplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.util.*
import kotlin.collections.set

class ServerReflectionImpl(private val registry: ServiceRegistry) : ServerReflectionGrpcKt.ServerReflectionImplBase() {
  private val serverReflectionIndex: ServerReflectionIndex by lazy {
    ServerReflectionIndex(registry.services(), emptyList())

    // TODO handle mutable services (probably don't need it)
//    val serverFileDescriptors: MutableSet<FileDescriptor> = HashSet()
//    val serverServiceNames: MutableSet<String> = HashSet()
//    val serverMutableServices: List<io.grpc.ServerServiceDefinition> = server.getMutableServices()
//    for (mutableService in serverMutableServices) {
//      val serviceDescriptor = mutableService.serviceDescriptor
//      if (serviceDescriptor.schemaDescriptor is ProtoFileDescriptorSupplier) {
//        val serviceName = serviceDescriptor.name
//        val fileDescriptor = (serviceDescriptor.schemaDescriptor as ProtoFileDescriptorSupplier?)
//          .getFileDescriptor()
//        serverFileDescriptors.add(fileDescriptor)
//        serverServiceNames.add(serviceName)
//      }
//    }
//
//    // Replace the index if the underlying mutable services have changed. Check both the file
//    // descriptors and the service names, because one file descriptor can define multiple
//    // services.
//
//    // Replace the index if the underlying mutable services have changed. Check both the file
//    // descriptors and the service names, because one file descriptor can define multiple
//    // services.
//    val mutableServicesIndex: FileDescriptorIndex = index.getMutableServicesIndex()
//    if (mutableServicesIndex.getServiceFileDescriptors() != serverFileDescriptors
//      || mutableServicesIndex.getServiceNames() != serverServiceNames
//    ) {
//      index = ServerReflectionIndex(server.getImmutableServices(), serverMutableServices)
//      serverReflectionIndexes.put(server, index)
//    }
//
//    return index
  }

  override suspend fun serverReflectionInfo(reqs: Flow<ServerReflectionRequest>): Flow<ServerReflectionResponse> {
    return flow {
      reqs.collect { req ->
        val resp = when(req.messageRequestCase) {
          ServerReflectionRequest.MessageRequestCase.FILE_BY_FILENAME -> getFileByName(req)
          ServerReflectionRequest.MessageRequestCase.FILE_CONTAINING_SYMBOL -> getFileContainingSymbol(req)
          ServerReflectionRequest.MessageRequestCase.FILE_CONTAINING_EXTENSION -> getFileContainingExtension(req)
          ServerReflectionRequest.MessageRequestCase.ALL_EXTENSION_NUMBERS_OF_TYPE -> getAllExtensions(req)
          ServerReflectionRequest.MessageRequestCase.LIST_SERVICES -> listServices(req)
          else -> throw Status.UNIMPLEMENTED.withDescription("${req.messageRequestCase} is not implemented").asException()
        }

        emit(resp)
      }
    }
  }

  private fun getFileByName(req: ServerReflectionRequest): ServerReflectionResponse {
    val name = req.fileByFilename
    val fd = serverReflectionIndex.getFileDescriptorByName(name)
      ?: throw Status.NOT_FOUND.withDescription("File $name is not found.").asException()
    return createServerReflectionResponse(req, fd)
  }

  private fun getFileContainingSymbol(req: ServerReflectionRequest): ServerReflectionResponse {
    val symbol = req.fileContainingSymbol
    val fd = serverReflectionIndex.getFileDescriptorBySymbol(symbol)
      ?: throw Status.NOT_FOUND.withDescription("Symbol $symbol is not found.").asException()
    return createServerReflectionResponse(req, fd)
  }

  private fun getFileContainingExtension(req: ServerReflectionRequest): ServerReflectionResponse {
    val extensionRequest = req.fileContainingExtension
    val type = extensionRequest.containingType
    val extension = extensionRequest.extensionNumber
    val fd = serverReflectionIndex.getFileDescriptorByExtensionAndNumber(type, extension)
      ?: throw Status.NOT_FOUND.withDescription("Extension $type/$extension is not found.").asException()
    return createServerReflectionResponse(req, fd)
  }

  private fun getAllExtensions(req: ServerReflectionRequest): ServerReflectionResponse {
    val type = req.allExtensionNumbersOfType
    val extensions = serverReflectionIndex.getExtensionNumbersOfType(type)
      ?: throw Status.NOT_FOUND.withDescription("Type $type is not found.").asException()

    return serverReflectionResponse {
      validHost = req.host
      originalRequest = req
      allExtensionNumbersResponse = extensionNumberResponse {
        baseTypeName = type
        extensionNumber.addAll(extensions)
      }
    }
  }

  private fun listServices(req: ServerReflectionRequest): ServerReflectionResponse {
    return serverReflectionResponse {
      validHost = req.host
      originalRequest = req
      listServicesResponse = listServiceResponse {
        service.addAll(serverReflectionIndex.serviceNames.map { serviceName ->
          serviceResponse {
            name = serviceName
          }
        })
      }
    }
  }

  private fun createServerReflectionResponse(
    request: ServerReflectionRequest, fd: FileDescriptor
  ): ServerReflectionResponse {
    val fdRBuilder: FileDescriptorResponse.Builder = FileDescriptorResponse.newBuilder()
    val seenFiles: MutableSet<String> = HashSet()
    val frontier: Queue<FileDescriptor> = ArrayDeque()
    seenFiles.add(fd.name)
    frontier.add(fd)
    while (!frontier.isEmpty()) {
      val nextFd = frontier.remove()
      fdRBuilder.addFileDescriptorProto(nextFd.toProto().toByteString())
      for (dependencyFd in nextFd.dependencies) {
        if (!seenFiles.contains(dependencyFd.name)) {
          seenFiles.add(dependencyFd.name)
          frontier.add(dependencyFd)
        }
      }
    }
    return ServerReflectionResponse.newBuilder()
      .setValidHost(request.host)
      .setOriginalRequest(request)
      .setFileDescriptorResponse(fdRBuilder)
      .build()
  }


  private class ServerReflectionIndex(
    immutableServices: List<ServerServiceDefinition>,
    mutableServices: List<ServerServiceDefinition>
  ) {
    private val immutableServicesIndex: FileDescriptorIndex
    private val mutableServicesIndex: FileDescriptorIndex

    init {
      immutableServicesIndex = FileDescriptorIndex(immutableServices)
      mutableServicesIndex = FileDescriptorIndex(mutableServices)
    }

    val serviceNames: Set<String>
      get() {
        val immutableServiceNames = immutableServicesIndex.getServiceNames()
        val mutableServiceNames = mutableServicesIndex.getServiceNames()
        val serviceNames: MutableSet<String> = HashSet(immutableServiceNames.size + mutableServiceNames.size)
        serviceNames.addAll(immutableServiceNames)
        serviceNames.addAll(mutableServiceNames)
        return serviceNames
      }

    fun getFileDescriptorByName(name: String): FileDescriptor? {
      var fd: FileDescriptor? = immutableServicesIndex.getFileDescriptorByName(name)
      if (fd == null) {
        fd = mutableServicesIndex.getFileDescriptorByName(name)
      }
      return fd
    }

    fun getFileDescriptorBySymbol(symbol: String): FileDescriptor? {
      var fd: FileDescriptor? = immutableServicesIndex.getFileDescriptorBySymbol(symbol)
      if (fd == null) {
        fd = mutableServicesIndex.getFileDescriptorBySymbol(symbol)
      }
      return fd
    }

    fun getFileDescriptorByExtensionAndNumber(type: String, extension: Int): FileDescriptor? {
      var fd: FileDescriptor? = immutableServicesIndex.getFileDescriptorByExtensionAndNumber(type, extension)
      if (fd == null) {
        fd = mutableServicesIndex.getFileDescriptorByExtensionAndNumber(type, extension)
      }
      return fd
    }

    fun getExtensionNumbersOfType(type: String): Set<Int>? {
      var extensionNumbers = immutableServicesIndex.getExtensionNumbersOfType(type)
      if (extensionNumbers == null) {
        extensionNumbers = mutableServicesIndex.getExtensionNumbersOfType(type)
      }
      return extensionNumbers
    }
  }

  /**
   * Provides a set of methods for answering reflection queries for the file descriptors underlying
   * a set of services. Used by [ServerReflectionIndex] to separately index immutable and
   * mutable services.
   */
  private class FileDescriptorIndex(services: List<ServerServiceDefinition>) {
    private val serviceNames: MutableSet<String> = HashSet()
    private val serviceFileDescriptors: MutableSet<FileDescriptor> = HashSet()
    private val fileDescriptorsByName: MutableMap<String, FileDescriptor> = HashMap()
    private val fileDescriptorsBySymbol: MutableMap<String, FileDescriptor> = HashMap()
    private val fileDescriptorsByExtensionAndNumber: MutableMap<String, MutableMap<Int, FileDescriptor>> = HashMap()

    init {
      val fileDescriptorsToProcess: Queue<FileDescriptor> = ArrayDeque()
      val seenFiles: MutableSet<String> = HashSet()
      for (service in services) {
        val serviceDescriptor = service.serviceDescriptor
        if (serviceDescriptor.schemaDescriptor is ProtoFileDescriptorSupplier) {
          val fileDescriptor = (serviceDescriptor.schemaDescriptor as ProtoFileDescriptorSupplier).fileDescriptor
          val serviceName = serviceDescriptor.name
          require(!serviceNames.contains(serviceName)) { "Service already defined: $serviceName" }
          serviceFileDescriptors.add(fileDescriptor)
          serviceNames.add(serviceName)
          if (!seenFiles.contains(fileDescriptor.name)) {
            seenFiles.add(fileDescriptor.name)
            fileDescriptorsToProcess.add(fileDescriptor)
          }
        }
      }

      while (!fileDescriptorsToProcess.isEmpty()) {
        val currentFd = fileDescriptorsToProcess.remove()
        processFileDescriptor(currentFd)
        for (dependencyFd in currentFd.dependencies) {
          if (!seenFiles.contains(dependencyFd.name)) {
            seenFiles.add(dependencyFd.name)
            fileDescriptorsToProcess.add(dependencyFd)
          }
        }
      }
    }

    /**
     * Returns the file descriptors for the indexed services, but not their dependencies. This is
     * used to check if the server's mutable services have changed.
     */
    private fun getServiceFileDescriptors(): Set<FileDescriptor> {
      return Collections.unmodifiableSet(serviceFileDescriptors)
    }

    fun getServiceNames(): Set<String> {
      return Collections.unmodifiableSet(serviceNames)
    }

    fun getFileDescriptorByName(name: String): FileDescriptor? {
      return fileDescriptorsByName[name]
    }

    fun getFileDescriptorBySymbol(symbol: String): FileDescriptor? {
      return fileDescriptorsBySymbol[symbol]
    }

    fun getFileDescriptorByExtensionAndNumber(type: String, number: Int): FileDescriptor? {
      return if (fileDescriptorsByExtensionAndNumber.containsKey(type)) {
        fileDescriptorsByExtensionAndNumber[type]!![number]
      } else null
    }

    fun getExtensionNumbersOfType(type: String): Set<Int>? {
      return if (fileDescriptorsByExtensionAndNumber.containsKey(type)) {
        Collections.unmodifiableSet(fileDescriptorsByExtensionAndNumber[type]!!.keys)
      } else null
    }

    private fun processFileDescriptor(fd: FileDescriptor) {
      val fdName: String = fd.name
      require(!fileDescriptorsByName.containsKey(fdName)) { "File name already used: $fdName" }
      fileDescriptorsByName[fdName] = fd
      for (service in fd.services) {
        processService(service, fd)
      }
      for (type in fd.messageTypes) {
        processType(type, fd)
      }
      for (extension in fd.extensions) {
        processExtension(extension, fd)
      }
    }

    private fun processService(service: ServiceDescriptor, fd: FileDescriptor) {
      val serviceName: String = service.fullName
      require(!fileDescriptorsBySymbol.containsKey(serviceName)) { "Service already defined: $serviceName" }
      fileDescriptorsBySymbol[serviceName] = fd
      for (method in service.methods) {
        val methodName: String = method.fullName
        require(!fileDescriptorsBySymbol.containsKey(methodName)) { "Method already defined: $methodName" }
        fileDescriptorsBySymbol[methodName] = fd
      }
    }

    private fun processType(type: Descriptor, fd: FileDescriptor) {
      val typeName: String = type.fullName
      require(!fileDescriptorsBySymbol.containsKey(typeName)) { "Type already defined: $typeName" }
      fileDescriptorsBySymbol[typeName] = fd
      for (extension in type.extensions) {
        processExtension(extension, fd)
      }
      for (nestedType in type.nestedTypes) {
        processType(nestedType, fd)
      }
    }

    private fun processExtension(extension: FieldDescriptor, fd: FileDescriptor) {
      val extensionName: String = extension.containingType.fullName
      val extensionNumber: Int = extension.number
      if (!fileDescriptorsByExtensionAndNumber.containsKey(extensionName)) {
        fileDescriptorsByExtensionAndNumber[extensionName] = HashMap<Int, FileDescriptor>()
      }
      require(!fileDescriptorsByExtensionAndNumber[extensionName]!!.containsKey(extensionNumber)) {
        "Extension name and number already defined: $extensionName, $extensionNumber"
      }
      fileDescriptorsByExtensionAndNumber[extensionName]!![extensionNumber] = fd
    }
  }
}
