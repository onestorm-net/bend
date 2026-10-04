#!/usr/bin/env bash
#
# Resolve every path and symbol a plan is about to cite.
#
# Fabricated citations are the failure mode that once cost a plan three
# reviewer rounds (see references/anti-patterns.md § Calibration): a phantom
# directory, a phantom package, and a precedent that did not exist, each one a
# two-second lookup. This script makes the lookup mechanical so the planner
# cannot self-report its way past it.
#
# Usage:
#   bash verify-citations.sh <file>          # one citation per line
#   printf '%s\n' a b | bash verify-citations.sh
#
# Each line is one of:
#   - a path (`protocol/src/main/java/.../VarInt28FrameDecoder.java`,
#     `build.gradle.kts`, `protocol/protocol-v1.md`). Resolved against the repo
#     root and against each path prefix (default: every module's
#     `src/main/java/` and `src/test/java/`, so a package-relative path like
#     `net/onestorm/bend/protocol/util/ProtocolUtil.java` resolves too).
#   - a member (`ProtocolUtil#readVarInt`, `ProtocolUtil.readVarInt`,
#     `ProtocolUtil#readVarInt(ByteBuf)`). The class must exist as `<Class>.java`
#     in the search roots and the member name must appear in that file.
#     Inherited members (a Netty superclass method) do not resolve: a framework
#     claim cites Netty's source instead, see SKILL.md Phase 1.4.
#   - a qualified name (`net.onestorm.bend.protocol.codec`,
#     `net.onestorm.bend.protocol.util.ProtocolUtil`). Resolved as a package
#     directory or class file under the path prefixes, then grepped literally.
#   - a bare symbol (`VarInt28FrameDecoder`, `MAXIMUM_FRAME_LENGTH`). Grepped
#     across the source trees.
# Backticks, list markers, wrapping punctuation, a trailing argument list and a
# trailing line reference (`:42`, `:233-234`, `:93,109`) are stripped first.
#
# Configuration (optional, space-separated, relative to the repo root):
#   CITATION_PATH_PREFIXES   prefixes a path may be relative to
#                            (default: <module>/src/main/java/ and
#                            <module>/src/test/java/ for every Bend module)
#   CITATION_SEARCH_ROOTS    trees to grep for a bare symbol
#                            (default: the module source trees and gradle/
#                            that exist, plus the files at the repo root; falls
#                            back to the whole repo minus prose and build trees)
#
# Exits 0 when every citation resolves, 1 when any is MISSING.

# -e is deliberately absent: every citation must be reported in one pass, so a
# failed lookup has to fall through to the next line rather than kill the run.
set -uo pipefail

root=$(git rev-parse --show-toplevel 2>/dev/null) || {
    echo "verify-citations: not inside a git repository" >&2
    exit 2
}

modules="protocol client-api client-netty server-netty"

# Path prefixes a citation may be relative to, in resolution order. The empty
# prefix (repo root) is always tried first.
prefixes=("")
if [ -n "${CITATION_PATH_PREFIXES:-}" ]; then
    # shellcheck disable=SC2206
    prefixes+=(${CITATION_PATH_PREFIXES})
else
    for module in $modules; do
        prefixes+=("$module/src/main/java/" "$module/src/test/java/")
    done
fi

