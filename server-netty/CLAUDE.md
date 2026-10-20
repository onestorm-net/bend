# server-netty

The Netty server implementation. Rules here are one line each; the linked ADR holds the
reasoning.

- Endpoint register, unregister and routing live here; there is no `server-api` in 0.1.
  ([0016](/docs/decisions/0016-module-set-0-1.md))
- Register, unregister and endpoint-handler signatures contain no Netty types.
  ([0016](/docs/decisions/0016-module-set-0-1.md))
- No Javadoc: this is an implementation module. ([0021](/docs/decisions/0021-javadoc-scope.md))
