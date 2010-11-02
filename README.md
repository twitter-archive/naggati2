
# Naggati 2.0

Naggati (Inuktitut for "make fit") is a protocol builder for netty, using
scala 2.8.

## What's netty?

Netty is a high-performance java library for doing asynchronous event-driven
I/O. You can check out their website here: <http://www.jboss.org/netty>

Netty works by creating one or more Selectors that poll a set of active
sockets. When a socket receives data, netty sends the received bytes through a
processing pipeline. Usually the pipeline decodes these bytes into a "message"
for whatever protocol you're speaking -- for example, in HTTP, the pipeline
might decode the received bytes into an `HTTPRequest` object that contains the
method, path, HTTP version, and headers. The last receiver on the pipeline is
your session object, which processes the message and possibly responds.

![pipeline](http://github.com/robey/naggati2/raw/docs/docs/pipeline.png)

## Okay, so how does naggati help?

For a protocol decoder to turn bytes into objects, it needs to implement a
kind of state machine, because there's no guarantee that a complete request
will arrive within one network packet. Typically, a protocol decoder buffers
bytes until a full request is received, then decodes it and passes it down the
filter. If extra bytes are left over, they're left in the buffer for the next
call.

Naggati's primary purpose is to simplify the creation of protocol decoders, by
letting you write sequential-looking code which is actually compiled into a
state machine. It also includes a simple adapter for turning netty events into
actor messages.

## A simple example

Let's build a simple HTTP header decoder using naggati. The resulting code
already lives in `com.twitter.naggati.codec.HttpRequest` if you want to go
peek at it.

First, we'll make case classes for the HTTP request objects:

    case class RequestLine(method: String, resource: String, version: String)
    case class HeaderLine(name: String, value: String)
    case class HttpRequest(request: RequestLine, headers: List[HeaderLine],
                           body: Array[Byte])

Then, we write the first decoding stage:

    val read = readLine(true, "UTF-8") { line =>
      line.split(' ').toList match {
        case method :: resource :: version :: Nil =>
          val requestLine = RequestLine(method, resource, version)
          readHeader(requestLine, Nil)
        case _ =>
          throw new ProtocolError("Malformed request line: " + line)
      }
    }

Each stage is literally an object of type `Stage`. A stage is called when new
data arrives on the socket. It takes the new `ChannelBuffer` and returns one
of three things:

- `Incomplete` if we need more bytes to make progress
- `Emit` if we decoded a full object and are ready to process it
- `GoToStage` if we decoded a partial item, and need to move to a new stage
  to continue

Usually a stage doesn't return `GoToStage` explicitly, because an implicit
conversion is supplied which converts a `Stage` into this return value
automatically. So a stage can return a different stage to implicitly move the
state machine to a new stage.

In many cases, you won't need to write a new stage yourself. Naggati comes
with various stages that perform standard protocol units like "read a line of
text" or "read a 32-bit network-order integer".

For our HTTP request, we just use `readLine` to read a line of text. The final
parameter is a closure (code block) to call when the entire line is read. It
takes the decoded line of text as a parameter and continues decoding. When
we've finished decoding the request line's method, path, and version, we
"call" `readHeader` to move to the stage where we read header lines.

    def readHeader(requestLine: RequestLine, headers: List[HeaderLine]) = {
      readLine(true, "UTF-8") { line =>
        if (line == "") {
          // end of headers
          val contentLength = headers.find { _.name == "content-length" }.map { _.value.toInt }.getOrElse(0)
          readBytes(contentLength) { data =>
            emit(HttpRequest(requestLine, headers.reverse, data))
          }
        } else if (line.length > 0 && (line.head == ' ' || line.head == '\t')) {
          // continuation line
          if (headers.size == 0) {
            throw new ProtocolError("Malformed header line: " + line)
          }
          val newHeaderLine = HeaderLine(headers.head.name, headers.head.value + " " + line.trim)
          readHeader(requestLine, newHeaderLine :: headers.drop(1))
        } else {
          val newHeaderLine = line.split(':').toList match {
            case name :: value :: Nil =>
              HeaderLine(name.trim.toLowerCase, value.trim)
            case _ =>
              throw new ProtocolError("Malformed header line: " + line)
          }
          readHeader(requestLine, newHeaderLine :: headers)
        }
      }
    }

That may look like a lot of code, but it completely handles the parsing of
HTTP request headers. It reads a line of text, and then handles three cases:

1. An empty line indicates that the headers are done. It reads the body
contents into a byte array and sends the `HttpRequest` object down the
pipeline.

2. A line starting with whitespace indicates a continuation of the previous
header line. The new contents are appended to the end of the last header line,
and we continue reading the header.

3. Anything else should be a new "`Name: value`" header line, which is added
to the front of the list. We then continue reading the header.

The recursive calls work because the return value of `readHeader` is really
just a `Stage` object (`readLine`) that contains the rest of the method body
as an attached closure. The closure is only called when a complete line of
text is read from the socket, and each of the recursive calls to `readHeader`
just immediately return another closure of the same type but with different
state. Each recursive call effectively just changes the current stage of the
state machine.

The final step is to wrap the initial `Stage` into a decoder that can be used
as a `ChannelHandler` in a netty pipeline:

    def decoder = new Decoder(read)

This `decoder` object can now be injected into the pipeline for a new netty
channel.

## How to build

