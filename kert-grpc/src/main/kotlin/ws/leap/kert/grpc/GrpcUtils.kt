package ws.leap.kert.grpc

import com.google.protobuf.AbstractMessage
import io.grpc.MethodDescriptor
import io.netty.buffer.*
import io.vertx.core.MultiMap
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
    if(buf.readableBytes() < Constants.messageHeaderSize) return null

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

  fun <M> serializeMessagePacket(message: M): ByteBuf {
    require(message is AbstractMessage)
    val buf = Unpooled.buffer()
    buf.writeByte(0)
    buf.writeInt(message.serializedSize)
    serialize(message, buf)
    return buf
  }

  private fun serialize(message: AbstractMessage, buf: ByteBuf) {
    val out = ByteBufOutputStream(buf)
    message.writeTo(out)
  }

  fun <ReqT> requestSerializer(method: MethodDescriptor<ReqT, *>): (ReqT) -> ByteBuf {
    return { msg: ReqT ->
      val msgStream = method.streamRequest(msg)
      val buf = Unpooled.buffer()
      buf.writeBytes(msgStream, 1024) // TODO how to get the actual stream size
      buf
    }
  }

  fun <RespT> responseSerializer(method: MethodDescriptor<*, RespT>): (RespT) -> ByteBuf {
    return { msg: RespT ->
      val msgStream = method.streamResponse(msg)
      val buf = Unpooled.buffer()
      // TODO bad performance
      buf.writeBytes(msgStream, 1024) // TODO how to get the actual stream size
      buf
    }
  }

  fun <ReqT> requestDeserializer(method: MethodDescriptor<ReqT, *>): (ByteBuf, Int) -> ReqT {
    return { buf: ByteBuf, size: Int ->
      val inStream = ByteBufInputStream(buf, size)
      method.parseRequest(inStream)
    }
  }

  fun <RespT> responseDeserializer(method: MethodDescriptor<*, RespT>): (ByteBuf, Int) -> RespT {
    return { buf: ByteBuf, size: Int ->
      val inStream = ByteBufInputStream(buf, size)
      method.parseResponse(inStream)
    }
  }

  fun buildInterceptorChain(interceptors: List<GrpcInterceptor>): GrpcInterceptor? {
    var interceptorChain: GrpcInterceptor? = null
    interceptors.map { interceptor ->
      interceptorChain = interceptorChain?.let { current ->
        { req, next ->
          current(req) { interceptor(it, next) }
        }
      } ?: interceptor
    }
    return interceptorChain
  }
}

fun emptyMetadata(): MultiMap = MultiMap.caseInsensitiveMultiMap()
