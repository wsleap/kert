/**
 * Copied from vertx-lang-kotlin-coroutines to fix compiling issue with latest Kotlin.
 */

package ws.leap.kert.http

import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

private const val DEFAULT_CAPACITY = 16

fun <T> ReadStream<T>.toChannel(context: Context): ReceiveChannel<T> {
  this.pause()
  val ret = ChannelReadStream(
    stream = this,
    channel = Channel(0),
    context = context
  )
  ret.subscribe()
  this.fetch(1)
  return ret
}

private class ChannelReadStream<T>(val stream: ReadStream<T>,
                                   val channel: Channel<T>,
                                   context: Context
) : Channel<T> by channel, CoroutineScope {

  override val coroutineContext: CoroutineContext = context.dispatcher()
  fun subscribe() {
    stream.endHandler {
      close()
    }
    stream.exceptionHandler { err ->
      close(err)
    }
    stream.handler { event ->
      launch {
        send(event)
        stream.fetch(1)
      }
    }
  }
}

@ExperimentalCoroutinesApi
fun <T> WriteStream<T>.toChannel(vertx: Vertx, capacity: Int = DEFAULT_CAPACITY): SendChannel<T> {
  return toChannel(vertx.getOrCreateContext(), capacity)
}

/**
 * Adapts the current write stream to Kotlin [SendChannel].
 *
 * The channel can be used to write items, the coroutine is suspended when the stream is full
 * and resumed when the stream is drained.
 *
 * @param context the vertx context
 * @param capacity the channel buffering capacity
 */
@ExperimentalCoroutinesApi
fun <T> WriteStream<T>.toChannel(context: Context, capacity: Int = DEFAULT_CAPACITY): SendChannel<T> {
  val ret = ChannelWriteStream(
    stream = this,
    channel = Channel(capacity),
    context = context
  )
  ret.subscribe()
  return ret
}

private class ChannelWriteStream<T>(val stream: WriteStream<T>,
                                    val channel: Channel<T>,
                                    context: Context
) : Channel<T> by channel, CoroutineScope {
  private var total = 0L
  override val coroutineContext: CoroutineContext = context.dispatcher()

  @ExperimentalCoroutinesApi
  fun subscribe() {
    stream.exceptionHandler {
      channel.close(it)
    }

    launch {
      while (true) {
        val elt = channel.receiveCatching().getOrNull()
        if (stream.writeQueueFull()) {
          logger.trace { "WriteStream.writeQueueFull" }
          stream.drainHandler {
            logger.trace { "WriteStream.drainHandler" }
            if (dispatch(elt)) {
              subscribe()
            }
          }
          break
        } else {
          if (!dispatch(elt)) {
            break
          }
        }
      }
    }
  }

  fun dispatch(elt: T?): Boolean {
    return if (elt != null) {
      total += size(elt)
      logger.trace { "WriteStream.write($total)" }
      stream.write(elt)
      true
    } else {
      close()
      false
    }
  }

  private fun size(elt: T?): Int {
    return when(elt) {
      is Buffer -> elt.length()
      else -> 0
    }
  }

  override fun close(cause: Throwable?): Boolean {
    val ret = channel.close(cause)
    if (ret) stream.end()
    return ret
  }
}
