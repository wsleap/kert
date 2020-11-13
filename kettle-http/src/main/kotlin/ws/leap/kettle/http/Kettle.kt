package ws.leap.kettle.http

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions

object Kettle {
  internal val vertx by lazy {
    val options = VertxOptions().setEventLoopPoolSize(VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE)
    Vertx.vertx(options)
  }
}
