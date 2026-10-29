# BD-0003-endpoint-registration-typed-handles: Decisions

## D1: Register methods take a `NamespacedId`, not a `String`
**Status:** Accepted

**Context:** The issue and BD-0001 D2 assumed `registerOutgoing(String id, ...)`, parsed with `NamespacedId.of`.

**Decision:** Both register methods take `NamespacedId id`. The value type exists so the API carries a valid id; an invalid one can't reach `register*`, because `NamespacedId.of` already throws `IllegalArgumentException`. `of(String)` stays the only factory: callers use it to build the argument.

**Rejected:**
- `String` only: duplicates the parsing rule's error path in every implementation, and the API never uses its own value type on input.
- Both overloads: four register methods for two use cases.

## D2: The shared parent is `EndpointHandle`
**Status:** Accepted

**Context:** The issue names the parent `Endpoint`, which is also the simple name of `net.onestorm.bend.protocol.endpoint.Endpoint`, used throughout `client-netty`.

**Decision:** The parent is `EndpointHandle`, non-generic, so handles of different message types sit in one collection; the children keep the issue's names `IncomingEndpoint<T>` and `OutgoingEndpoint<T>`. Both children carry `T` now, even where no member uses it yet, because adding a type parameter later breaks every caller's source. API names take priority over implementation names, and "handle" says what `register*` returns.

**Rejected:**
- `Endpoint`: same simple name as the protocol wire type.
- `Endpoint<T>`: the parent's members never depend on the message type, and a typed parent makes mixed collections need wildcards.
- `Registration` / `EndpointSender` families: `send` on a "registration" reads oddly, and role names lose the pairing with `registerIncoming`/`registerOutgoing`.

## D3: An id can be registered again after `unregister()`
**Status:** Accepted

**Context:** Runtime add and remove is a core feature (ADR 0016); the issue says nothing about reuse after unregister.

**Decision:** After a handle's `unregister()`, the same id may be registered again in that direction; the call returns a new handle. The old handle stays unregistered for good: calling its `unregister()` again does nothing and never affects the new registration.

**Rejected:**
- An id burned for the life of the `Client`: blocks swapping a codec or handler at runtime.

## D4: ADR 0019 counts both directions and adds a local-unregister trigger
**Status:** Accepted

**Context:** ADR 0019 resends the supported set on two triggers and its Context says "ids the client has local handlers for". protocol-v1.md § Endpoint message routing has the server block client messages on ids outside Supported Endpoints, so an outgoing-only registration must be declared too, and an unregister must withdraw it.

**Decision:** Update ADR 0019: the supported set is the available ids that have at least one local registration, incoming or outgoing; the triggers are a new `Available Endpoints` list, a registration whose id is available, and an unregister that removes the last registration for an available id. Still a full snapshot.

**Rejected:**
- Leaving the ADR to the client-netty epic: it would build the supported set from a Decision that is wrong for outgoing endpoints.

## D5: No test class in `client-api`
**Status:** Accepted

**Context:** `Client`, `EndpointHandle`, `IncomingEndpoint` and `OutgoingEndpoint` are interfaces without default methods.

**Decision:** No test class, for BD-0002 D7's reason: no executable code, so a stub-based test cannot fail for any change to the contract. Verification is `javadoc` and the empty dependency block; duplicate, re-register and idempotent-unregister behaviour is tested in `client-netty`.

**Rejected:**
- A stub `Client` in tests: it tests the stub.

## D6: A call the current state forbids raises `IllegalStateException`
**Status:** Accepted

**Context:** Registering an id already registered in that direction is caller misuse, but the argument is valid; it's the `Client`'s state that conflicts. CLAUDE.md § API design decisions and ADR 0011 name only `IllegalArgumentException` for caller misuse.

**Decision:** `IllegalArgumentException` stays for a bad argument; a call the object's current state forbids raises `IllegalStateException`, as in the JDK. Duplicate registration is the first case; BD-0004 (send after own unregister) and BD-0005 (register after close) follow. ADR 0011 gains this as a Decision bullet and the CLAUDE.md line names both.

**Rejected:**
- `IllegalArgumentException` for duplicates: the same id is valid on another client, or after `unregister()`, so blaming the argument misleads.
