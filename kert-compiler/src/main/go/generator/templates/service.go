package templates

const (
	Service = `
{{- with $s := .}}
package {{$s.JavaPackage}}

import java.net.URL
import kotlinx.coroutines.flow.Flow
import io.grpc.MethodDescriptor.generateFullMethodName
import ws.leap.kert.core.combine
import ws.leap.kert.grpc.ClientCalls
import ws.leap.kert.grpc.ServerCalls
import ws.leap.kert.http.HttpClient
import ws.leap.kert.grpc.CallOptions
import ws.leap.kert.grpc.AbstractStub
import ws.leap.kert.grpc.BindableService
import ws.leap.kert.grpc.ServerServiceDefinition
import ws.leap.kert.grpc.GrpcInterceptor
import ws.leap.kert.grpc.GrpcUtils

{{/**
 * <pre>
 * Test service that supports all call types.
 * </pre>
 */}}
@javax.annotation.Generated(
  value = ["by gRPC proto compiler (version 0.5.0)"],
  comments = "Source: {{.ProtoFile}}")
object {{$s.Name}}GrpcKt {
  const val SERVICE_NAME = "{{$s.ProtoName}}"

  // Static method descriptors that strictly reflect the proto.
  {{- range $i, $m := .Methods}}
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  val {{$m.FieldName}}: io.grpc.MethodDescriptor<{{$m.InputType}}, {{$m.OutputType}}> =
    io.grpc.MethodDescriptor.newBuilder<{{$m.InputType}}, {{$m.OutputType}}>()
      .setType(io.grpc.MethodDescriptor.MethodType.{{$m.GrpcMethodType}})
      .setFullMethodName(generateFullMethodName("{{$s.ProtoName}}", "{{$m.Name}}"))
      .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller({{$m.InputType}}.getDefaultInstance()))
      .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller({{$m.OutputType}}.getDefaultInstance()))
      .build()
  {{- end}}

  /**
   * Creates a new stub with Client
   */
  fun stub(client: HttpClient, callOptions: CallOptions = CallOptions(), interceptors: List<GrpcInterceptor> = emptyList()): {{.Name}}Stub {
    val combinedInterceptor = combine(*interceptors.toTypedArray())
    return {{.Name}}Stub(client, callOptions, combinedInterceptor)
  }

  /**
   * Creates a new stub
   */
  fun stub(address: URL, callOptions: CallOptions = CallOptions(), interceptors: List<GrpcInterceptor> = emptyList()): {{.Name}}Stub {
    val client = HttpClient.create(address)
    return stub(client, callOptions, interceptors)
  }

  {{/**
   * <pre>
   * Test service that supports all call types.
   * </pre>
   */}}
  interface {{$s.Name}} {
    {{range $i, $m := .Methods}}
    {{- /**
     * <pre>
     * One requestMore followed by one response.
     * The server returns the client payload as-is.
     * </pre>
     */ -}}
    suspend fun {{$m.JavaName}}(req: {{$m.FullInputType}}): {{$m.FullOutputType}}
    {{end}}
  }

  {{/**
   * <pre>
   * Test service that supports all call types.
   * </pre>
   */}}
  abstract class {{.Name}}ImplBase : BindableService, {{$s.Name}} {
    {{range $i, $m := .Methods}}
    {{- /**
     * <pre>
     * One requestMore followed by one response.
     * The server returns the client payload as-is.
     * </pre>
     */ -}}
    override suspend fun {{$m.JavaName}}(req: {{$m.FullInputType}}): {{$m.FullOutputType}} {
      return ServerCalls.{{$m.UnimplementedCall}}({{$m.FieldName}})
    }
    {{end}}
    override fun bindService(): ServerServiceDefinition {
      return ServerServiceDefinition(serviceDescriptor) {
        {{- range $i, $m := .Methods}}
        addMethod({{$m.FieldName}}, ServerCalls.{{$m.Call}}(::{{$m.JavaName}}))
        {{- end}}
      }
    }
  }

  {{/**
   * <pre>
   * Test service that supports all call types.
   * </pre>
   */}}
  class {{$s.Name}}Stub internal constructor(client: HttpClient, callOptions: CallOptions, interceptors: GrpcInterceptor?)
    : AbstractStub<{{$s.Name}}Stub>(client, callOptions, interceptors), {{$s.Name}} {
    {{range $i, $m := .Methods}}
    {{- /**
     * <pre>
     * One requestMore followed by one response.
     * The server returns the client payload as-is.
     * </pre>
     */ -}}
    override suspend fun {{$m.JavaName}}(req: {{$m.FullInputType}}): {{$m.FullOutputType}} {
      return ClientCalls.{{$m.Call}}(
        newCall({{$m.FieldName}}, callOptions), {{$m.CallParams}})
    }
    {{end}}

    override fun build(client: HttpClient, callOptions: CallOptions, interceptors: GrpcInterceptor?): {{$s.Name}}Stub {
      return {{.Name}}Stub(client, callOptions, interceptors)
    }
  }

  private class {{$s.Name}}DescriptorSupplier : io.grpc.protobuf.ProtoFileDescriptorSupplier {
    override fun getFileDescriptor(): com.google.protobuf.Descriptors.FileDescriptor {
      return {{$s.JavaPackage}}.{{$s.OuterClassName}}.getDescriptor()
    }
  }

  val serviceDescriptor: io.grpc.ServiceDescriptor by lazy {
    io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
      .setSchemaDescriptor({{$s.Name}}DescriptorSupplier())
      {{- range $i, $m := .Methods}}
      .addMethod({{$m.FieldName}})
      {{- end}}
      .build()
  }
}
{{- end}}
`
)
