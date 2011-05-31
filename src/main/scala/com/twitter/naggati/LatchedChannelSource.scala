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
import com.twitter.concurrent.{ChannelSource, Observer}
import com.twitter.util.Future

/**
 * A `ChannelSource` that buffers all posted messages until there is at least one receiver.
 * As soon as there is at least one receiver, it latches open and never buffers again.
 */
class LatchedChannelSource[A] extends ChannelSource[A] {
  var buffer = new mutable.ListBuffer[A]
  var ready = false

  numObservers.respond {
    case 1 =>
      synchronized {
        if (!ready) {
          buffer.foreach { item => super.send(item) }
          ready = true
        }
      }
      Future.Done
  }

  override def send(a: A): Seq[Future[Observer]] = {
    if (synchronized {
      if (!ready) {
        buffer += a
        false
      } else {
        true
      }
    }) {
      super.send(a)
    } else {
      Seq()
    }
  }
}
