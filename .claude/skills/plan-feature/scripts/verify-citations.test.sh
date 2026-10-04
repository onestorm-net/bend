#!/usr/bin/env bash
#
# Tests for verify-citations.sh.
#
# Every case below is a regression. The resolver shipped several times with a
# bug that a two-line fixture would have caught for free:
#
#   v1  passed every phantom from the calibration plan, because a path-shaped
#       citation fell back to the symbol grep and .claude/ was in the search
#       roots, so anti-patterns.md, the file recording those citations as
#       fabricated, vouched for them.
#   v2  reported real files as MISSING when the citation carried a line range,
#       because the strip rule only handled `:42` and not `:233-234`.
#   v3  reported real files as MISSING when that line range was wrapped in
#       prose punctuation, `(Foo.java:233-234)`, because the strip rules ran
#       in an order that let the `)` block the line rule's anchor.
#   v4  would have reported every line-referenced citation as MISSING under
#       BSD sed, because the line rule used GNU-only BRE `\+`. No fixture below
#       can catch that on a GNU sed (Git Bash ships one), so it is pinned
#       statically under "Portability" instead.
#
# The Bend adaptation added member citations (`ProtocolUtil#readVarInt`,
# `ProtocolUtil.readVarInt()`), qualified names, and the build/ exclusion; each
# has fixtures below in both directions.
#
# Both directions matter and both are pinned here. A missed phantom costs a
# reviewer round; a false MISSING teaches the planner the gate is wrong and can
# be worked around, which costs every future catch.
#
# No framework by design: Bend has no shell test harness and no CI, so the
# suite runs as a plain script:
#
#   bash "$(git rev-parse --show-toplevel)/.claude/skills/plan-feature/scripts/verify-citations.test.sh"
#
# The OK cases resolve against a throwaway fixture repository built in a temp
# directory, not against the repo the script ships in, so the suite does not
# drift when Bend's own sources move. The fixture mirrors Bend's shape (Gradle
# multi-module: protocol/ and client-api/ with src/main/java + src/test/java,
# gradle/, root build scripts) plus a prose tree and a build/ tree that mention
# the phantoms, which is the trap v1 fell into.

set -uo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
subject="$script_dir/verify-citations.sh"

if [ ! -f "$subject" ]; then
    echo "verify-citations.sh not found at $subject" >&2
    exit 2
fi

# --- Fixture -----------------------------------------------------------------

fixture=$(mktemp -d "${TMPDIR:-/tmp}/verify-citations.XXXXXX")
trap 'rm -rf "$fixture"' EXIT

build_fixture() {
    local dir=$1 pkg=net/onestorm/bend/protocol
    git -c init.defaultBranch=main init -q "$dir"
    mkdir -p "$dir/protocol/src/main/java/$pkg/codec" "$dir/protocol/src/main/java/$pkg/util"              "$dir/protocol/src/test/java/$pkg/util" "$dir/protocol/build/classes"              "$dir/client-api/src/main/java/net/onestorm/bend/client" "$dir/gradle"              "$dir/.claude/skills/plan-feature/references" "$dir/docs/issues/BD-0001-example"
    printf 'package net.onestorm.bend.protocol.codec;
public class VarInt28FrameDecoder {
    public static final int MAXIMUM_FRAME_LENGTH = 268435455;
    protected void decode() {}
}
'         > "$dir/protocol/src/main/java/$pkg/codec/VarInt28FrameDecoder.java"
    printf 'package net.onestorm.bend.protocol.util;
public final class ProtocolUtil {
    public static int readVarInt(Object buffer) { return 0; }
}
'         > "$dir/protocol/src/main/java/$pkg/util/ProtocolUtil.java"
    printf 'package net.onestorm.bend.protocol.util;
class ProtocolUtilTest {
    void readVarInt_truncated_throws() {}
}
'         > "$dir/protocol/src/test/java/$pkg/util/ProtocolUtilTest.java"
    printf 'package net.onestorm.bend.client;
public interface Client {}
'         > "$dir/client-api/src/main/java/net/onestorm/bend/client/Client.java"
    printf '[versions]
netty = "4.2.13.Final"
' > "$dir/gradle/libs.versions.toml"
    printf 'subprojects { apply<JavaLibraryPlugin>() }
' > "$dir/build.gradle.kts"
    printf 'rootProject.name = "bend"
' > "$dir/settings.gradle.kts"
    printf 'Frames MUST NOT exceed the cap.
' > "$dir/protocol/protocol-v1.md"
    # The phantoms appear ONLY in prose and in build output. If the resolver
    # ever greps these trees, the phantoms come back OK and the v1 regression
    # is back.
    printf 'Phantom path: `protocol/src/main/java/net/onestorm/bend/protocol/support/Rank.java`
Phantom symbol: `PhantomFrameDecoder`
'         > "$dir/.claude/skills/plan-feature/references/anti-patterns.md"
    printf 'See PhantomFrameDecoder and PhantomEndpointRegistry
'         > "$dir/docs/issues/BD-0001-example/PLAN.md"
    printf 'PhantomBuildSymbol
' > "$dir/protocol/build/classes/leak.txt"
    printf 'RootReadmeSymbol
' > "$dir/README.md"
}

