# Bend Protocol v1

### Types
| name         | note                                                                                                                                                                                                                                                                                                                                             |
|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| VarInt       | A variable length integer spread over 1-5 bytes, the most significant bit of each byte is used as a continue bit, when this bit is '1' it means there is another byte for this integer, because an integer exists out of 32 bits at maximum and we would use 4 of them as continue bits, thats why the last 4 bits can be found in the 5th byte. The first byte holds the least-significant 7 bits of the value; each following byte holds the next 7 bits, so the value is reassembled least-significant group first. |
| String (n)   | A utf-8 encoded character array prefixed with a VarInt for the length in bytes. Where n is the maximum number of characters.                                                                                                                                                                                                                     |
| Endpoint     | An endpoint is a custom channel/message. It contains an internal id and a public namespaced id. After a client connects, the server sends a list of available endpoints, whereafter the client can declare which endpoints it supports.                                                                                                           |
| <type> Array | A collection of the provided type, array items are not separated by any separator. Arrays are not self-prefixed; their length MUST be defined explicitly in the packet structure.                                                                                                                                                                |

#### Namespaced Id/Key
A namespaced id consists of a namespace string and a key string, both lowercase, a-z, 0-9, dashes/underscores.
They are separated by a colon `:`. These namespaced ids should be unique. The total length is maximum 64 characters.

Implementations MUST validate this format (character set and the single colon-separated namespace/key structure) when
decoding an Endpoint. A namespaced id violating it MUST be treated as a protocol violation.

#### Endpoint
| Field name    | Field type  | Note                                                                                  |
|---------------|-------------|---------------------------------------------------------------------------------------|
| Endpoint ID   | VarInt      | The internal endpoint id. Internal ids do not have to be persistent between sessions. |
| Namespaced ID | String (64) | A public id which the client can use to get the correct internal id for messaging.    |
| Flags         | VarInt      | Bitset describing the direction(s) of this endpoint, see table below.                 |

| Bit | Value | Name        | Note                                                        |
|-----|-------|-------------|-------------------------------------------------------------|
| 0   | 0x01  | Clientbound | The server can send messages to the client on this endpoint |
| 1   | 0x02  | Serverbound | The client can send messages to the server on this endpoint |

At least one direction bit MUST be set. When both are set the endpoint is bidirectional. An endpoint received with
no direction bit set MUST be treated as a protocol violation by the receiver.

### Frame
Frame can not be larger than 4mb (4 * 1024 * 1024), this excludes the length field itself. Sending anything larger will
disconnect the client.

A frame MAY be preceded by `0x00` padding bytes, which readers MUST skip before reading `Length`. A run of more than
16 consecutive `0x00` bytes MUST be treated as a protocol violation (disconnect) rather than awaited as incomplete
data.

A frame's `Length` MUST NOT be `0` (every frame carries at least a 1-byte Packet ID), and a length of `0` MUST be
treated as a protocol violation. This applies even to a non-minimal multi-byte VarInt encoding of `0` (e.g. `0x80 0x00`)
that starts with a non-`0x00` byte, since that is not caught by the leading-padding skip above.

A packet's fields MUST consume exactly `Length` bytes; any bytes left over after decoding a packet's fields MUST be
treated as a protocol violation. This does not apply to `Endpoint Message`, whose payload is explicitly defined to
consume all remaining bytes in the frame.

| field name     | field type | note                                                                                                                                           |
|----------------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| Length         | VarInt     | Number of bytes that follow (Packet ID + Payload). Limited to 4 bytes / 28 bits, narrower than the general VarInt type above (5 bytes / 32 bits), which is never a practical limit given the 4MB frame cap. |
| Packet ID      | VarInt     | The id of the packet. Limited to a single byte (0-255).                                                                                       |
| Packet payload |            | The actual payload                                                                                                                             |

### Flow

