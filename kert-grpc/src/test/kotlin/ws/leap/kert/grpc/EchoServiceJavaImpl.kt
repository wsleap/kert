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
  Count:	2652162
  Total:	30.00 s
  Slowest:	37.28 ms
  Fastest:	0.09 ms
  Average:	1.04 ms
  Requests/sec:	88399.48

Response time histogram:
  0.094 [1]	|
  3.812 [972068]	|∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎
  7.530 [23734]	|∎
  11.248 [3054]	|
  14.966 [723]	|
  18.684 [235]	|
  22.403 [94]	|
  26.121 [44]	|
  29.839 [13]	|
  33.557 [4]	|
  37.275 [30]	|

Latency distribution:
  10 % in 0.36 ms
  25 % in 0.50 ms
  50 % in 0.73 ms
  75 % in 1.15 ms
  90 % in 1.99 ms
  95 % in 2.90 ms
  99 % in 5.74 ms

Status code distribution:
  [OK]            2652118 responses
  [Unavailable]   42 responses
  [Canceled]      2 responses
 */
fun main() {
  val server = ServerBuilder
    .forPort(8550)
    .addService(EchoServiceJavaImpl())
    .build()

  server.start()
  server.awaitTermination()
}