# A repo with none of the default source trees: pins the whole-repo fallback
# and the prose and build exclusions that keep it honest.
build_flat_fixture() {
    local dir=$1
    git -c init.defaultBranch=main init -q "$dir"
    mkdir -p "$dir/lib/core" "$dir/.claude" "$dir/docs" "$dir/build"
    printf 'public final class FlatRepoThing { void run() {} }
' > "$dir/lib/core/FlatRepoThing.java"
    printf 'OnlyInProse is a phantom
' > "$dir/.claude/notes.md"
    printf 'OnlyInDocs is a phantom
' > "$dir/docs/notes.txt"
    printf 'OnlyInBuild is a phantom
' > "$dir/build/out.txt"
}

build_fixture "$fixture/layered"
build_flat_fixture "$fixture/flat"
cd "$fixture/layered" || exit 2

# --- Harness -----------------------------------------------------------------

passed=0
failed=0

# assert_verdict <OK|MISSING> <citation> <description>
assert_verdict() {
    local expected=$1 citation=$2 description=$3 output actual
    output=$(printf '%s\n' "$citation" | bash "$subject" 2>&1)
    actual=$(printf '%s' "$output" | awk 'NR==1 {print $1}')

    if [ "$actual" = "$expected" ]; then
        passed=$((passed + 1))
        printf '  ok    %s\n' "$description"
    else
        failed=$((failed + 1))
        printf '  FAIL  %s\n        expected %s, got %s\n' \
            "$description" "$expected" "${actual:-<no output>}"
    fi
}

# assert_exit <expected code> <description> <citation>...
assert_exit() {
    local expected=$1 description=$2 actual
    shift 2
    printf '%s\n' "$@" | bash "$subject" >/dev/null 2>&1
    actual=$?

    if [ "$actual" = "$expected" ]; then
        passed=$((passed + 1))
        printf '  ok    %s\n' "$description"
    else
        failed=$((failed + 1))
        printf '  FAIL  %s\n        expected exit %s, got %s\n' \
            "$description" "$expected" "$actual"
    fi
}

# assert_output <substring> <description> <citation>...
assert_output() {
    local expected=$1 description=$2 output
    shift 2
    output=$(printf '%s\n' "$@" | bash "$subject" 2>&1)

    if printf '%s' "$output" | grep -qF -- "$expected"; then
        passed=$((passed + 1))
        printf '  ok    %s\n' "$description"
    else
        failed=$((failed + 1))
        printf '  FAIL  %s\n        expected output containing: %s\n        got:\n%s\n' \
            "$description" "$expected" "$output"
    fi
}

# --- Cases -------------------------------------------------------------------

decoder='protocol/src/main/java/net/onestorm/bend/protocol/codec/VarInt28FrameDecoder.java'

echo "Resolving real citations"
assert_verdict OK "$decoder"                         'path from repo root'
assert_verdict OK 'net/onestorm/bend/protocol/util/ProtocolUtil.java' \
    'path relative to a module src/main/java/'
