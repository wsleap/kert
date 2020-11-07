package ws.leap.kettle.http

import io.vertx.core.Vertx

object Kettle {
  internal val vertx by lazy { Vertx.vertx() }
}
