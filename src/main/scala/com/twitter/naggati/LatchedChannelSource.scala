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

import com.twitter.concurrent.{Broker, Offer, Serialized}
import com.twitter.util.{Future, Promise}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.JavaConversions._

/**
 * LatchedChannelSource buffers all posted messages until there is at least one receiver.
 * As soon as there is at least one receiver, it latches open and never buffers again.
 */
class LatchedChannelSource[A] extends Serialized {
  @volatile private[this] var open = true
  private[this] val channelBroker = new Broker[A]
  private[this] val closeBroker = new Broker[Unit]
  private[this] val observersAdded = new AtomicInteger(0)

  private[this] val observers =
    new JConcurrentMapWrapper(new ConcurrentHashMap[ConcreteObserver[A], ConcreteObserver[A]])

  protected[naggati] var buffer = new ListBuffer[A]
  @volatile protected[naggati] var ready = false
  @volatile protected[naggati] var closed = false

  /**
   * An object representing the lifecycle of subscribing to a LatchedChannelSource.
   * This object can be used to unsubscribe.
   */
  trait Observer[A] {
    /**
     * Indicates that the Observer is no longer interested in receiving
     * messages.
     */
    def dispose()
  }

  private[this] class ConcreteObserver[A](listener: A => Future[Unit]) extends Observer[A] {
    def apply(a: A) = { listener(a) }
    def dispose() {
      LatchedChannelSource.this.serialized {
        observers.remove(this)
      }
    }
  }

  private[this] val _closes =  new Promise[Unit]
  val closes: Future[Unit] = _closes

  def isOpen = open

  def respond(listener: A => Future[Unit]): Observer[A] = {
    val observer = new ConcreteObserver(listener)
    def loop() {
      val closeOffer = closeBroker.recv

      val channelOffer = channelBroker.recv { a =>
        val observersCopy = new ArrayBuffer[ConcreteObserver[A]]
        observers.keys.copyToBuffer(observersCopy)
        observersCopy.foreach { _(a) }
        loop()
      }

      // sequence to ensure channel gets priority over close
      val offer = channelOffer orElse {
        Offer.choose(channelOffer, closeOffer)
      }
      offer.sync()
    }

    serialized {
      if (open) {
        observers += observer -> observer
        if (observersAdded.incrementAndGet == 1) {
          // first observer -- deliver the buffer and handle delayed close
          deliverBuffer()
          if (!closed) loop()
        }
      }
    }

    observer
  }

  // if the channel isn't ready yet, execute some code inside the lock.
  // return whether the channel was ready: true = channel ready; false = code was executed
  private def checkReady(ifNot: => Unit): Boolean = {
    synchronized {
      if (!ready) ifNot
      ready
    }
  }

  private[this] def closeInternal() {
    serialized {
      if (open) {
        open = false
        _closes.setValue(())
        observers.clear()
      }
    }
  }

  def close() {
    // don't allow a close() to take effect until after we latch.
    if (checkReady { closed = true }) {
      closeInternal()
      closeBroker.send(()).sync()
    }
  }

  def send(a: A): Seq[Future[Unit]] = {
    if (checkReady { buffer += a }) {
      Seq(channelBroker.send(a).sync())
    } else {
      Seq()
    }
  }

  private[this] def deliverBuffer() {
    synchronized {
      if (!ready) {
        buffer.foreach { item => observers.keys.foreach { _(item) } }
        buffer.clear()
        ready = true
        if (closed) closeInternal()
      }
    }
  }
}
