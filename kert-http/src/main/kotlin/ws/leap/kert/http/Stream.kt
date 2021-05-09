package ws.leap.kert.http

import io.vertx.core.Context
import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun <T : Any> ReadStream<T>.asFlow(context: Context): Flow<T> {
  // return toChannel(context).consumeAsFlow()
  return toFlow(context)
}

/**
 * Another implementation of ReadStream to Flow, without launching a new coroutine for each send.
 * Throughput and latency seems slightly better (0.5%)
 * Causes io.vertx.core.VertxException: Connection was closed for GrpcBasicSpec "bidi stream"
 * Sending too fast at beginning because the buffered channel?
 */
fun <T : Any> ReadStream<T>.toFlow(context: Context): Flow<T> {
  pause()

  val channel = Channel<T>(Channel.BUFFERED)
  handler { msg ->
    val result = channel.trySend(msg)
    if(!result.isSuccess) {
      throw IllegalStateException("Element $msg was not added to channel, result=$result")
    }
  }
  endHandler {
    channel.close(null)
  }
  exceptionHandler { exception ->
    channel.close(exception)
  }
  return flow {
    fetch(1)

    while(true) {
      val result = channel.receiveCatching()
      when {
        result.isClosed -> {
          break
        }
        else -> {
          val message = result.getOrThrow()
          emit(message)
          fetch(1)
        }
      }
    }
  }
}

suspend fun write(context: Context, body: Flow<Buffer>, stream: WriteStream<Buffer>) {
  body.collect { data ->
    logger.trace { "Sending to channel length=${data.length()}" }
    // TODO not efficient since waiting for message to be sent
    stream.write(data).await()
  }

//  val channel = stream.toChannel(context, Channel.RENDEZVOUS)
//  body.collect { data ->
//    logger.trace { "Sending to channel length=${data.length()}" }
//    channel.send(data)
//  }
//  channel.close()
}
