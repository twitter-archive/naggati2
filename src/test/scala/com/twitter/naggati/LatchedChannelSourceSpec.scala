/*
 * Copyright 2011 Twitter, Inc.
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
import com.twitter.util.{Future, FuturePool}
import org.specs.Specification

class LatchedChannelSourceSpec extends Specification {
  "LatchedChannelSource" should {
    "buffer when there are no receivers" in {
      val channel = new LatchedChannelSource[String]
      channel.send("hello")
      channel.ready mustEqual false
      channel.buffer.size mustEqual 1
      channel.send("goodbye")
      channel.ready mustEqual false
      channel.buffer.size mustEqual 2
    }

    "send pent-up messages when a receiver is added" in {
      val channel = new LatchedChannelSource[String]
      channel.send("hello")
      channel.send("kitty")
      channel.ready mustEqual false
      channel.buffer.size mustEqual 2

      var received = new mutable.ListBuffer[String]
      channel.respond { s =>
        received += s
        Future.Done
      }
      received.toList mustEqual List("hello", "kitty")
      channel.ready mustEqual true
    }

    "not buffer after the channel is latched" in {
      val channel = new LatchedChannelSource[String]
      channel.send("hello")
      channel.ready mustEqual false
      channel.buffer.size mustEqual 1

      var received = new mutable.ListBuffer[String]
      channel.respond { s =>
        received += s
        Future.Done
      }
      received.toList mustEqual List("hello")
      channel.ready mustEqual true

      channel.send("kitty")
      received.toList mustEqual List("hello", "kitty")
      channel.ready mustEqual true
    }

    "not actually close until the channel is latched" in {
      val channel = new LatchedChannelSource[String]
      channel.send("hello")
      channel.close()
      channel.ready mustEqual false
      channel.buffer.size mustEqual 1
      channel.isOpen mustEqual true

      var received = new mutable.ListBuffer[String]
      channel.respond { s =>
        received += s
        Future.Done
      }
      received.toList mustEqual List("hello")
      channel.isOpen mustEqual false
    }

    "keep items in order after latching" in {
      val channel = new LatchedChannelSource[String]
      var received = new mutable.ListBuffer[String]
      val expected = List("a", "b", "c", "d", "e", "f", "g", "h", "i")

      def add(items: List[String]) {
        items match {
          case head :: tail =>
            channel.send(head)
            add(tail)
          case Nil =>
        }
      }

      channel.respond { s =>
        received += s
        Thread.sleep(100)
        Future.Done
      }

      add(expected)

      channel.close()
      channel.closes()
      received.toList mustEqual expected
    }
  }
}
