package ws.leap.kert.http

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.kotlin.coroutines.await

object Kert {
  internal val vertx by lazy {
    val options = VertxOptions()
      .setEventLoopPoolSize(VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE)
    Vertx.vertx(options)
  }

  suspend fun close() {
    vertx.close().await()
  }
}
