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
import com.twitter.stats.Stats

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
}

/**
 * A netty ChannelHandler for decoding data into protocol objects on the way in, and packing
 * objects into byte arrays on the way out. Optionally, the bytes in/out are tracked.
 */
class Codec(firstStage: Stage, encoder: PartialFunction[Any, ChannelBuffer],
            bytesReadCounter: String, bytesWrittenCounter: String)
extends FrameDecoder with ChannelDownstreamHandler {
  def this(firstStage: Stage, encoder: PartialFunction[Any, ChannelBuffer]) =
    this(firstStage, encoder, "bytes_read", "bytes_written")

  private var stage = firstStage

  private def buffer(context: ChannelHandlerContext) = {
    ChannelBuffers.dynamicBuffer(context.getChannel().getConfig().getBufferFactory())
  }

  // turn an Encodable message into a Buffer.
  override final def handleDownstream(context: ChannelHandlerContext, event: ChannelEvent) {
    event match {
      case message: DownstreamMessageEvent =>
        val obj = message.getMessage()
        if (encoder.isDefinedAt(obj)) {
          Channels.write(context, message.getFuture, encoder(obj), message.getRemoteAddress)
        } else {
          context.sendDownstream(event)
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
    Stats.incr(bytesReadCounter, readableBytes - buffer.readableBytes())
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