assert_verdict OK 'net/onestorm/bend/protocol/util/ProtocolUtilTest.java' \
    'path relative to a module src/test/java/'
assert_verdict OK 'client-api/src/main/java/'        'directory'
assert_verdict OK 'build.gradle.kts'                 'root build script'
assert_verdict OK 'gradle/libs.versions.toml'        'version catalog'
assert_verdict OK 'protocol/protocol-v1.md'          'markdown spec is citable as a path'
assert_verdict OK 'VarInt28FrameDecoder'             'bare class name'
assert_verdict OK 'MAXIMUM_FRAME_LENGTH'             'bare constant'
assert_verdict OK 'JavaLibraryPlugin'                'symbol in a root build script'
assert_verdict OK 'netty'                            'symbol in gradle/'

echo
echo "Resolving members"
assert_verdict OK 'ProtocolUtil#readVarInt'          'Class#member'
assert_verdict OK 'ProtocolUtil.readVarInt'          'Class.member'
assert_verdict OK 'ProtocolUtil#readVarInt()'        'member with empty argument list'
assert_verdict OK 'ProtocolUtil#readVarInt(ByteBuf)' 'member with argument list'
assert_verdict OK '(ProtocolUtil#readVarInt)'        'member wrapped in parens'
assert_verdict OK 'net.onestorm.bend.protocol.util.ProtocolUtil#readVarInt' \
    'qualified class with member'
assert_verdict OK 'VarInt28FrameDecoder#MAXIMUM_FRAME_LENGTH' 'Class#CONSTANT'
assert_verdict MISSING 'ProtocolUtil#readVarLong'    'real class, phantom member'
assert_verdict MISSING 'ProtocolUtil.readVarLong()'  'real class, phantom member, dotted'
assert_verdict MISSING 'PhantomFrameDecoder#decode'  'phantom class with a real member name'
# Netty's own classes are not in the source trees: a framework claim cites
# Netty's source, it cannot pass through a member lookup.
assert_verdict MISSING 'ByteToMessageDecoder#decode' 'framework class is not vouched for'

echo
echo "Resolving qualified names"
assert_verdict OK 'net.onestorm.bend.protocol.codec' 'package'
assert_verdict OK 'net.onestorm.bend.protocol.util.ProtocolUtil' 'fully qualified class'
assert_verdict MISSING 'net.onestorm.bend.protocol.support' 'phantom package'

echo
echo "Rejecting the calibration phantoms"
# Path-shaped and MISSING even though the literal string appears in
# references/anti-patterns.md: pins "a path-shaped citation never falls back
# to the symbol grep".
assert_verdict MISSING 'protocol/src/main/java/net/onestorm/bend/protocol/support/Rank.java' \
    'phantom path is not rescued by the symbol grep'
assert_verdict MISSING 'Rank.java'                   'bare file name is path-shaped'
# Symbol-shaped and present ONLY in .claude/, docs/ and build/: pins the
# exclusions.
assert_verdict MISSING 'PhantomFrameDecoder'         'phantom documented in .claude/ stays MISSING'
assert_verdict MISSING 'PhantomEndpointRegistry'     'phantom documented in docs/ stays MISSING'
assert_verdict MISSING 'PhantomBuildSymbol'          'symbol only in build/ output stays MISSING'
assert_verdict MISSING 'RootReadmeSymbol'            'root markdown is not grepped for symbols'
assert_verdict MISSING 'ThisSymbolDoesNotExistAnywhere' 'unknown symbol'
assert_verdict MISSING 'protocol/does-not-exist.java' 'unknown path'

echo
echo "Tolerating line references on a real path"
assert_verdict OK "$decoder:42"                      'single line'
assert_verdict OK "$decoder:40-52"                   'line range'
assert_verdict OK 'net/onestorm/bend/protocol/util/ProtocolUtil.java:28,54' 'line set'
assert_verdict OK "$decoder:12-40,55"                'mixed range and set'
# A line reference wrapped in prose punctuation: the shape that regressed when
# the strip rules ran in the wrong order. Neither half is exercised by the
# plain-punctuation or plain-line-reference cases above; only the combination
# is, so it needs its own fixtures.
assert_verdict OK "$decoder:40-52)"                  'line range followed by a closing paren'
assert_verdict OK "($decoder:40-52)"                 'line range wrapped in parens'
assert_verdict OK "$decoder:28,54."                  'line set ending a sentence'

