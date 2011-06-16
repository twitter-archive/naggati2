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

import com.twitter.concurrent
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel.Channel
import org.specs.Specification
import org.specs.mock.JMocker
import Stages._
import test.TestCodec

class CodecSpec extends Specification with JMocker {
  def wrap(s: String) = ChannelBuffers.wrappedBuffer(s.getBytes)

  val encoder = new Encoder[String] {
    def encode(x: String) = {
      val buffer = ChannelBuffers.buffer(x.size)
      buffer.writeBytes(x.getBytes("UTF-8"))
      Some(buffer)
    }
  }

  "Codec" should {
    "read a fixed number of bytes" in {
      val (codec, counter) = TestCodec(readBytes(4) { bytes => emit(new String(bytes)) },
        Codec.NONE)

      codec(wrap("12")) mustEqual Nil
      counter.readBytes mustEqual 0
      codec(wrap("345")) mustEqual List("1234")
      counter.readBytes mustEqual 4
      codec(wrap("6789")) mustEqual List("5678")
      counter.readBytes mustEqual 8
      codec(wrap("ABCDEFGHIJKLM")) mustEqual List("9ABC", "DEFG", "HIJK")
      counter.readBytes mustEqual 20
      codec(wrap("N")) mustEqual Nil
      codec(wrap("O")) mustEqual List("LMNO")
      counter.readBytes mustEqual 24
      codec(wrap("PQRS")) mustEqual List("PQRS")
      counter.readBytes mustEqual 28
    }

    "read up to a delimiter, in chunks" in {
      val (codec, counter) = TestCodec(readToDelimiter('\n'.toByte) { bytes => emit(new String(bytes)) }, Codec.NONE)

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
        val (codec, counter) = TestCodec(readLine(true, "UTF-8") { line => emit(line) },
          Codec.NONE)
        codec(wrap("hello.\n")) mustEqual List("hello.")
        codec(wrap("hello there\r\ncat")) mustEqual List("hello there")
        codec(wrap("s don't use CR\n")) mustEqual List("cats don't use CR")
        codec(wrap("thin")) mustEqual Nil
        codec(wrap("g\r\n\nstop\r\n\r\nokay.\n")) mustEqual List("thing", "", "stop", "", "okay.")
      }

      "keep linefeeds" in {
        val (codec, counter) = TestCodec(readLine(false, "UTF-8") { line => emit(line) },
          Codec.NONE)
        codec(wrap("hello.\n")) mustEqual List("hello.\n")
        codec(wrap("hello there\r\ncat")) mustEqual List("hello there\r\n")
        codec(wrap("s don't use CR\n")) mustEqual List("cats don't use CR\n")
        codec(wrap("thin")) mustEqual Nil
        codec(wrap("g\r\n\nstop\r\n\r\nokay.\n")) mustEqual List("thing\r\n", "\n", "stop\r\n", "\r\n", "okay.\n")
      }
    }

    "encode" in {
      "basic" in {
        val (codec, counter) = TestCodec(readLine(true, "UTF-8") { line => emit(line) }, encoder)
        codec.send("hello") mustEqual List("hello")
        counter.writtenBytes mustEqual 5
      }

      "pass-through things it doesn't know" in {
        val encoder = new Encoder[String] {
          def encode(x: String) = {
            val modified = "%%" + x + "%%"
            val buffer = ChannelBuffers.buffer(modified.size)
            buffer.writeBytes(modified.getBytes("UTF-8"))
            Some(buffer)
          }
        }

        val (codec, counter) = TestCodec(readLine(true, "UTF-8") { line => emit(line) }, encoder)
        codec.send("hello") mustEqual List("%%hello%%")
        counter.writtenBytes mustEqual 9
        codec.send(23) mustEqual List("23")
        counter.writtenBytes mustEqual 9
      }
    }
  }
}