# Directories never searched. build/ and .gradle/ hold generated copies and
# reports that would vouch for anything once compiled; .idea/ and .git/ are
# tooling. The prose trees (.claude/, docs/, and every *.md file) are excluded
# because a document that *discusses* a phantom symbol contains its name:
# grepping them would make anti-patterns.md vouch for the very citations it
# records as fabricated. A Markdown file is still citable as a *path*.
excluded_dirs=(build .gradle .idea .git .claude docs)
grep_excludes=()
find_prunes=()
for dir in "${excluded_dirs[@]}"; do
    grep_excludes+=("--exclude-dir=$dir")
    [ ${#find_prunes[@]} -gt 0 ] && find_prunes+=(-o)
    find_prunes+=(-name "$dir")
done

default_search_roots="protocol/src client-api/src client-netty/src server-netty/src gradle"

search_roots=()
for candidate in ${CITATION_SEARCH_ROOTS:-$default_search_roots}; do
    [ -d "$root/$candidate" ] && search_roots+=("$root/$candidate")
done
if [ ${#search_roots[@]} -eq 0 ]; then
    # No source tree at all: grep the whole repo. The prose and build
    # exclusions keep that fallback honest.
    search_roots=("$root")
elif [ -z "${CITATION_SEARCH_ROOTS:-}" ]; then
    # The default roots also cover the build scripts at the repo root
    # (build.gradle.kts, settings.gradle.kts), but not its subdirectories.
    while IFS= read -r file; do
        search_roots+=("$file")
    done < <(find "$root" -maxdepth 1 -type f ! -name '*.md' 2>/dev/null)
fi

resolve_path() {
    local citation=$1 prefix
    for prefix in "${prefixes[@]}"; do
        if [ -e "$root/$prefix$citation" ]; then
            printf '%s' "$prefix$citation"
            return 0
        fi
    done
    return 1
}

resolve_symbol() {
    local citation=$1
    grep -rIqF "${grep_excludes[@]}" --exclude='*.md' \
        -- "$citation" "${search_roots[@]}" 2>/dev/null
}

# Every `<Class>.java` under the search roots, one per line.
find_class_files() {
    local class=$1
    find "${search_roots[@]}" \( -type d \( "${find_prunes[@]}" \) -prune \) \
        -o -type f -name "$class.java" -print 2>/dev/null
}

# `Class#member` / `Class.member`: the class file must exist and name the
# member. A class that exists with no such member is MISSING, not OK: "the
# decoder has a reset() method" is exactly the confident claim this catches.
resolve_member() {
    local class=$1 member=$2 file found_class=1
    while IFS= read -r file; do
        [ -z "$file" ] && continue
        found_class=0
        if grep -qw -- "$member" "$file" 2>/dev/null; then
            printf '%s' "${file#"$root"/}"
            return 0
        fi
    done < <(find_class_files "$class")
    return $((1 + found_class))
}

# A citation that names a file or directory is resolved as a path and never
# falls back to the symbol grep. `protocol/src/.../Rank.java` appearing as a
# string somewhere is not evidence that the file exists; that fallback is
# exactly how a phantom path earns an OK.
is_path_shaped() {
    case "$1" in
        */*) return 0 ;;
        *.java | *.kts | *.gradle | *.toml | *.xml | *.properties | *.md \
            | *.yaml | *.yml | *.json | *.sh)
            return 0 ;;
        *) return 1 ;;
    esac
}

# Class#member, Class.member, or pkg.Class#member. Sets `class` and `member`.
# The class is the last dotted segment before the member and must start
# upper-case, so a package or qualified class name (`net.onestorm.bend.protocol`,
# `net.onestorm.bend.protocol.util.ProtocolUtil`) never lands here.
is_member_shaped() {
    [[ "$1" =~ ^([A-Za-z_][A-Za-z0-9_.]*)[#.]([A-Za-z_][A-Za-z0-9_]*)$ ]] || return 1
    class=${BASH_REMATCH[1]##*.}
    member=${BASH_REMATCH[2]}
    case "$class" in [A-Z]*) return 0 ;; *) return 1 ;; esac
}

missing=0
checked=0

while IFS= read -r line || [ -n "$line" ]; do
    # Strip list markers, backticks, surrounding whitespace, wrapping
    # punctuation, a trailing argument list, and a trailing line reference.
    #
    # The line reference has to cover ranges and sets, not just `:42`. Plans
    # cite `VarInt28FrameDecoder.java:40-52` and `ProtocolUtil.java:28,54`.
    # Stripping only `:42` left the rest attached to the path, which reported
    # a real file as MISSING. A false positive in a fail-closed gate is worse
    # than a missed one: it teaches the planner the script is wrong and can be
    # ignored.
    #
    # Punctuation is stripped BEFORE the line reference, and that order is
    # load-bearing. Prose cites `(ProtocolUtil.java:28-54)`; with the line rule
    # first the trailing `)` blocks its `$` anchor, the `:28-54` survives into
    # the path, and a real file comes back MISSING, the same false positive by
    # a different route. Both ends of the wrap have to go for the same reason.
    #
    # The argument-list rule runs after the punctuation rule, which has already
    # eaten the closing paren: `readVarInt(ByteBuf)` is `readVarInt(ByteBuf` by
    # then, and `readVarInt()` is `readVarInt(`. The rule drops an unclosed
    # trailing `(` and whatever follows it.
    #
    # -E (extended regex) is required, not cosmetic. The line rule needs `+`,
    # and BRE `\+` is a GNU extension: under POSIX BRE (BSD sed) the rule does
    # not fire and every line-referenced citation resolves MISSING.
    citation=$(printf '%s' "$line" \
        | sed -E -e 's/^[[:space:]]*[-*+][[:space:]]*//' \
              -e 's/`//g' \
              -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' \
              -e 's/^[([]*//' \
              -e 's/[.,;)]*$//' \
              -e 's/[(][^()]*$//' \
              -e 's/:[0-9]+([,-][0-9]+)*$//')

    [ -z "$citation" ] && continue
    case "$citation" in \#*) continue ;; esac

    checked=$((checked + 1))

    if resolved=$(resolve_path "$citation"); then
        printf 'OK       %-58s → %s\n' "$citation" "$resolved"
    elif is_path_shaped "$citation"; then
        printf 'MISSING  %-58s → no such file or directory\n' "$citation"
        missing=$((missing + 1))
    elif is_member_shaped "$citation"; then
        resolved=$(resolve_member "$class" "$member")
        status=$?
        if [ "$status" -eq 0 ]; then
            printf 'OK       %-58s → %s\n' "$citation" "$resolved"
        elif [ "$status" -eq 1 ]; then
            printf 'MISSING  %-58s → %s.java has no member %s\n' "$citation" "$class" "$member"
            missing=$((missing + 1))
        else
            printf 'MISSING  %-58s → no %s.java in source\n' "$citation" "$class"
            missing=$((missing + 1))
        fi
    elif [[ "$citation" =~ ^[a-z][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+$ ]] \
        && { resolved=$(resolve_path "${citation//.//}") \
             || resolved=$(resolve_path "${citation//.//}.java"); }; then
        # Qualified name: a package directory or a class file.
        printf 'OK       %-58s → %s\n' "$citation" "$resolved"
    elif resolve_symbol "$citation"; then
        printf 'OK       %-58s → symbol found in source\n' "$citation"
    else
        printf 'MISSING  %-58s → symbol not found in source\n' "$citation"
        missing=$((missing + 1))
    fi
done < "${1:-/dev/stdin}"

echo
if [ "$missing" -gt 0 ]; then
    echo "$missing of $checked citations do not resolve."
    exit 1
fi

echo "All $checked citations resolve."
