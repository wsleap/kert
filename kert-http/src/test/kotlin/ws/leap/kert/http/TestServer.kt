package ws.leap.kert.http

import io.vertx.core.buffer.Buffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

fun httpTestServer(): HttpServer = httpServer(8550) {
  val logger = KotlinLogging.logger {}

  // global filter
  filter { req, next ->
    logger.info { "request ${req.path} in filter1" }
    val resp = next(req)
    logger.info { "response for ${resp} in filter1" }
    resp
  }

  router {
    filter { req, next ->
      logger.info { "request ${req.path} in filter2" }
      val resp = next(req)
      logger.info { "response for ${resp} in filter2" }
      resp
    }

    get("/ping") {
      val data = Buffer.buffer("pong".toByteArray())
      response(body = data)
    }

    get("/server-stream") {
      val data = flow {
        for(i in 0 until 500) {
          delay(1)
          emit(Buffer.buffer(ByteArray(32 * 1024)))
        }
      }

      response(body = data)
    }

    post("/client-stream") { req ->
      var total = 0L
      req.body.collect { data ->
        total += data.length()
        delay(1)
        logger.info { "received data, total=$total" }
      }

      response()
    }

    post("/bidi-stream") { req ->
      var total = 0L
      val data = req.body.map { data ->
        total += data.length()
        delay(1)
        logger.info { "received data, total=$total" }
        data
      }

      response(body = data)
    }

    subRouter("/sub") {
      filter { req, next ->
        logger.info { "request ${req.path} in sub filter" }
        val resp = next(req)
        logger.info { "response for ${resp} in sub filter" }
        resp
      }

      get("/hello") {
        response(body ="world")
      }
    }
  }
}

fun main(args: Array<String>) = runBlocking {
  val server = httpTestServer()
  server.start()
}

