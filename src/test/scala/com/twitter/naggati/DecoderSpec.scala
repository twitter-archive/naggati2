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

class DecoderSpec extends Specification with JMocker {
  def wrap(s: String) = ChannelBuffers.wrappedBuffer(s.getBytes)

  "Decoder" should {
    "read a line" in {
      "strip linefeeds" in {
        val decoder = new TestDecoder(readLine(true, "UTF-8") { line => emit(line) })
        decoder(wrap("hello.\n")) mustEqual List("hello.")
        decoder(wrap("hello there\r\ncat")) mustEqual List("hello there")
        decoder(wrap("s don't use CR\n")) mustEqual List("cats don't use CR")
        decoder(wrap("thin")) mustEqual Nil
        decoder(wrap("g\r\n\nstop\r\n\r\nokay.\n")) mustEqual List("thing", "", "stop", "", "okay.")
      }

      "keep linefeeds" in {
        val decoder = new TestDecoder(readLine(false, "UTF-8") { line => emit(line) })
        decoder(wrap("hello.\n")) mustEqual List("hello.\n")
        decoder(wrap("hello there\r\ncat")) mustEqual List("hello there\r\n")
        decoder(wrap("s don't use CR\n")) mustEqual List("cats don't use CR\n")
        decoder(wrap("thin")) mustEqual Nil
        decoder(wrap("g\r\n\nstop\r\n\r\nokay.\n")) mustEqual List("thing\r\n", "\n", "stop\r\n", "\r\n", "okay.\n")
      }
    }
  }
}
