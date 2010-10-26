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

import Stages._

case class MemcacheRequest(line: List[String], data: Option[Array[Byte]]) {
  override def toString = {
    "<Request: " + line.mkString("[", " ", "]") + (data match {
      case None => ""
      case Some(x) => ": " + x.size + " bytes"
    }) + ">"
  }
}

object MemcacheRequest {
  private val STORAGE_COMMANDS = List("set", "add", "replace", "append", "prepend", "cas")

  def asciiDecoder = new Decoder(readAscii)

  val readAscii = readLine(true, "ISO-8859-1") { line =>
    // KestrelStats.bytesRead.incr(line.length + 1)

    val segments = line.split(" ")
    segments(0) = segments(0).toLowerCase

    val command = segments(0)
    if (STORAGE_COMMANDS contains command) {
      if (segments.length < 5) {
        throw new ProtocolError("Malformed request line")
      }
      val dataBytes = segments(4).toInt
      ensureBytes(dataBytes + 2) { buffer =>
        // KestrelStats.bytesRead.incr(dataBytes + 2)
        // final 2 bytes are just "\r\n" mandated by protocol.
        val bytes = new Array[Byte](dataBytes)
        buffer.readBytes(bytes)
        buffer.skipBytes(2)
        emit(MemcacheRequest(segments.toList, Some(bytes)))
      }
    } else {
      emit(MemcacheRequest(segments.toList, None))
    }
  }
}
