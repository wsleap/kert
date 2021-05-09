package ws.leap.kert.grpc

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import ws.leap.kert.test.*

class EchoServiceJavaImpl : EchoGrpc.EchoImplBase() {
  override fun unary(request: EchoReq, responseObserver: StreamObserver<EchoResp>) {
    val response = EchoResp.newBuilder()
      .setId(request.id)
      .setValue(request.value)
      .build()
    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  override fun serverStreaming(request: EchoCountReq, responseObserver: StreamObserver<EchoResp>) {
    super.serverStreaming(request, responseObserver)
  }

  override fun clientStreaming(responseObserver: StreamObserver<EchoCountResp>): StreamObserver<EchoReq> {
    return super.clientStreaming(responseObserver)
  }

  override fun bidiStreaming(responseObserver: StreamObserver<EchoResp>): StreamObserver<EchoReq> {
    return super.bidiStreaming(responseObserver)
  }
}

/*
ghz --insecure -c 100 -z 30s --connections 100 \
  --proto kert-grpc/src/test/proto/echo.proto \
  --call ws.leap.kert.test.Echo.unary \
  -d '{"id":1, "value":"hello"}' \
  0.0.0.0:8550

Summary:
  Count:	2923518
  Total:	30.00 s
  Slowest:	29.55 ms
  Fastest:	0.10 ms
  Average:	0.94 ms
  Requests/sec:	97443.68

Response time histogram:
  0.097 [1]	|
  3.043 [969170]	|∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎
  5.988 [26428]	|∎
  8.933 [3386]	|
  11.878 [662]	|
  14.824 [196]	|
  17.769 [115]	|
  20.714 [30]	|
  23.659 [6]	|
  26.605 [3]	|
  29.550 [3]	|

Latency distribution:
  10 % in 0.35 ms
  25 % in 0.48 ms
  50 % in 0.69 ms
  75 % in 1.03 ms
  90 % in 1.66 ms
  95 % in 2.40 ms
  99 % in 4.75 ms

Status code distribution:
  [OK]            2923462 responses
  [Unavailable]   56 responses

Error distribution:
  [56]   rpc error: code = Unavailable desc = transport is closing
 */
fun main() {
  val server = ServerBuilder
    .forPort(8550)
    .addService(EchoServiceJavaImpl())
    .build()

  server.start()
  server.awaitTermination()
}
