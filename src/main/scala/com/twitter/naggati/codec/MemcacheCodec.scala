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
import Stages._

case class MemcacheRequest(line: List[String], data: Option[Array[Byte]], bytesRead: Int) {
  override def toString = {
    "<Request: " + line.mkString("[", " ", "]") + (data match {
      case None => ""
      case Some(x) => " data=" + x.size
    }) + " read=" + bytesRead + ">"
  }
}

case class MemcacheResponse(line: String, data: Option[Array[Byte]]) {
  def this(line: String) = this(line, None)
  def this(line: String, data: Array[Byte]) = this(line, Some(data))

  override def toString = {
    "<Repsonse: " + line + (data match {
      case None => ""
      case Some(x) => " data=" + x.size
    }) + ">"
  }

  val lineData = line.getBytes("ISO-8859-1")

  def writeAscii() = {
    val size = lineData.size + MemcacheCodec.CRLF.size +
      (if (data.isDefined) (data.get.size + MemcacheCodec.END.size) else 0)
    val buffer = ChannelBuffers.buffer(size)
    buffer.writeBytes(lineData)
    buffer.writeBytes(MemcacheCodec.CRLF)
    data.foreach { x =>
      buffer.writeBytes(x)
      buffer.writeBytes(MemcacheCodec.END)
    }
    buffer
  }
}

object MemcacheCodec {
  val STORAGE_COMMANDS = List("set", "add", "replace", "append", "prepend", "cas")
  val END = "\r\nEND\r\n".getBytes
  val CRLF = "\r\n".getBytes

  def asciiCodec(bytesReadCounter: String, bytesWrittenCounter: String) =
    new Codec(readAscii, writeAscii, bytesReadCounter, bytesWrittenCounter)

  def asciiCodec() = new Codec(readAscii, writeAscii)

  val readAscii = readLine(true, "ISO-8859-1") { line =>
    val segments = line.split(" ")
    segments(0) = segments(0).toLowerCase

    val command = segments(0)
    if (STORAGE_COMMANDS contains command) {
      if (segments.length < 5) {
        throw new ProtocolError("Malformed request line")
      }
      val dataBytes = segments(4).toInt
      ensureBytes(dataBytes + 2) { buffer =>
        // final 2 bytes are just "\r\n" mandated by protocol.
        val bytes = new Array[Byte](dataBytes)
        buffer.readBytes(bytes)
        buffer.skipBytes(2)
        emit(MemcacheRequest(segments.toList, Some(bytes), line.length + dataBytes + 4))
      }
    } else {
      emit(MemcacheRequest(segments.toList, None, line.length + 2))
    }
  }

  val writeAscii: PartialFunction[Any, ChannelBuffer] = {
    case response: MemcacheResponse =>
      response.writeAscii()
  }
}