1. Client MUST send `Handshake` as the first packet.
2. Server MUST respond with `Available Endpoints` OR `Disconnect`.
3. Client MUST respond with `Available Endpoints Acknowledged`.
4. Server SHOULD start sending `Keep Alive` packets; client MUST echo each one.
5. Server and client CAN now exchange `Endpoint Message`s.

At any point after step 3:
- The server MAY send a new `Available Endpoints`; the client MUST acknowledge it.
- The client MAY send `Supported Endpoints` to declare or update which endpoints it wants to use; the server MUST respond with `Supported Endpoints Acknowledged`.
- The client MUST only include endpoint ids from the most recently acknowledged `Available Endpoints` list.
- When the client's supported endpoints change, it sends the complete current list, replacing the previous one.

#### Endpoint message routing

The server MUST block any `Endpoint Message` from the client on an endpoint that is not in the client's current `Supported Endpoints` list. The server implementation MAY disconnect the client or MAY silently ignore the message and resend `Available Endpoints`.

#### Endpoint list updates

Endpoints cannot be modified, only added or removed. When either side removes an endpoint from its list:

- **Before sending** the updated list (`Available Endpoints` or `Supported Endpoints`), the sender MUST stop sending `Endpoint Message`s on any removed endpoints.
- **After sending the acknowledgement** (`Available Endpoints Acknowledged` or `Supported Endpoints Acknowledged`), the acknowledging side MUST stop accepting incoming `Endpoint Message`s on removed endpoints.

This means in-flight messages on a removed endpoint remain valid until the acknowledgement is sent, so no messages are lost mid-flight.

New endpoints in an updated list MUST NOT be used until the full acknowledge cycle for that list is complete.

#### Revisions

Each side maintains a monotonically increasing revision counter for the list it owns. The server increments `availableRevision` each time it sends `Available Endpoints`; the client increments `supportedRevision` each time it sends `Supported Endpoints`. The acknowledging side echoes the revision back so the sender knows exactly which version was processed. This matters when a new list is sent before the previous one was acknowledged.

The client also includes `availableRevision` in `Supported Endpoints` to tie its supported list to a specific server state.

#### Disconnect conditions

The server MUST disconnect a client when:
- Packets are not sent in the order described above;
- The `Handshake` packet has an unsupported protocol version number;
- The client does not send a `Handshake` within 30 seconds of connecting;
- The client does not acknowledge `Available Endpoints` within 30 seconds of it being sent;
- The client sends invalid built-in packets;
- The client sends an `Endpoint Message` on an unsupported endpoint (the server MAY instead ignore and resend `Available Endpoints`);
- The client sends an `Endpoint Message` on an endpoint that is supported and available, but in a direction not permitted by that endpoint's flags (the server MAY instead ignore and resend `Available Endpoints`).

#### Client-side disconnect conditions

The client MUST disconnect (give up on the connection) when:
- The server does not respond with `Available Endpoints` or `Disconnect` within 30 seconds of the client sending `Handshake`;
- No `Keep Alive` is received from the server within 30 seconds of connecting, or of the previous one;
- The server sends invalid built-in packets;
- The server sends an `Endpoint Message` on an endpoint that is not currently available, or in a direction not permitted by that endpoint's flags;
- Packets are not received in the order described in Flow above.

### Security / Authentication / Encryption
This protocol does not support authentication nor encryption. It is recommended for implementations to add TLS/mTLS
for encryption and/or authentication. The implementation in this project will support an optional mTLS setting.

### Server » Client

#### Disconnect
Sent to the client when the server is about to drop the connection. A reason is provided for debugging.

| packet id | field name | field type   | note                                        |
|-----------|------------|--------------|---------------------------------------------|
| 0x01      | Type       | VarInt       | The disconnect type, see table below        |
|           | Reason     | String (256) | The reason why the client was disconnected. |

| id | note                |
|----|---------------------|
| 0  | Generic; See Reason |
| 1  | Protocol error      |
| 2  | Unsupported version |
| 3  | Timeout             |

