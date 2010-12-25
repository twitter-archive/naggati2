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

import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.specs.Specification
import org.specs.mock.JMocker
import Stages._
import test.TestCodec

class CodecSpec extends Specification with JMocker {
  def wrap(s: String) = ChannelBuffers.wrappedBuffer(s.getBytes)

  val encoder: PartialFunction[Any, ChannelBuffer] = {
    case x: String =>
      val buffer = ChannelBuffers.buffer(x.size)
      buffer.writeBytes(x.getBytes("UTF-8"))
      buffer
  }

  "Codec" should {
    "read a fixed number of bytes" in {
      val codec = new TestCodec(readBytes(4) { bytes => emit(new String(bytes)) }, Codec.NONE)

      codec(wrap("12")) mustEqual Nil
      codec(wrap("345")) mustEqual List("1234")
      codec(wrap("6789")) mustEqual List("5678")
      codec(wrap("ABCDEFGHIJKLM")) mustEqual List("9ABC", "DEFG", "HIJK")
      codec(wrap("N")) mustEqual Nil
      codec(wrap("O")) mustEqual List("LMNO")
      codec(wrap("PQRS")) mustEqual List("PQRS")
    }

    "read up to a delimiter, in chunks" in {
      val codec = new TestCodec(readToDelimiter('\n'.toByte) { bytes => emit(new String(bytes)) }, Codec.NONE)

      codec(wrap("partia")) mustEqual Nil
      codec(wrap("l\nand")) mustEqual List("partial\n")
      codec(wrap(" another\nbut the")) mustEqual List("and another\n")
      codec(wrap("n\nmany\nnew ones\nbo")) mustEqual List("but then\n", "many\n", "new ones\n")
      codec(wrap("re")) mustEqual Nil
      codec(wrap("d now\n")) mustEqual List("bored now\n")
      codec(wrap("bye\n")) mustEqual List("bye\n")
    }

    "read a line" in {
      "strip linefeeds" in {
        val codec = new TestCodec(readLine(true, "UTF-8") { line => emit(line) }, Codec.NONE)
        codec(wrap("hello.\n")) mustEqual List("hello.")
        codec(wrap("hello there\r\ncat")) mustEqual List("hello there")
        codec(wrap("s don't use CR\n")) mustEqual List("cats don't use CR")
        codec(wrap("thin")) mustEqual Nil
        codec(wrap("g\r\n\nstop\r\n\r\nokay.\n")) mustEqual List("thing", "", "stop", "", "okay.")
      }

      "keep linefeeds" in {
        val codec = new TestCodec(readLine(false, "UTF-8") { line => emit(line) }, Codec.NONE)
        codec(wrap("hello.\n")) mustEqual List("hello.\n")
        codec(wrap("hello there\r\ncat")) mustEqual List("hello there\r\n")
        codec(wrap("s don't use CR\n")) mustEqual List("cats don't use CR\n")
        codec(wrap("thin")) mustEqual Nil
        codec(wrap("g\r\n\nstop\r\n\r\nokay.\n")) mustEqual List("thing\r\n", "\n", "stop\r\n", "\r\n", "okay.\n")
      }
    }

    "encode" in {
      val codec = new TestCodec(readLine(true, "UTF-8") { line => emit(line) }, encoder)
      codec.send("hello") mustEqual List("hello")
    }
  }
}
