package ws.leap.kert.http

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.impl.future.FailedFuture
import io.vertx.core.impl.future.SucceededFuture
import io.vertx.core.streams.WriteStream
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class MockWriteStream(private val size: Int) : WriteStream<Buffer> {
  private var writtenTotal = 0
  override fun exceptionHandler(handler: Handler<Throwable>?): WriteStream<Buffer> {
    return this
  }

  override fun write(data: Buffer): Future<Void> {
    writtenTotal += data.length()
    logger.trace { "Write data total=${writtenTotal}" }
    return SucceededFuture(null)
  }

  override fun write(data: Buffer, handler: Handler<AsyncResult<Void>>) {
    writtenTotal += data.length()
    logger.trace { "Write data total=${writtenTotal}" }
    handler.handle(SucceededFuture(null))
  }

  override fun end(handler: Handler<AsyncResult<Void>>) {
    logger.trace { "end" }
    if(writtenTotal == size) {
      handler.handle(SucceededFuture(null))
    } else {
      handler.handle(FailedFuture("Expected size $size, actual $writtenTotal"))
    }
  }

  override fun setWriteQueueMaxSize(maxSize: Int): WriteStream<Buffer> {
    return this
  }

  override fun writeQueueFull(): Boolean {
    return false
  }

  override fun drainHandler(handler: Handler<Void>?): WriteStream<Buffer> {
    return this
  }
}
