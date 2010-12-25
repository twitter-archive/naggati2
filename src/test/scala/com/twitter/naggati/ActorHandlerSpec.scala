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
import com.twitter.actors.Actor
import com.twitter.actors.Actor._
import org.jboss.netty.channel._
import org.specs.Specification
import org.specs.mock.JMocker

class ActorHandlerSpec extends Specification with JMocker {
  val messages = new mutable.ListBuffer[Any]()

  var currentActor: Actor = null

  def factory(channel: Channel): Actor = startActor()

  def startActor() = {
    messages.clear()
    currentActor = actor {
      loop {
        receive {
          case "exit" =>
            messages += "exit"
            exit()
          case x =>
            messages += x
        }
      }
    }
    currentActor
  }

  "ActorHandler" should {
    val context = mock[ChannelHandlerContext]
    val channel = mock[Channel]
    val messageEvent = mock[MessageEvent]

    "receive events" in {
      val filter = NettyMessage.defaultFilter - classOf[NettyMessage.MessageSent]
      val handler = new ActorHandler(filter, factory)

      expect {
        one(context).getChannel willReturn channel
        one(context).setAttachment(an[Actor])
      }

      handler.channelOpen(context, null)
      messages.toList mustEqual Nil

      expect {
        one(context).getAttachment() willReturn currentActor
        one(messageEvent).getMessage() willReturn "hello"
      }

      handler.messageReceived(context, messageEvent)
      currentActor ! "exit"
      messages.toList must eventually(be_==(List(NettyMessage.MessageReceived("hello"), "exit")))
    }

    "not receive filtered-out events" in {
      val filter = NettyMessage.defaultFilter - classOf[NettyMessage.MessageSent]
      val handler = new ActorHandler(filter, factory)

      expect {
        one(context).getChannel willReturn channel
        one(context).setAttachment(an[Actor])
        one(context).getAttachment() willReturn currentActor
      }

      handler.channelOpen(context, null)
      handler.writeComplete(context, null)
      currentActor ! "exit"
      messages.toList must eventually(be_==(List("exit")))
    }
  }
}
