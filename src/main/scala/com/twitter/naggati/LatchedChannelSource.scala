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

import java.util.concurrent.CountDownLatch
import scala.collection.mutable
import com.twitter.concurrent.{ChannelSource, Observer}
import com.twitter.util.Future

/**
 * A `ChannelSource` that buffers all posted messages until there is at least one receiver.
 * As soon as there is at least one receiver, it latches open and never buffers again.
 */
class LatchedChannelSource[A] extends ChannelSource[A] {
  protected[naggati] var buffer = new mutable.ListBuffer[A]
  protected[naggati] var ready = false
  protected[naggati] var closed = false

  numObservers.respond {
    case 1 =>
      synchronized {
        if (!ready) {
          buffer.foreach { item => super.send(item) }
          ready = true
          if (closed) {
            super.close()
          }
        }
      }
      Future.Done
  }

  // if the channel isn't ready yet, execute some code inside the lock.
  // return whether the channel was ready: true = channel ready; false = code was executed
  private def checkReady(ifNot: => Unit): Boolean = {
    synchronized {
      if (!ready) ifNot
      ready
    }
  }

  override def close() {
    // don't allow a close() to take effect until after we latch.
    if (checkReady { closed = true }) {
      super.close()
    }
  }

  override def send(a: A): Seq[Future[Observer]] = {
    if (checkReady { buffer += a }) {
      super.send(a)
    } else {
      Seq()
    }
  }
}
