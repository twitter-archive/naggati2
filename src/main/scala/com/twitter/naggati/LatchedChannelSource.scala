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

import com.twitter.concurrent.{Broker, Offer}
import com.twitter.util.{Future, Promise}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.{ArrayBuffer, Queue}
import scala.collection.JavaConversions._

sealed abstract trait Notification
case object Sent extends Notification
case object Done extends Notification

/**
 * LatchedChannelSource buffers all posted messages until there is at least one receiver.
 * As soon as there is at least one receiver, it latches open and never buffers again.
 */
class LatchedChannelSource[A] {
  private[this] val channelBroker = new Broker[Notification]
  private[this] val observersAdded = new AtomicInteger(0)

  private[this] val observers =
    new JConcurrentMapWrapper(new ConcurrentHashMap[Observer[A], Observer[A]])

  protected[naggati] val buffer = new Queue[A]

  private[this] val closesPromise = new Promise[Unit]

  private[this] class Observer[A](listener: A => Future[Unit]) {
    def apply(a: A) = { listener(a) }
  }

  def respond(listener: A => Future[Unit]) {
    val observer = new Observer(listener)
    def loop() {
      val channelOffer = channelBroker.recv {
        case Sent =>
          val item = synchronized { buffer.dequeue }
          val observersCopy = new ArrayBuffer[Observer[A]]
          observers.keys.copyToBuffer(observersCopy)
          observersCopy.foreach { _(item) }
          loop()
        case Done =>
          closesPromise.setValue(())
      }

      channelOffer.sync()
    }

    observers += observer -> observer
    if (observersAdded.incrementAndGet == 1) {
      loop()
    }
  }

  def closes: Future[Unit] = closesPromise

  def close() {
    channelBroker.send(Done).sync()
  }

  def send(a: A): Future[Unit] = {
    synchronized {
      buffer.enqueue(a)
    }
    channelBroker.send(Sent).sync()
  }
}
