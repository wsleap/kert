package ws.leap.kettle.http

import io.vertx.core.buffer.Buffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

fun httpTestServer(): Server = server(8080) {
  val logger = KotlinLogging.logger {}

  http {
    get("/ping") {
      val data = Buffer.buffer("pong".toByteArray())
      response(data)
    }

    get("/server-stream") {
      val data = flow {
        for(i in 0 until 500) {
          delay(1)
          emit(Buffer.buffer(ByteArray(32 * 1024)))
        }
      }

      response(data)
    }

    post("/client-stream") {
      var total = 0L
      request.body.collect { data ->
        total += data.length()
        delay(1)
        logger.info { "received data, total=$total" }
      }

      response()
    }

    post("/bidi-stream") {
      var total = 0L
      val data = request.body.map { data ->
        total += data.length()
        delay(1)
        logger.info { "received data, total=$total" }
        data
      }

      response(data)
    }
  }
}

fun main(args: Array<String>) = runBlocking {
  val server = httpTestServer()
  server.start()
}

