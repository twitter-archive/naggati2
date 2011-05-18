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
import org.jboss.netty.channel.Channel
import org.specs.Specification
import org.specs.mock.JMocker
import test.TestCodec

class MemcacheCodecSpec extends Specification with JMocker {
  def wrap(s: String) = ChannelBuffers.wrappedBuffer(s.getBytes)

  "MemcacheRequest" should {
    "get request" in {
      val (codec, counter) = TestCodec(MemcacheCodec.readAscii, MemcacheCodec.writeAscii)

      codec(wrap("get foo\r\n")) mustEqual List(MemcacheRequest(List("get", "foo"), None, 9))
      codec(wrap("get f")) mustEqual Nil
      codec(wrap("oo\r\n")) mustEqual List(MemcacheRequest(List("get", "foo"), None, 9))

      codec(wrap("g")) mustEqual Nil
      codec(wrap("et foo\r")) mustEqual Nil
      codec(wrap("\nget ")) mustEqual List(MemcacheRequest(List("get", "foo"), None, 9))
      codec(wrap("bar\r\n")) mustEqual List(MemcacheRequest(List("get", "bar"), None, 9))
    }

    "set request" in {
      val (codec, counter) = TestCodec(MemcacheCodec.readAscii, MemcacheCodec.writeAscii)

      codec(wrap("set foo 0 0 5\r\nhello\r\n")).map(_.toString) mustEqual List("<Request: [set foo 0 0 5] data=5 read=22>")
      codec(wrap("set foo 0 0 5\r\n")).map(_.toString) mustEqual Nil
      codec(wrap("hello\r\n")).map(_.toString) mustEqual List("<Request: [set foo 0 0 5] data=5 read=22>")
      codec(wrap("set foo 0 0 5")).map(_.toString) mustEqual Nil
      codec(wrap("\r\nhell")).map(_.toString) mustEqual Nil
      codec(wrap("o\r\n")).map(_.toString) mustEqual List("<Request: [set foo 0 0 5] data=5 read=22>")
    }

    "quit request" in {
      val (codec, counter) = TestCodec(MemcacheCodec.readAscii, MemcacheCodec.writeAscii)
      codec(wrap("QUIT\r\n")) mustEqual List(MemcacheRequest(List("quit"), None, 6))
    }
  }

  "MemcacheResponse" should {
    val channel = mock[Channel]
    val capturedChannel = capturingParam[ChannelBuffer]

    def unpackBuffer(buffer: ChannelBuffer) = {
      val bytes = new Array[Byte](buffer.readableBytes)
      buffer.readBytes(bytes)
      new String(bytes, "ISO-8859-1")
    }

    "write response" in {
      val (codec, counter) = TestCodec(MemcacheCodec.readAscii, MemcacheCodec.writeAscii)

      codec.send(new MemcacheResponse("CLIENT_ERROR foo")) mustEqual List("CLIENT_ERROR foo\r\n")
    }

    "write data response" in {
      val (codec, counter) = TestCodec(MemcacheCodec.readAscii, MemcacheCodec.writeAscii)

      codec.send(new MemcacheResponse("VALUE foo 0 5", Some("hello".getBytes))) mustEqual
        List("VALUE foo 0 5\r\nhello\r\nEND\r\n")
    }

    "write repsonse, then disconnect" in {
      val (codec, counter) = TestCodec(MemcacheCodec.readAscii, MemcacheCodec.writeAscii)

      codec.send(new MemcacheResponse("CLIENT_ERROR foo") then Codec.Disconnect) mustEqual
        List("CLIENT_ERROR foo\r\n", "<CLOSE>")
    }
  }
}
