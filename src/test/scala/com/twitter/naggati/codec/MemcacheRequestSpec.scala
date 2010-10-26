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
package codec

import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.specs.Specification

class MemcacheRequestSpec extends Specification {
  def wrap(s: String) = ChannelBuffers.wrappedBuffer(s.getBytes)

  "MemcacheRequest" should {
    "get request" in {
      val decoder = new TestDecoder(MemcacheRequest.asciiDecoder)

      decoder(wrap("get foo\r\n")) mustEqual List(MemcacheRequest(List("get", "foo"), None))
      decoder(wrap("get f")) mustEqual Nil
      decoder(wrap("oo\r\n")) mustEqual List(MemcacheRequest(List("get", "foo"), None))

      decoder(wrap("g")) mustEqual Nil
      decoder(wrap("et foo\r")) mustEqual Nil
      decoder(wrap("\nget ")) mustEqual List(MemcacheRequest(List("get", "foo"), None))
      decoder(wrap("bar\r\n")) mustEqual List(MemcacheRequest(List("get", "bar"), None))
    }

    "set request" in {
      val decoder = new TestDecoder(MemcacheRequest.asciiDecoder)

      decoder(wrap("set foo 0 0 5\r\nhello\r\n")).map(_.toString) mustEqual List("<Request: [set foo 0 0 5]: 5 bytes>")
      decoder(wrap("set foo 0 0 5\r\n")).map(_.toString) mustEqual Nil
      decoder(wrap("hello\r\n")).map(_.toString) mustEqual List("<Request: [set foo 0 0 5]: 5 bytes>")
      decoder(wrap("set foo 0 0 5")).map(_.toString) mustEqual Nil
      decoder(wrap("\r\nhell")).map(_.toString) mustEqual Nil
      decoder(wrap("o\r\n")).map(_.toString) mustEqual List("<Request: [set foo 0 0 5]: 5 bytes>")
    }

    "quit request" in {
      val decoder = new TestDecoder(MemcacheRequest.asciiDecoder)
      decoder(wrap("QUIT\r\n")) mustEqual List(MemcacheRequest(List("quit"), None))
    }
  }
}
