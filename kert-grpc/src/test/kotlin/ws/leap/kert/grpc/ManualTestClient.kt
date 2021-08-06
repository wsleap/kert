package ws.leap.kert.grpc

import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.runBlocking
import ws.leap.kert.http.httpClient
import ws.leap.kert.test.EchoGrpcKt
import ws.leap.kert.test.echoReq

/**
 * This is for manual test (debugging) to trigger one request.
 */
fun main() {
  val client = httpClient {
    options {
      protocolVersion = HttpVersion.HTTP_2
      defaultPort = 8551
      isHttp2ClearTextUpgrade = false
    }
  }
  val stub = EchoGrpcKt.stub(client)
  runBlocking {
    val req = echoReq { id = 1; value = EchoTest.message }
    val resp = stub.unary(req)
    println(resp)
  }
}
