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
Summary:
  Count:	951316
  Total:	10.00 s
  Slowest:	45.72 ms
  Fastest:	0.10 ms
  Average:	0.97 ms
  Requests/sec:	95121.19

Response time histogram:
  0.099 [1]	|
  4.661 [938965]	|∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎
  9.222 [11054]	|
  13.784 [992]	|
  18.346 [195]	|
  22.907 [28]	|
  27.469 [12]	|
  32.030 [6]	|
  36.592 [15]	|
  41.153 [11]	|
  45.715 [7]	|

Latency distribution:
  10 % in 0.35 ms
  25 % in 0.49 ms
  50 % in 0.70 ms
  75 % in 1.06 ms
  90 % in 1.75 ms
  95 % in 2.57 ms
  99 % in 5.11 ms
 */
fun main() {
  val server = ServerBuilder
    .forPort(8887)
    .addService(EchoServiceJavaImpl())
    .build()

  server.start()
  server.awaitTermination()
}
