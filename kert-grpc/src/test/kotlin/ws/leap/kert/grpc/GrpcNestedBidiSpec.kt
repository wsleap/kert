package ws.leap.kert.grpc

import io.kotest.core.spec.DoNotParallelize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import ws.leap.kert.test.*
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * A test to demonstrate feed request messages with response messages from server.
 */
@DoNotParallelize
class GrpcNestedBidiSpec : GrpcSpec() {
  private val messageNum = 10
  private val omittedCount = AtomicInteger()

  override fun configureServer(builder: GrpcServerBuilder) {
    val echoService = object: EchoGrpcKt.EchoImplBase() {
      override suspend fun bidiStreaming(req: Flow<EchoReq>): Flow<EchoResp> {
        return flow {
          req.collect { reqMsg ->
            val reqValue = reqMsg.value.toInt()
            // if the value is less than 100, double it then send it back
            // otherwise omit it
            if (reqValue < 100) {
              logger.trace { "Server: Message id=${reqMsg.id} value=${reqMsg.value} bounce" }
              val respValue = (reqValue * 2).toString()
              val respMsg = echoResp { id = reqMsg.id; value = respValue }
              emit(respMsg)
            } else {
              logger.trace { "Server: Message id=${reqMsg.id} value=${reqMsg.value} omitted" }
              omittedCount.incrementAndGet()
              if(omittedCount.get() == messageNum) {
                // all messages are omitted, end the loop
                emit(echoResp {
                  id = -1
                  value = "end"
                })
              }
            }
          }
        }
      }
    }

    builder.service(echoService)
  }

  private val stub = EchoGrpcKt.stub(client)

  init {
    context("Grpc") {
      test("can use response messages as request messages") {
        val channel = Channel<EchoResp>()
        val req = flow {
          for(i in 1 .. messageNum) {
            // send initial 10 messages
            val msg = echoReq { id = i; value = i.toString() }
            emit(msg)
            logger.trace { "Client sent id=${msg.id} value=${msg.value}" }
          }

          // client always bounce the message back to server
          for(resp in channel) {
            logger.trace { "Client: Message id=${resp.id} value=${resp.value} bounce" }
            val msg = echoReq { id = resp.id; value = resp.value }
            emit(msg)
          }
        }

        val resp = stub.bidiStreaming(req)
        var count = 0
        resp.collect { msg ->
          logger.trace { "Client received id=${msg.id}" }
          if(msg.id == -1) {
            // server indicates end, end the loop
            channel.close()
          } else {
            count++
            // put message to channel so it can be sent to server again
            channel.send(msg)
          }
        }
        count shouldBe 50  // all messages received, doesn't count the "end" message
      }
    }
  }
}
