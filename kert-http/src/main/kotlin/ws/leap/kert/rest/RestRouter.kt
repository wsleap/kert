package ws.leap.kert.rest

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.http.HttpMethod
import ws.leap.kert.http.HttpRouterDsl
import ws.leap.kert.http.HttpServerBuilder
import ws.leap.kert.http.response

// this is a draft, not ready to use
// TODO use kotlin serialization instead of jackson for json?
fun HttpServerBuilder.rest(mapper: ObjectMapper, configure: RestRouterConfigurator.() -> Unit) {
  router {
    val configurator = RestRouterConfigurator(mapper, this)
    configure(configurator)
  }
}

class RestRouterConfigurator(val mapper: ObjectMapper, val underlying: HttpRouterDsl) {
  inline fun <reified REQ, RESP> route(method: HttpMethod, path: String, noinline handler: suspend (REQ) -> RESP) {
    underlying.call(method, path) { req ->
      val requestJson = req.body().toString("utf8")
      val req = mapper.readValue<REQ>(requestJson, REQ::class.java)
      val resp = handler(req)
      val responseJson = mapper.writeValueAsBytes(resp)
      response(body = responseJson)
    }
  }

  inline fun <reified REQ, RESP>  get(path: String, noinline handler: suspend (REQ) -> RESP) {
    route(HttpMethod.GET, path, handler)
  }

  inline fun <reified REQ, RESP>  head(path: String, noinline handler: suspend (REQ) -> RESP) {
    route(HttpMethod.HEAD, path, handler)
  }

  inline fun <reified REQ, RESP>  post(path: String, noinline handler: suspend (REQ) -> RESP) {
    route(HttpMethod.POST, path, handler)
  }

  inline fun <reified REQ, RESP>  put(path: String, noinline handler: suspend (REQ) -> RESP) {
    route(HttpMethod.PUT, path, handler)
  }

  inline fun <reified REQ, RESP>  delete(path: String, noinline handler: suspend (REQ) -> RESP) {
    route(HttpMethod.DELETE, path, handler)
  }

  inline fun <reified REQ, RESP>  patch(path: String, noinline handler: suspend (REQ) -> RESP) {
    route(HttpMethod.PATCH, path, handler)
  }

  inline fun <reified REQ, RESP>  options(path: String, noinline handler: suspend (REQ) -> RESP) {
    route(HttpMethod.OPTIONS, path, handler)
  }
}
