package ws.leap.kert.http

import io.kotest.core.spec.DoNotParallelize
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.vertx.core.Vertx

interface TestServer {
  fun start()
  fun stop()
}

/**
 * Use different http clients (Vertx http client, web client, Kert http client)
 * to test with different servers (Vertx http server, http server in verticle, Kert http server)
 * with 4 different request patterns (unary, server streaming, client streaming, bidirectional streaming)
 * for cross reference
 * to make sure there is no compatibility issue and easier to identify bugs.
 */
@DoNotParallelize
abstract class ClientServerSpec : FunSpec() {
  protected val vertx = Vertx.vertx()
  protected open val port: Int = 8500
  protected abstract val server: TestServer

  // TODO enable this when all problems fixed
  override fun testCaseOrder() = TestCaseOrder.Random

  override fun beforeSpec(spec: Spec) {
    server.start()
  }

  override fun afterSpec(spec: Spec) {
    server.stop()
    vertx.close()
  }
}
