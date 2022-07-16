package ws.leap.kert.http

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class HttpFilterSpec : FunSpec() {
  private val vertx = Vertx.vertx()
  private val logger = KotlinLogging.logger {}
  private val server = httpServer(vertx,8550) {
    options {
      isSsl = false
    }

    router {
      // a filter to track response time
      filter { req, next ->
        val start = System.currentTimeMillis()
        val resp = next(req)
        val time = System.currentTimeMillis() - start
        logger.trace { "${req.path} server response time is $time millis" }
        resp
      }

      // request handler with it's own filter
      get("/ping", filtered({
        response(body = "pong")
      }, { req, next ->
        logger.trace { "ping with it's own filter" }
        next(req)
      }) )


      subRouter("/sub") {
        // a filter to verify the authentication header must be available
        filter { req, next ->
          if (req.headers["authentication"] == null) throw IllegalArgumentException("Authentication header is missing")
          next(req)
        }

        get("/ping") {
          response(body = "pong")
        }
      }
    }
  }

  private val client = httpClient(vertx) {
    options {
      defaultPort = 8550
      protocolVersion = HttpVersion.HTTP_2
    }

    // a filter to set authentication header in request
    filter { req, next ->
      req.headers["authentication"] = "mocked-authentication"
      next(req)
    }

    // a filter to measure client side response time
    filter { req, next ->
      val start = System.currentTimeMillis()
      val resp = next(req)
      val time = System.currentTimeMillis() - start
      logger.trace { "${req.uri} client response time is $time millis" }
      resp
    }
  }

  // a client doesn't have authentication header injected
  private val clientNoAuth = httpClient(vertx) {
    options {
      defaultPort = 8550
    }
  }

  init {
    beforeSpec {
      server.start()
    }

    afterSpec {
      server.stop()
    }

    context("filter on sub router") {
      test("/sub/ping works with authentication header") {
        val resp = client.get("/sub/ping")
        resp.statusCode shouldBe 200
        resp.body().toString() shouldBe "pong"
      }

      test("/sub/ping works with authentication header from filter") {
        val filteredClient = clientNoAuth.withFilter { req, next ->
          req.headers["authentication"] = "mocked-authentication"
          next(req)
        }
        val resp = filteredClient.get("/sub/ping")
        resp.statusCode shouldBe 200
        resp.body().toString() shouldBe "pong"
      }

      test("/sub/ping fails without authentication header") {
        val resp = clientNoAuth.get("/sub/ping")
        resp.statusCode shouldBe 500
      }
    }

    test("/ping doesn't require authentication header") {
      val resp = clientNoAuth.get("/ping")
      resp.statusCode shouldBe 200
      resp.body().toString() shouldBe "pong"
    }
  }
}
