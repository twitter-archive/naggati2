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

import scala.actors.Actor
import scala.collection.immutable
import org.jboss.netty.channel._
import org.jboss.netty.handler.timeout.{IdleState, IdleStateAwareChannelUpstreamHandler, IdleStateEvent}

/**
 * All messages sent to an actor by ActorHandler will be a subclass of NettyMessage.
 */
abstract sealed class NettyMessage
object NettyMessage {
  case class ChannelConnected() extends NettyMessage
  case class MessageReceived(message: AnyRef) extends NettyMessage
  case class MessageSent() extends NettyMessage
  case class ExceptionCaught(cause: Throwable) extends NettyMessage
  case class ChannelIdle(status: IdleState) extends NettyMessage
  case class ChannelDisconnected() extends NettyMessage

  type Filter = immutable.Set[Class[_ <: NettyMessage]]

  def classOfObject(obj: NettyMessage) = obj.getClass.asInstanceOf[Class[NettyMessage]]

  val defaultFilter: Filter = immutable.Set(
    classOf[NettyMessage.ChannelConnected],
    classOf[NettyMessage.MessageReceived],
    classOf[NettyMessage.MessageSent],
    classOf[NettyMessage.ExceptionCaught],
    classOf[NettyMessage.ChannelIdle],
    classOf[NettyMessage.ChannelDisconnected])
}

/**
 * Converts netty ChannelEvents into messages to be sent to an actor.
 */
@ChannelHandler.Sharable
class ActorHandler(filter: NettyMessage.Filter)(actorFactory: => Actor)
      extends IdleStateAwareChannelUpstreamHandler {
  // not currently handled:
  // channelBound, channelUnbound, channelInterestChanged, childChannelOpen, childChannelClosed,
  //   channelClosed

  private final def actor(context: ChannelHandlerContext) = context.getAttachment.asInstanceOf[Actor]

  final def send(actor: Actor, message: NettyMessage) {
    if (filter contains NettyMessage.classOfObject(message)) {
      actor ! message
    }
  }

  final def send(context: ChannelHandlerContext, message: NettyMessage) {
    send(actor(context), message)
  }

  override final def channelOpen(context: ChannelHandlerContext, event: ChannelStateEvent) {
    val actor = actorFactory
    context.setAttachment(actor)
  }

  override final def channelConnected(context: ChannelHandlerContext, event: ChannelStateEvent) {
    send(context, NettyMessage.ChannelConnected())
  }

  override final def messageReceived(context: ChannelHandlerContext, event: MessageEvent) {
    send(context, NettyMessage.MessageReceived(event.getMessage))
  }

  override final def writeComplete(context: ChannelHandlerContext, event: WriteCompletionEvent) {
    send(context, NettyMessage.MessageSent())
  }

  override final def exceptionCaught(context: ChannelHandlerContext, event: ExceptionEvent) {
    send(context, NettyMessage.ExceptionCaught(event.getCause))
  }

  override final def channelIdle(context: ChannelHandlerContext, event: IdleStateEvent) {
    send(context, NettyMessage.ChannelIdle(event.getState))
  }

  override final def channelDisconnected(context: ChannelHandlerContext, event: ChannelStateEvent) {
    send(context, NettyMessage.ChannelDisconnected())
  }
}
