/*
 * Copyright 2010 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.naggati

import scala.annotation.tailrec
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.frame.FrameDecoder

/*
 * Convenience exception class to allow decoders to indicate a protocol error.
 */
class ProtocolError(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}

object Codec {
  val NONE: PartialFunction[Any, ChannelBuffer] = {
    case null => null
  }

  sealed abstract class Flag
  case object Disconnect extends Flag

  /**
   * Mixin for outbound (write-side) codec objects to allow them to be used for signalling
   * out-of-bound messages to the codec engine.
   *
   * Primarily this is used to signal that the connection should be closed after writing the
   * object. For example, if `Response` is a case class for writing a response, and `Signalling`
   * is mixed in, you can use:
   *
   *     channel.write(new Response(...) then Codec.Disconnect)
   *
   * to signal that the connection should be closed after writing the response.
   */
  trait Signalling {
    private var flags: List[Flag] = Nil

    /**
     * Add a signal flag to this outbound message.
     */
    def then(flag: Flag): this.type = {
      flags = flag :: flags
      this
    }

    def signals = flags
  }
}

object DontCareCounter extends (Int => Unit) {
  def apply(x: Int) { }
}

/**
 * A netty ChannelHandler for decoding data into protocol objects on the way in, and packing
 * objects into byte arrays on the way out. Optionally, the bytes in/out are tracked.
 */
class Codec(firstStage: Stage, encoder: PartialFunction[Any, ChannelBuffer],
            bytesReadCounter: Int => Unit, bytesWrittenCounter: Int => Unit)
extends FrameDecoder with ChannelDownstreamHandler {
  def this(firstStage: Stage, encoder: PartialFunction[Any, ChannelBuffer]) =
    this(firstStage, encoder, DontCareCounter, DontCareCounter)

  private var stage = firstStage

  private def buffer(context: ChannelHandlerContext) = {
    ChannelBuffers.dynamicBuffer(context.getChannel.getConfig.getBufferFactory)
  }

  // turn an Encodable message into a Buffer.
  override final def handleDownstream(context: ChannelHandlerContext, event: ChannelEvent) {
    event match {
      case message: DownstreamMessageEvent =>
        val obj = message.getMessage
        if (encoder.isDefinedAt(obj)) {
          val buffer = encoder(obj)
          bytesWrittenCounter(buffer.readableBytes)
          Channels.write(context, message.getFuture, buffer, message.getRemoteAddress)
        } else {
          context.sendDownstream(event)
        }
        if (obj.isInstanceOf[Codec.Signalling]) {
          obj.asInstanceOf[Codec.Signalling].signals.foreach { signal =>
            signal match {
              case Codec.Disconnect => context.getChannel.close()
            }
          }
        }
      case _ =>
        context.sendDownstream(event)
    }
  }

  @tailrec
  override final def decode(context: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer) = {
    val readableBytes = buffer.readableBytes()
    val nextStep = try {
      stage(buffer)
    } catch {
      case e: Throwable =>
        // reset state before throwing.
        stage = firstStage
        throw e
    }
    bytesReadCounter(readableBytes - buffer.readableBytes())
    nextStep match {
      case Incomplete =>
        null
      case GoToStage(s) =>
        stage = s
        decode(context, channel, buffer)
      case Emit(obj) =>
        stage = firstStage
        obj
    }
  }
}
