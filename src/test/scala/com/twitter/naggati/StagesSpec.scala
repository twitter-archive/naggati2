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

import org.jboss.netty.buffer.ChannelBuffer
import org.specs.Specification
import org.specs.mock.JMocker

class StagesSpec extends Specification with JMocker {
  "Stages" should {
    val buffer = mock[ChannelBuffer]

    "ensureBytes" in {
      "dynamic" in {
        var calls = 0
        val stage = Stages.ensureBytesDynamic({ calls += 1; 4 }) { buffer =>
          Emit("hello")
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
        val stage = Stages.ensureBytes(4) { buffer =>
          Emit("hello")
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
        val stage = Stages.readBytesDynamic({ calls += 1; 4 }) { buffer =>
          Emit("hello")
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
        val stage = Stages.readBytes(4) { buffer =>
          Emit("hello")
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
  }
}
