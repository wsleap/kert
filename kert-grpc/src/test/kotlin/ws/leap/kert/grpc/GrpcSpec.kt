package ws.leap.kert.grpc

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.runBlocking
import ws.leap.kert.http.httpClient
import ws.leap.kert.http.httpServer

abstract class GrpcSpec : FunSpec() {
  protected val port = 8551
  protected val client = httpClient {
    options {
      protocolVersion = HttpVersion.HTTP_2
      defaultPort = port
      isHttp2ClearTextUpgrade = false
    }
  }

  protected val server = httpServer(port) {
    grpc {
      configureServer(this)
    }
  }
  protected abstract fun configureServer(builder: GrpcServerBuilder)

  override fun beforeSpec(spec: Spec) = runBlocking<Unit> {
    server.start()
  }

  override fun afterSpec(spec: Spec) = runBlocking {
    server.stop()
  }
}
