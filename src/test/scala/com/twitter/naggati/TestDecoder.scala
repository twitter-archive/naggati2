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

import scala.collection.mutable
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel._

/**
 * Netty doesn't appear to have a good set of fake objects yet, so this wraps a Decoder in a fake
 * environment that collects emitted objects and returns them.
 */
class TestDecoder(decoder: Decoder) {
  def this(firstStage: Stage) = this(new Decoder(firstStage))

  val output = new mutable.ListBuffer[AnyRef]

  val fin = new SimpleChannelUpstreamHandler() {
    override def messageReceived(c: ChannelHandlerContext, e: MessageEvent) {
      output += e.getMessage
    }
  }
  val pipeline = Channels.pipeline()
  pipeline.addLast("decoder", decoder)
  pipeline.addLast("fin", fin)

  val context = pipeline.getContext(decoder)
  val sink = new AbstractChannelSink() {
    def eventSunk(pipeline: ChannelPipeline, event: ChannelEvent) { }
  }
  val channel = new AbstractChannel(null, null, pipeline, sink) {
    def getRemoteAddress() = null
    def getLocalAddress() = null
    def isConnected() = true
    def isBound() = true
    def getConfig() = new DefaultChannelConfig()
  }

  def apply(buffer: ChannelBuffer) = {
    output.clear()
    decoder.messageReceived(context, new UpstreamMessageEvent(pipeline.getChannel, buffer, null))
    output.toList
  }
}
