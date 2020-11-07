package ws.leap.kettle.grpc

import io.grpc.MethodDescriptor
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import kotlinx.coroutines.flow.*
import java.io.ByteArrayInputStream

object GrpcUtils {
  suspend fun <T> writeMessages(stream: WriteStream<Buffer>, messages: Flow<T>, serializer: (T) -> ByteBuf) {
    messages.collect { msg ->
      val buf = serializer(msg)

//      stream.writeByte(0)  // compressed flag
//      stream.writeInt(buf.readableBytes())  // message size
//      stream.writeFully(ByteBufUtil.getBytes(buf))  // message bytes
    }
  }

  suspend fun <T> readMessages(stream: Flow<Buffer>, deserializer: (ByteBuf, Int) -> T): Flow<T> {
    // accumulate bytes in the buffer
    val accuBuffer = Unpooled.buffer()

    return flow {
      stream.collect { data ->
        accuBuffer.discardReadBytes()
        accuBuffer.writeBytes(data.byteBuf)

        while(true) {
          val msg = readMessage(accuBuffer, deserializer) ?: break
          emit(msg)
        }
      }
    }
  }

  private fun <T> readMessage(buf: ByteBuf, deserializer: (ByteBuf, Int) -> T): T? {
    if(buf.readableBytes() <= Constants.messageHeaderSize) return null

    val slice = buf.slice()  // create a slice so read won't change reader position
    val compressedFlag = slice.readUnsignedByte()
    val messageSize = slice.readUnsignedInt()

    // there is no complete message in buffer, return null
    if (slice.readableBytes() < messageSize) return null

    // move the reader index to consume the GRPC message header
    buf.readerIndex(buf.readerIndex() + Constants.messageHeaderSize)

    // TODO message compression is not supported yet
    if (compressedFlag == 1.toShort()) throw UnsupportedOperationException("Compression is not supported yet")

    return deserializer(buf, messageSize.toInt())
  }

  fun <M> serializeMessagePacket(message: M, serializer: (M) -> ByteBuf): ByteBuf {
    val buf = Unpooled.buffer()
    val messageBytes = serializer(message)
    buf.writeByte(0)
    buf.writeInt(messageBytes.readableBytes())
    buf.writeBytes(messageBytes)
    return buf
  }

  fun <ReqT> requestSerializer(method: MethodDescriptor<ReqT, *>): (ReqT) -> ByteBuf {
    return { msg: ReqT ->
      val msgStream = method.streamRequest(msg)
      val buf = Unpooled.buffer()
      buf.writeBytes(msgStream, Constants.maxMessageSize) // TODO how to get the actual stream size
      buf
    }
  }

  fun <RespT> responseSerializer(method: MethodDescriptor<*, RespT>): (RespT) -> ByteBuf {
    return { msg: RespT ->
      val msgStream = method.streamResponse(msg)
      val buf = Unpooled.buffer()
      buf.writeBytes(msgStream, Constants.maxMessageSize) // TODO how to get the actual stream size
      buf
    }
  }

  fun <ReqT> requestDeserializer(method: MethodDescriptor<ReqT, *>): (ByteBuf, Int) -> ReqT {
    return { buf: ByteBuf, size: Int ->
      val messageBuf = ByteArray(size)
      buf.readBytes(messageBuf)
      val inStream = ByteArrayInputStream(messageBuf)

      method.parseRequest(inStream)
    }
  }

  fun <RespT> responseDeserializer(method: MethodDescriptor<*, RespT>): (ByteBuf, Int) -> RespT {
    return { buf: ByteBuf, size: Int ->
      val messageBuf = ByteArray(size)
      buf.readBytes(messageBuf)
      val inStream = ByteArrayInputStream(messageBuf)

      method.parseResponse(inStream)
    }
  }
}
