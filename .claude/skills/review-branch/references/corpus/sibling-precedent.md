# Sibling precedent

Before accepting any novel shape, find the code that already solves it: another packet class,
another `ProtocolUtil` reader, the matching codec on the other side of the wire, the client
counterpart of a server handler, another test class in the same package.

List every new or moved unit the diff introduces (a packet, a codec or pipeline handler, a
`ProtocolUtil` helper, a registry or dispatch path, a public API type, a test class) and, for
each, name the sibling you compared it against or state that you grepped and found none.
Finding the sibling is the expensive part of this review and the thing that makes findings
actionable; budget for it. A claim that nothing like this exists is a `confirmed` claim of
absence and needs the grep behind it in `checked`.

Three shapes:

- **A second implementation of something a shared helper or a sibling already provides.** A
  hand-rolled VarInt, string, or array read instead of `ProtocolUtil`; a length check written
  inline instead of `ProtocolUtil.validateArrayLength`; a new exception type where
  `ProtocolException` fits.
- **A convention the sibling applies that this diff drops.** Packets that validate in the
  constructor so factory and wire decode share one check; codecs that take their cap as a
  required constructor argument and clamp it; throw sites that use `ProtocolException` with a
  specific message; tests named `method_condition_expected` using AssertJ. Check the pre-diff
  version of a moved unit: a *removed* convention is a regression and weighs more than one that
  was never there.
- **A cross-wire contract the sibling establishes that this diff diverges from**: a new packet
  whose field layout or naming departs from its peers, a client-side rule with no server-side
  counterpart (or the reverse) where `protocol-v1.md` treats the two sides symmetrically.

Name the sibling `file:line` on every finding. A finding with no named sibling is preference,
and preference is not this section: "could be shorter", "extract a helper" are not findings
unless a sibling establishes the shape. Precedent is the standard, not taste.
