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

class HttpRequestSpec extends Specification {
  def wrap(s: String) = ChannelBuffers.wrappedBuffer(s.getBytes)

  val TEST1 = """POST /query.cgi HTTP/1.1
Host: www.example.com
Content-Length: 6
X-Special-Request: cheese
 and ham

hello!"""

  "HttpRequest" should {
    "parse a simple http request" in {
      val decoder = new TestCodec(HttpRequest.codec)
      val written = decoder(wrap(TEST1)).asInstanceOf[List[HttpRequest]]

      written.size mustEqual 1
      written(0).request mustEqual RequestLine("POST", "/query.cgi", "HTTP/1.1")
      written(0).headers.size mustEqual 3
      written(0).headers(0) mustEqual HeaderLine("host", "www.example.com")
      written(0).headers(1) mustEqual HeaderLine("content-length", "6")
      written(0).headers(2) mustEqual HeaderLine("x-special-request", "cheese and ham")
      new String(written(0).body) mustEqual "hello!"
    }
  }
}
