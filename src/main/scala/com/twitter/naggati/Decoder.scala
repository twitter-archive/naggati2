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
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{Channel, ChannelHandler, ChannelHandlerContext}
import org.jboss.netty.handler.codec.frame.FrameDecoder

/*
 * Convenience exception class to allow decoders to indicate a protocol error.
 */
class ProtocolError(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}

/**
 * A netty ChannelHandler for decoding data into protocol objects.
 */
class Decoder(firstStage: Stage) extends FrameDecoder {
  private var stage = firstStage

  var bytesRead: Long = 0L

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
    bytesRead += readableBytes - buffer.readableBytes()
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
