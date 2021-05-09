package ws.leap.kert.http

import io.kotest.core.config.AbstractProjectConfig

object ProjectConfig : AbstractProjectConfig() {
  override val parallelism = 4
}
