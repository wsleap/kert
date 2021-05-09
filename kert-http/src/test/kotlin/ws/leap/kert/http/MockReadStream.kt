package ws.leap.kert.http

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.ReadStream
import mu.KotlinLogging
import kotlin.math.min

private val logger = KotlinLogging.logger {}

class MockReadStream(vertx: Vertx, size: Int) : ReadStream<Buffer> {
  private val timer = vertx.periodicStream(1)
  private var remaining = size
  private val chunkSize = 8 * 1024
  private var endHandler: Handler<Void>? = null
  private var readTotal = 0

  override fun exceptionHandler(handler: Handler<Throwable>?): ReadStream<Buffer> {
    timer.exceptionHandler(handler)
    return this
  }

  override fun handler(handler: Handler<Buffer>?): ReadStream<Buffer> {
    if(handler != null) {
      timer.handler {
        val size = min(remaining, chunkSize)
        val bytes = ByteArray(size)
        remaining -= size
        readTotal += size
        try {
          logger.trace { "read data total=${readTotal}, remaining=${remaining}" }
          handler.handle(Buffer.buffer(bytes))
        } catch (t: Throwable) {
          logger.error(t) { "Error when handling data" }
        }

        if(remaining == 0) {
          timer.cancel()
          logger.trace("end")
          endHandler?.handle(null)
        }
      }
    } else {
      timer.handler(null)
    }
    return this
  }

  override fun pause(): ReadStream<Buffer> {
    logger.trace("pause")
    timer.pause()
    return this
  }

  override fun resume(): ReadStream<Buffer> {
    logger.trace("resume")
    timer.resume()
    return this
  }

  override fun fetch(amount: Long): ReadStream<Buffer> {
    logger.trace("fetch amount=${amount}")
    timer.fetch(amount)
    return this
  }

  override fun endHandler(endHandler: Handler<Void>?): ReadStream<Buffer> {
    this.endHandler = endHandler
    return this
  }
}
