package ws.leap.kettle.http

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.vertx.core.Vertx
import io.vertx.core.http.*
import io.vertx.core.streams.Pump
import io.vertx.ext.web.Router

fun createServer(vertx: Vertx): HttpServer {
  val options = HttpServerOptions()
    .setSsl(false)
    .setUseAlpn(true)
    .setMaxChunkSize(16 * 1024)
    .setInitialSettings(Http2Settings().setMaxFrameSize(16 * 1024))
  val server = vertx.createHttpServer(options)

  val router = Router.router(vertx)

  router.post("/bidi-stream").handler { ctx ->
    ctx.response().setChunked(true)
    ctx.request().endHandler {
      ctx.response().end()
    }
    val pump = Pump.pump(ctx.request(), ctx.response())
    pump.start()
  }

  router.post("/client-stream").handler { ctx ->
    val request = ctx.request()
    var total = 0L

    request.exceptionHandler { e ->
      println(e)
      ctx.response().end("wrong")
    }

    request.endHandler {
      println("received total $total bytes")
      ctx.response().end("OK")
    }

    request.handler { buf ->
      println("received ${buf.length()} bytes")
      total += buf.length()
    }
  }

  router.get("/server-stream").handler { ctx ->
    ctx.response().setChunked(true)
    ctx.request().endHandler {
      ctx.response().end()
    }
    val pump = Pump.pump(ctx.request(), ctx.response())
    pump.start()
  }

  server.requestHandler(router)
  return server
}

fun main() {
  val vertx = Vertx.vertx()
  val server = createServer(vertx)
  server.listen(8888)
}

// test bidi stream with vertx raw http client & server api
class VertxSpec : FunSpec() {
  private val vertx = Vertx.vertx()
  private val server = createServer(vertx)

  private val client = run {
    val options = HttpClientOptions()
      .setDefaultHost("localhost")
      .setDefaultPort(8888)
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setMaxChunkSize(16 * 1024)
      .setInitialSettings(Http2Settings().setMaxFrameSize(16 * 1024))
      .setSsl(false)
      .setHttp2ClearTextUpgrade(true)

    vertx.createHttpClient(options)
  }

  override fun beforeSpec(spec: Spec) {
    server.listen(8888)
    Thread.sleep(500)
  }

  init {
    test("bidi stream") {
      client.request(HttpMethod.POST, "/bidi-stream") { ar ->
        if(ar.succeeded()) {
          val request = ar.result()
          //Pump.pump( )
        }
      }
    }
  }
}
