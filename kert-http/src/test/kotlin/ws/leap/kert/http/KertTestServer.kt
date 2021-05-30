package ws.leap.kert.http

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private fun createHttpServer(vertx: Vertx, port: Int): HttpServer = httpServer(vertx, port) {
  options {
    isUseAlpn = true
    isSsl = false
  }
  val logger = KotlinLogging.logger {}

  // global filter
  filter { req, next ->
    logger.trace { "request ${req.path} in filter1" }
    val resp = next(req)
    logger.trace { "response for $resp in filter1" }
    resp
  }

  router {
    filter { req, next ->
      logger.trace { "request ${req.path} in filter2" }
      val resp = next(req)
      logger.trace { "response for $resp in filter2" }
      resp
    }

    get("/ping") {
      val data = Buffer.buffer("pong".toByteArray())
      response(body = data)
    }

    get("/server-stream") {
      var total = 0L
      val data = flow {
        for(i in 0 until 500) {
          val buf = Buffer.buffer(ByteArray(8 * 1024))
          total += buf.length()
          emit(buf)
          logger.trace { "sent data, total=$total" }
        }
      }

      response(body = data)
    }

    post("/client-stream") { req ->
      var total = 0L
      req.body.collect { data ->
        total += data.length()
        delay(1)
        logger.trace { "received data, total=$total" }
      }

      response(body = total.toString())
    }

    post("/bidi-stream") { req ->
      var total = 0L
      val data = req.body.map { data ->
        total += data.length()
        delay(1)
        logger.trace { "received data, total=$total" }
        data
      }

      response(body = data)
    }

    subRouter("/sub") {
      filter { req, next ->
        logger.trace { "request ${req.path} in sub filter" }
        val resp = next(req)
        logger.trace { "response for $resp in sub filter" }
        resp
      }

      get("/hello") {
        response(body ="world")
      }
    }
  }
}

fun createKertServer(vertx: Vertx, port: Int): TestServer {
  val server = createHttpServer(vertx, port)
  return object: TestServer {
    override fun start() {
      runBlocking {
        server.start()
      }
    }

    override fun stop() {
      runBlocking {
        server.stop()
      }
    }
  }
}

fun main(args: Array<String>) = runBlocking {
  val server = createHttpServer(Vertx.vertx(), 8000)
  server.start()
}
