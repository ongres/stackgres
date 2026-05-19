"""Macro: vector_binary(name, features) — cargo-zigbuild a curated Vector.

Each call generates a genrule that stages @vector source into a writable temp
dir, runs `cargo zigbuild --release` with the given feature set, and emits
the resulting binary as <name>_bin. The host must have `cargo` on PATH (the
toolchain is taken from @vector's rust-toolchain.toml; rustup will install
1.92 on first run if missing). zig + cargo-zigbuild + protoc are supplied
hermetically via Bazel.

The WORK dir is namespaced by package + target name so multiple invocations
of the macro within the same workspace don't collide on /tmp.
"""

# Template containing %{name} and %{features} placeholders. Replace via
# .replace() rather than str.format() to avoid clashing with literal `{` /
# `}` if any ever land in the shell body.
_CMD_TEMPLATE = """
set -euo pipefail

# Capture absolute paths up front; we cd into a writable temp dir below.
START_DIR="$$PWD"
PROTOC_BIN="$$(readlink -f "$(execpath @protobuf//:protoc)")"
ZIG_BIN="$$(readlink -f "$(execpath @zig//:zig)")"
ZIG_DIR="$$(dirname "$$ZIG_BIN")"
CARGO_ZIGBUILD_BIN="$$(readlink -f "$(execpath @cargo_zigbuild//:cargo-zigbuild)")"
CARGO_ZIGBUILD_DIR="$$(dirname "$$CARGO_ZIGBUILD_BIN")"
OUT_REL="$@"

# Locate @vector workspace root: the top-level Cargo.toml at @vector's root.
# Bazel's external-repo path numbering varies; match on the unique suffix.
SRCROOT=""
for f in $(SRCS); do
    case "$$f" in
        *+vector/Cargo.toml)
            SRCROOT="$$(readlink -f "$$(dirname "$$f")")"
            break
            ;;
    esac
done
if [ -z "$$SRCROOT" ]; then
    echo "ERROR: could not find @vector//:Cargo.toml among genrule inputs" >&2
    exit 1
fi

if ! command -v cargo >/dev/null 2>&1; then
    echo "ERROR: cargo not on PATH. Install rustup (https://rustup.rs) or your" >&2
    echo "       distro's rust-toolchain package. Vector's rust-toolchain.toml" >&2
    echo "       requests Rust 1.92." >&2
    exit 1
fi

# Stable, name-namespaced build dir. Stable = reproducibility (openssl-src
# bakes its install prefix into the static archive; rustc's
# --remap-path-prefix doesn't reach C-archive contents). Name-namespaced =
# concurrent invocations of the macro from different targets don't collide.
WORK="/tmp/vector-build-bazel-%{work_suffix}"
rm -rf "$$WORK"
mkdir -p "$$WORK"
trap 'rm -rf "$$WORK"' EXIT

cp -a "$$SRCROOT/." "$$WORK/"
cd "$$WORK"

export CARGO_HOME="$$WORK/.cargo_home"
export CARGO_TARGET_DIR="$$WORK/target"
export SOURCE_DATE_EPOCH=315532800
export PROTOC="$$PROTOC_BIN"
# Belt-and-suspenders alongside the stable WORK name: remap rustc-emitted
# paths (panic locations, track_caller strings that survive strip=symbols).
export RUSTFLAGS="--remap-path-prefix=$$WORK=/build"
export PATH="$$ZIG_DIR:$$CARGO_ZIGBUILD_DIR:$$PATH"

# Production release profile per doc/INVESTIGATION.md §7.
# --target ...gnu.2.36 matches gcr.io/distroless/cc-debian12 (the runtime
# image). Picking the runtime's glibc as the ceiling keeps the build host
# irrelevant for reproducibility.
cargo zigbuild --release \\
    --target x86_64-unknown-linux-gnu.2.36 \\
    --no-default-features \\
    --features '%{features}' \\
    --bin vector \\
    --config 'profile.release.opt-level=3' \\
    --config 'profile.release.lto="fat"' \\
    --config 'profile.release.codegen-units=1' \\
    --config 'profile.release.panic="abort"' \\
    --config 'profile.release.strip="symbols"'

cp "target/x86_64-unknown-linux-gnu/release/vector" "$$START_DIR/$$OUT_REL"
"""

def vector_binary(name, features, visibility = None):
    """Builds a curated Vector binary via cargo-zigbuild.

    Args:
        name: Target name. Output is `<name>_bin`. Also used to namespace the
            cargo work directory under /tmp/vector-build-bazel-<name>.
        features: Comma-separated cargo features for `--features`. Default
            features are NOT enabled (`--no-default-features`).
        visibility: Bazel visibility list.
    """
    # WORK dir suffix is package_name + target name; the package qualifier
    # disambiguates when multiple packages declare a target named "vector".
    pkg = native.package_name().replace("/", "_") or "root"
    work_suffix = "{}_{}".format(pkg, name)
    cmd = _CMD_TEMPLATE.replace("%{work_suffix}", work_suffix).replace("%{features}", features)
    native.genrule(
        name = name,
        srcs = ["@vector//:srcs"],
        outs = [name + "_bin"],
        cmd = cmd,
        tools = [
            "@protobuf//:protoc",
            "@zig//:zig",
            "@zig//:files",  # zig needs lib/ siblings to the binary
            "@cargo_zigbuild//:cargo-zigbuild",
        ],
        # cargo needs network for crates.io fetches and modifies CARGO_HOME.
        local = 1,
        tags = ["requires-network"],
        visibility = visibility,
    )
