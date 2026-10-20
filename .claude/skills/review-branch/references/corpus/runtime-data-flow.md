# Runtime data-flow

**Transform divergence.** A conversion applied on one path and not its mirror drifts the two
apart: a `ByteBuffer` ↔ `ByteBuf` conversion at the API boundary that copies on send and
wraps on receive (so a user mutates a buffer Netty still owns), a reader index or position left
somewhere else on one side, a string encoded UTF-8 by one writer and read with a different
length rule by its reader. Two paths writing one registry or one connection state with
different normalisation drift the same way. Name a value the divergence breaks.

**Untrusted input driving unbounded work.** Decoded peer data (an array count, a string length,
an endpoint list, a run of padding bytes) needs a bound before the expensive operation:
an allocation, a loop, a re-send, a registry walk per element. Name the operation, the bound
that is missing, and what else is stranded when it dies: a half-applied endpoint list, a
connection left registered after its handler threw.

**The quiet drop.** A message that has nowhere to go and goes nowhere with no signal: a send
for an object with no registration that returns normally, an inbound `Endpoint Message` for an
unknown id ignored with neither a disconnect nor the `Available Endpoints` resend the spec
permits, a write to an inactive channel whose failed
`ChannelFuture` nobody reads. This is the shape of the pre-migration bug where built-in
packets went through the public send path and vanished. Say what the caller concludes ("sent")
and what actually happened.
