package ws.leap.kert.http

import io.kotest.core.spec.DoNotParallelize
import io.kotest.matchers.shouldBe
import io.vertx.core.http.*
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.coroutines.await

/**
 * Use Vertx web client to test with different servers
 */
abstract class VertxWebClientSpec : ClientServerSpec() {
  private val client by lazy {
    val options = WebClientOptions()
      .setDefaultPort(port)
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setHttp2ClearTextUpgrade(false)
    WebClient.create(vertx, options)
  }

  init {
    test("ping pong") {
      val future = client.get("/ping")
        .send()
        .onSuccess { resp ->
          val result = resp.body().bytes.toString(Charsets.UTF_8)
          result shouldBe "pong"
        }
      future.await()
    }

    test("client stream") {
      val future = client.post("/client-stream")
        .sendStream(MockReadStream(vertx, streamSize))
        .onSuccess { resp ->
          val result = resp.body().bytes.toString(Charsets.UTF_8)
          result shouldBe "$streamSize"
        }
      future.await()
    }

    test("server stream") {
      val future = client.get("/server-stream")
        .`as`(BodyCodec.pipe(MockWriteStream(streamSize)))
        .send()
        .onSuccess { resp ->
          resp.statusCode() shouldBe 200
        }
      future.await()
    }

    test("bidi stream") {
      val future = client.post("/bidi-stream")
        .`as`(BodyCodec.pipe(MockWriteStream(streamSize)))
        .sendStream(MockReadStream(vertx, streamSize))
        .onSuccess { resp ->
          resp.statusCode() shouldBe 200
        }
      future.await()
    }
  }
}

@DoNotParallelize
class VertxWebClientVertxServerSpec : VertxWebClientSpec() {
  override val server = createVertxServer(vertx, port)
}

@DoNotParallelize
class VertxWebClientVertxVerticleServerSpec : VertxWebClientSpec() {
  override val server = createVertxVerticleServer(vertx, port)
}

@DoNotParallelize
class VertxWebClientKertServerSpec : VertxWebClientSpec() {
  override val server = createKertServer(vertx, port)
}
