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

class StagesSpec extends Specification with JMocker {
  "Stages" should {
    val buffer = mock[ChannelBuffer]

    "ensureBytes" in {
      "dynamic" in {
        var calls = 0
        val stage = ensureBytesDynamic({ calls += 1; 4 }) { buffer =>
          emit("hello")
        }

        expect {
          one(buffer).readableBytes willReturn 2
        }
        stage(buffer) mustEqual Incomplete
        calls mustEqual 1

        expect {
          one(buffer).readableBytes willReturn 4
        }
        stage(buffer) mustEqual Emit("hello")
        calls mustEqual 2
      }

      "static" in {
        val stage = ensureBytes(4) { buffer =>
          emit("hello")
        }

        expect {
          one(buffer).readableBytes willReturn 2
        }
        stage(buffer) mustEqual Incomplete

        expect {
          one(buffer).readableBytes willReturn 4
        }
        stage(buffer) mustEqual Emit("hello")
      }
    }

    "readBytes" in {
      "dynamic" in {
        var calls = 0
        val stage = readBytesDynamic({ calls += 1; 4 }) { buffer =>
          emit("hello")
        }

        expect {
          one(buffer).readableBytes willReturn 2
        }
        stage(buffer) mustEqual Incomplete
        calls mustEqual 1

        expect {
          one(buffer).readableBytes willReturn 4
          one(buffer).readBytes(a[Array[Byte]])
        }
        stage(buffer) mustEqual Emit("hello")
        calls mustEqual 2
      }

      "static" in {
        val stage = readBytes(4) { buffer =>
          emit("hello")
        }

        expect {
          one(buffer).readableBytes willReturn 2
          one(buffer).readBytes(a[Array[Byte]])
        }
        stage(buffer) mustEqual Incomplete

        expect {
          one(buffer).readableBytes willReturn 4
        }
        stage(buffer) mustEqual Emit("hello")
      }
    }

    "ensureDelimiter" in {
      "dynamic" in {
        var calls = 0
        val stage = ensureDelimiterDynamic({ calls += 1; 32.toByte }) { (n, bytes) =>
          emit("hello" + n)
        }

        expect {
          one(buffer).bytesBefore(32.toByte) willReturn -1
        }
        stage(buffer) mustEqual Incomplete
        calls mustEqual 1

        expect {
          one(buffer).bytesBefore(32.toByte) willReturn 3
        }
        stage(buffer) mustEqual Emit("hello4")
        calls mustEqual 2
      }

      "static" in {
        val stage = ensureDelimiter(32.toByte) { (n, bytes) =>
          emit("hello" + n)
        }

        expect {
          one(buffer).bytesBefore(32.toByte) willReturn -1
        }
        stage(buffer) mustEqual Incomplete

        expect {
          one(buffer).bytesBefore(32.toByte) willReturn 3
        }
        stage(buffer) mustEqual Emit("hello4")
      }
    }

    "ensureMultiByteDelimiter" in {
      def wrap(s: String) = ChannelBuffers.wrappedBuffer(s.getBytes)

      "dynamic" in {
        var calls = 0
        val delimiter = Array[Byte]('\r', '\n')
        val stage = ensureMultiByteDelimiterDynamic({ calls += 1; delimiter }) { (n, bytes) =>
          emit("hello" + n)
        }

        stage(wrap("hello there")) mustEqual Incomplete
        calls mustEqual 1

        stage(wrap("hello there\r\ncat")) mustEqual Emit("hello13")
        calls mustEqual 2
      }

      "static" in {
        val delimiter = Array[Byte]('\r', '\n')
        val stage = ensureMultiByteDelimiter(delimiter) { (n, bytes) =>
          emit("hello" + n)
        }

        stage(wrap("hello there")) mustEqual Incomplete

        stage(wrap("hello there\r\ncat")) mustEqual Emit("hello13")
      }
    }

    "readLine" in {
      def wrap(s: String) = ChannelBuffers.wrappedBuffer(s.getBytes)

      val stage = readLine(true, "UTF-8") { line => emit(line) }

      stage(wrap("hello there\r\ncat")) mustEqual Emit("hello there")
      stage(wrap("cats don't use CR\n")) mustEqual Emit("cats don't use CR")
      stage(wrap("okay.")) mustEqual Incomplete
    }
  }
}
