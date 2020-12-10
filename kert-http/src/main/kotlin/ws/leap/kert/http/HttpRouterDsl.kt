package ws.leap.kert.http

import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.CoroutineExceptionHandler

interface HttpRouterDsl {
  fun filter(filter: HttpServerFilter)

  fun subRouter(path: String, exceptionHandler: CoroutineExceptionHandler? = null, configure: HttpRouterBuilder.() -> Unit)

  fun call(method: HttpMethod, path: String, handler: HttpServerHandler)

  fun get(path: String, handler: HttpServerHandler) {
    call(HttpMethod.GET, path, handler)
  }

  fun head(path: String, handler: HttpServerHandler) {
    call(HttpMethod.HEAD, path, handler)
  }

  fun post(path: String, handler: HttpServerHandler) {
    call(HttpMethod.POST, path, handler)
  }

  fun put(path: String, handler: HttpServerHandler) {
    call(HttpMethod.PUT, path, handler)
  }

  fun delete(path: String, handler: HttpServerHandler) {
    call(HttpMethod.DELETE, path, handler)
  }

  fun patch(path: String, handler: HttpServerHandler) {
    call(HttpMethod.PATCH, path, handler)
  }

  fun options(path: String, handler: HttpServerHandler) {
    call(HttpMethod.OPTIONS, path, handler)
  }
}
