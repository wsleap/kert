package ws.leap.kert.http

import io.kotest.core.spec.DoNotParallelize
import io.kotest.matchers.shouldBe
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use Kert http client to test with different servers
 */
abstract class KertClientSpec : ClientServerSpec() {
  private val client by lazy {
    httpClient(vertx) {
      options {
        protocolVersion = HttpVersion.HTTP_2
        defaultPort = port
        isHttp2ClearTextUpgrade = false
      }
    }
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
        delay(1)
        total += data.length()
      }
      println("received data $total bytes")
    }

    // TODO this test fails when running it only with KertServer
    // but it doesn't fail when running with other tests in the spec
    // or running with VertxServer or VertxVerticleServer
    // so looks like it's a problem with KertServer to serve first request
    test("client stream") {
      val body = flow {
        for (i in 0 until 500) {
          // delay(10)
          emit(Buffer.buffer(ByteArray(8 * 1024)))
        }
      }
      val resp = client.post("/client-stream", body)
      resp.statusCode shouldBe 200
      resp.body().toString(Charsets.UTF_8) shouldBe "${500 * 8 * 1024}"
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
        logger.trace { "received data, total=$total" }
      }
    }
  }
}

@DoNotParallelize
class KertClientVertxServerSpec : KertClientSpec() {
  override val server = createVertxServer(vertx, port)
}

@DoNotParallelize
class KertClientVertxVerticleServerSpec : KertClientSpec() {
  override val server = createVertxVerticleServer(vertx, port)
}

@DoNotParallelize
class KertClientKertServerSpec : KertClientSpec() {
  override val server = createKertServer(vertx, port)
}
