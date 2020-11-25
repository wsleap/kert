package ws.leap.kert.http

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.net.URL

class HttpFilterSpec : FunSpec() {
  private val logger = KotlinLogging.logger {}
  private val server = server(8080) {
    http {
      // a filter to track response time
      filter { req, next ->
        val start = System.currentTimeMillis()
        val resp = next(req)
        val time = System.currentTimeMillis() - start
        logger.info { "${req.path} server response time is $time millis" }
        resp
      }

      get("/ping") {
        response("pong")
      }

      router("/sub") {
        // a filter to verify the authentication header must be available
        filter { req, next ->
          if (req.headers["authentication"] == null) throw IllegalArgumentException("Authentication header is missing")
          next(req)
        }

        get("/ping") {
          response("pong")
        }
      }
    }
  }

  private val client = client(URL("http://localhost:8080")) {
    protocolVersion = HttpVersion.HTTP_2

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
      logger.info { "${req.path} client response time is $time millis" }
      resp
    }
  }

  // a client doesn't have authentication header injected
  private val clientNoAuth = client(URL("http://localhost:8080")) {}

  override fun beforeSpec(spec: Spec) = runBlocking {
    server.start()
  }

  override fun afterSpec(spec: Spec) = runBlocking {
    server.stop()
  }

  init {
    context("filter on sub router") {
      test("/sub/ping works with authentication header") {
        val resp = client.get("/sub/ping")
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
