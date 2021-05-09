package ws.leap.kert.http

import io.kotest.core.spec.DoNotParallelize
import io.kotest.matchers.shouldBe
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.kotlin.coroutines.await
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

/**
 * Use Vertx http client to test with different servers
 */
abstract class VertxClientSpec : ClientServerSpec() {
  private val client by lazy {
    val options = HttpClientOptions()
      .setDefaultPort(port)
      .setProtocolVersion(HttpVersion.HTTP_2)
      // FIXME set it to true cause the error below for bidi stream test
      // https://github.com/netty/netty/issues/7079
      // Max content exceeded 65536 bytes.
      // io.netty.handler.codec.TooLongFrameException: Max content exceeded 65536 bytes.
      //	 at io.vertx.core.http.impl.Http2UpgradedClientConnection$UpgradingStream$2.channelRead(Http2UpgradedClientConnection.java:257)
      .setHttp2ClearTextUpgrade(false)

    vertx.createHttpClient(options)
  }

  init {
    test("ping pong") {
      client.request(HttpMethod.GET, "/ping").flatMap { req ->
        req.end()
        req.response().flatMap { resp ->
          resp.statusCode() shouldBe 200
          resp.body().map { body ->
            body.toString(Charsets.UTF_8) shouldBe "pong"
          }
        }
      }.await()
    }

    // TODO this test fails when running it only with KertServer
    test("client stream") {
      client.request(HttpMethod.POST, "/client-stream").flatMap { req ->
        req.isChunked = true
        val stream = MockReadStream(vertx, streamSize)
        stream.pipe().to(req).flatMap {
          req.end()
          req.response().flatMap { resp ->
            resp.statusCode() shouldBe 200
            resp.body().map { body ->
              body.toString(Charsets.UTF_8) shouldBe "$streamSize"
            }
          }
        }
      }.await()
    }

    test("client stream no backpressure") {
      client.request(HttpMethod.POST, "/client-stream").flatMap { req ->
        req.isChunked = true
        val stream = MockReadStream(vertx, streamSize)
        stream.pipe().to(req).flatMap {
          var sentTotal = 0L
          for(i in 0 until 500) {
            sentTotal += 8 * 1024
            logger.trace { "req.write($sentTotal)" }
            req.write(Buffer.buffer(ByteArray(8 * 1024)))
          }
          req.end()
          req.response().flatMap { resp ->
            resp.statusCode() shouldBe 200
            resp.body().map { body ->
              body.toString(Charsets.UTF_8) shouldBe "$streamSize"
            }
          }
        }
      }.await()
    }

    test("server stream") {
      client.request(HttpMethod.GET, "/server-stream").flatMap { req ->
        req.end()
        req.response().flatMap { resp ->
          resp.pipe().to(MockWriteStream(streamSize))
        }
      }.await()
    }

    test("bidi stream") {
      client.request(HttpMethod.POST, "/bidi-stream").flatMap { req ->
        req.isChunked = true
        val stream = MockReadStream(vertx, streamSize)
        stream.pipe().to(req).map {
          req.end()
        }
        req.response().flatMap { resp ->
          resp.pipe().to(MockWriteStream(streamSize))
        }
      }.await()
    }
  }
}

@DoNotParallelize
class VertxClientVertxServerSpec : VertxClientSpec() {
  override val server = createVertxServer(vertx, port)
}

@DoNotParallelize
class VertxClientVertxVerticleServerSpec : VertxClientSpec() {
  override val server = createVertxVerticleServer(vertx, port)
}

@DoNotParallelize
class VertxClientKertServerSpec : VertxClientSpec() {
  override val server = createKertServer(vertx, port)
}
