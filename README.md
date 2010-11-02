
# Naggati 2.0

Naggati is a protocol builder for netty, using scala 2.8.

Netty is a high-performance java library for doing asynchronous event-driven
I/O. You can check out their website here: <http://www.jboss.org/netty>

Netty works by creating one or more Selectors that poll a set of active
sockets. When a socket receives data, netty sends the received bytes through a
processing pipeline. Usually the pipeline decodes these bytes into a "message"
for whatever protocol you're speaking -- for example, in HTTP, the pipeline
might decode the received bytes into an `HTTPRequest` object that contains the
method, path, HTTP version, and headers. The last receiver on the pipeline is
your session object, which processes the message and possibly responds.