echo
echo "Normalising how citations get pasted"
# The literal backticks are the fixture: these two cases assert the resolver
# strips them off a citation pasted out of markdown. Single quotes are required
# to keep them literal, so SC2016 is inverted here.
# shellcheck disable=SC2016
assert_verdict OK '`build.gradle.kts`'               'backtick-wrapped'
# shellcheck disable=SC2016
assert_verdict OK '- `ProtocolUtil#readVarInt`'      'markdown list item'
assert_verdict OK '  build.gradle.kts  '             'surrounding whitespace'
assert_verdict OK 'build.gradle.kts.'                'trailing sentence punctuation'

echo
echo "Skipping blanks and comments"
assert_output 'All 1 citations resolve.' 'comments and blank lines are not counted' \
    '# a comment' '' 'build.gradle.kts'

echo
echo "Honouring configuration"
assert_verdict MISSING 'codec/VarInt28FrameDecoder.java' 'a prefix outside the default list does not resolve'
CITATION_PATH_PREFIXES='protocol/src/main/java/net/onestorm/bend/protocol/' \
    assert_verdict OK 'codec/VarInt28FrameDecoder.java' 'CITATION_PATH_PREFIXES adds a prefix'
CITATION_SEARCH_ROOTS='client-api/src' \
    assert_verdict MISSING 'VarInt28FrameDecoder' 'CITATION_SEARCH_ROOTS narrows the symbol grep'
CITATION_SEARCH_ROOTS='client-api/src' \
    assert_verdict MISSING 'JavaLibraryPlugin' 'CITATION_SEARCH_ROOTS drops the root build scripts'

echo
echo "Falling back to the whole repo when no default tree exists"
cd "$fixture/flat" || exit 2
assert_verdict OK 'FlatRepoThing'      'symbol found outside the default trees'
assert_verdict OK 'FlatRepoThing#run' 'member lookup uses the fallback too'
assert_verdict MISSING 'OnlyInProse'   'fallback still skips .claude/'
assert_verdict MISSING 'OnlyInDocs'    'fallback still skips docs/'
assert_verdict MISSING 'OnlyInBuild'   'fallback still skips build/'
cd "$fixture/layered" || exit 2

echo
echo "Portability"
# Static rather than behavioural, deliberately. GNU sed accepts `\+`, so every
# assertion above passes on Git Bash and Linux while the same strip rule
# matches nothing under POSIX BRE (BSD sed). Reproducing that needs a sed the
# runner does not have, so the check that runs everywhere is "the script never
# relies on the extension". Comment lines are excluded: one of them names `\+`
# in order to explain this.
if grep -vE '^[[:space:]]*#' "$subject" | grep -qE '\\\+|\\\|'; then
    failed=$((failed + 1))
    printf '  FAIL  %s\n        found GNU-only BRE; the strip rules need sed -E\n' \
        'strip rules avoid GNU-only BRE'
else
    passed=$((passed + 1))
    printf '  ok    %s\n' 'strip rules avoid GNU-only BRE'
fi

echo
echo "Exit codes"
assert_exit 0 'clean run exits 0' 'build.gradle.kts' 'VarInt28FrameDecoder'
assert_exit 1 'any MISSING exits 1' 'build.gradle.kts' 'PhantomFrameDecoder'
assert_exit 1 'all MISSING exits 1' 'PhantomFrameDecoder'
# Reports on every citation rather than bailing at the first failure: the
# behaviour Phase 1.4's prose promises.
assert_output '2 of 3 citations do not resolve.' 'reports all failures in one pass' \
    'PhantomFrameDecoder' 'build.gradle.kts' 'ThisSymbolDoesNotExistAnywhere'

echo
if [ "$failed" -gt 0 ]; then
    echo "FAILED: $failed of $((passed + failed)) assertions"
    exit 1
fi

echo "OK: all $passed assertions passed"