#### Available Endpoints
Sent by the server to inform the client what endpoints are available. The client must acknowledge this packet,
and may then declare which endpoints it supports.

This packet can be sent multiple times to update the available endpoints during runtime.

| packet id | field name          | field type     | note                                                          |
|-----------|---------------------|----------------|---------------------------------------------------------------|
| 0x02      | Available Revision  | VarInt         | The server's current revision of the available endpoints list |
|           | Number of endpoints | VarInt         | Number of endpoints in the next array                         |
|           | Endpoints           | Endpoint Array | All available endpoints                                       |

#### Supported Endpoints Acknowledged
Acknowledges that the supported endpoint list has been received and processed.
Removed endpoints MUST NOT be used by the server after this.

| packet id | field name         | field type | note                                                         |
|-----------|--------------------|------------|--------------------------------------------------------------|
| 0x03      | Supported Revision | VarInt     | The supported revision this acknowledgement is responding to |

#### Endpoint Message
Sends a message over a specific endpoint. An endpoint message can only be sent if the server has the endpoint available and the client has declared it supported. The endpoint's flags determine the valid direction.

| packet id | field name      | field type | note                                                    |
|-----------|-----------------|------------|---------------------------------------------------------|
| 0x04      | Endpoint ID     | VarInt     | The internal endpoint id                                |
|           | Message payload | Byte Array | The payload, length is the remaining bytes in the frame |

#### Keep Alive
Keeps the connection alive by periodically sending a value to confirm activity.

- Server sends `Keep Alive` every 5 seconds.
- Client must respond within 30 seconds, echoing back the exact value it received.
- The server MUST verify the echoed value matches the one it sent; a mismatched value MUST be treated the same as a missed response.
- Failure results in disconnect.

| packet id | field name | field type | note                 |
|-----------|------------|------------|----------------------|
| 0x05      | Value      | VarInt     | The keep-alive value |

### Client » Server

#### Handshake

| packet id | field name              | field type  | note                                                                                 |
|-----------|-------------------------|-------------|--------------------------------------------------------------------------------------|
| 0x01      | Protocol version number | VarInt      | The client chooses the protocol version. The server can reject it with `Disconnect`. |
|           | Client identifier       | String (64) | The client's name/identifier, used only for debugging/logging internally.            |

#### Available Endpoints Acknowledged
Acknowledges that the available endpoint list has been received and processed.
Removed endpoints MUST NOT be used by the client after this.

| packet id | field name         | field type | note                                                         |
|-----------|--------------------|------------|--------------------------------------------------------------|
| 0x02      | Available Revision | VarInt     | The available revision this acknowledgement is responding to |

#### Supported Endpoints
Sends the complete list of endpoint ids the client currently supports. Can be sent at any time after step 3
to declare or update the supported list.

| packet id | field name             | field type   | note                                                          |
|-----------|------------------------|--------------|---------------------------------------------------------------|
| 0x03      | Available Revision     | VarInt       | The available revision this supported list is responding to   |
|           | Supported Revision     | VarInt       | The client's current revision of the supported endpoints list |
|           | Number of endpoint ids | VarInt       | Number of endpoint ids in the next array                      |
|           | Endpoint ids           | VarInt Array | An array of endpoint ids the client currently supports        |

#### Endpoint Message
Sends a message over a specific endpoint. An endpoint message can only be sent if the server has the endpoint available and the client has declared it supported. The endpoint's flags determine the valid direction.

| packet id | field name      | field type | note                                                    |
|-----------|-----------------|------------|---------------------------------------------------------|
| 0x04      | Endpoint ID     | VarInt     | The internal endpoint id                                |
|           | Message payload | Byte Array | The payload, length is the remaining bytes in the frame |

#### Keep Alive
The client responds with the same keep-alive value to confirm it is still active.

| packet id | field name | field type | note                 |
|-----------|------------|------------|----------------------|
| 0x05      | Value      | VarInt     | The keep-alive value |
