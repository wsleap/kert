package ws.leap.kert.http

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.vertx.core.buffer.Buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.net.URL

class HttpServerClientSpec : FunSpec() {
  private val logger = KotlinLogging.logger {}
  private val server = httpTestServer()
  private val client = client(URL("http://localhost:8550"))

  override fun beforeSpec(spec: Spec) = runBlocking<Unit> {
    server.start()

    // TODO client-stream and bidi-stream will fail (if run the case only)
    // but it works after a request to WARM UP the client/connection!!??
    // this only happens for HTTP2
    client.get("/ping")
  }

  override fun afterSpec(spec: Spec) = runBlocking<Unit> {
    server.stop()
  }

  init {
    test("ping pong") {
      val resp = client.get("/ping")
      resp.statusCode shouldBe 200
      resp.body().toString() shouldBe "pong"
    }

    test("server stream") {
      val resp = client.get("/server-stream")
      var total = 0L
      resp.body.collect { data ->
        total += data.length()
      }
      println("received data $total bytes")
    }

    test("client stream") {
      val body = flow {
        for (i in 0 until 500) {
          emit(Buffer.buffer(ByteArray(8 * 1024)))
        }
      }
      val resp = client.post("/client-stream", body)
      resp.statusCode shouldBe 200
    }

    test("bidi stream") {
      val body = flow {
        for (i in 0 until 500) {
          emit(Buffer.buffer(ByteArray(8 * 1024)))
        }
      }

      val resp = client.post("/bidi-stream", body)

      var total = 0L
      resp.body.collect { data ->
        total += data.length()
        logger.info { "received data, total=$total" }
      }
    }
  }
}
