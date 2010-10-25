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

object Stages {
  /**
   * Generate a Stage from a code block.
   */
  final def stage(f: ChannelBuffer => NextStep): Stage = new Stage {
    def apply(buffer: ChannelBuffer) = f(buffer)
  }

  final def proxy(stage: => Stage): Stage = new Stage {
    def apply(buffer: ChannelBuffer) = stage.apply(buffer)
  }

  /**
   * Ensure that a certain number of bytes is buffered before executing the next step, calling
   * `getCount` each time new data arrives, to recompute the total number of bytes desired.
   */
  final def ensureBytesDynamic(getCount: => Int)(process: ChannelBuffer => NextStep) = proxy {
    ensureBytes(getCount)(process)
  }

  /**
   * Ensure that a certain number of bytes is buffered before executing the * next step.
   */
  final def ensureBytes(count: Int)(process: ChannelBuffer => NextStep) = stage { buffer =>
    if (buffer.readableBytes < count) {
      Incomplete
    } else {
      process(buffer)
    }
  }

  /**
   * Read a certain number of bytes into a byte buffer and pass that buffer to the next step in
   * processing. `getCount` is called each time new data arrives, to recompute * the total number of
   * bytes desired.
   */
  final def readBytesDynamic(getCount: => Int)(process: Array[Byte] => NextStep) = proxy {
    readBytes(getCount)(process)
  }

  /**
   * Read `count` bytes into a byte buffer and pass that buffer to the next step in processing.
   */
  final def readBytes(count: Int)(process: Array[Byte] => NextStep) = stage { buffer =>
    if (buffer.readableBytes < count) {
      Incomplete
    } else {
      val bytes = new Array[Byte](count)
      buffer.readBytes(bytes)
      process(bytes)
    }
  }
}
