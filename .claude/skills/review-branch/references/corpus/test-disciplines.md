# Test disciplines

This section rides only when the diff changes a test file (`*Test.java`, anything under a
module's `src/test/`). When it does not, say so in `checked` and stop. Apply it to every test the diff touches and to every claim you support
with one.

- **An assertion this diff edited is the change, not its proof.** Never cite an expected
  value this diff wrote as evidence the new behaviour is intended. When expected values
  change in the same diff as the behaviour they pin, say so plainly ("the pinning test
  was edited to agree") and grade code and test as one change against the recorded
  rulings and the pre-existing contract, never against each other.
- **A test this diff weakened pins less than it did.** A matcher loosened, an assertion
  deleted, a case dropped, a `@Disabled` added, an `isInstanceOf(Exception.class)` where the
  test used to pin `ProtocolException`: each retires a
  defect the suite could catch, and the diff that retires one owes its reason. Name the
  regression that now passes: the value the old assertion rejected and the new one accepts.
- **A test's name is a claim.** Read the assertions and say what they actually pin. A test
  named for a rule that asserts only a weaker property certifies nothing about the rule:
  a round-trip that matches does not pin the byte layout the spec requires. The gap between
  the name and the assertions is a finding under the covering-test rule.
- **A test that can fail only on its own setup is ceremony.** `CLAUDE.md` states Bend's
  testing rules (Testing / coverage): tests are named `method_condition_expected`, and a
  coverage gap on an unreachable line is a prompt to delete the line, never a reason for a
  contrived test (reflection, a hand-built impossible state) that turns it green. Hunt each in
  every test the diff touches; name the rule a test breaks. Three shapes regardless: a rule
  proven at two layers, such as a pipeline test re-asserting a `ProtocolUtil` unit test; an
  assertion on what the test's own Arrange put there, such as reading back the bytes the test
  just wrote into the buffer; a fixture sized to a limit, a 4MB array to see the cap fire,
  where a small configured `maximumFrameLength` would prove the rule. Name the defect the test
  cannot fail on.
- **Coverage attests execution, not correctness.** A suite can run every changed line and
  still be unable to fail on the defect class the change risks. Name what the suite is
  blind to, never what percentage it runs.
