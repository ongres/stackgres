#!/usr/bin/env bash
#
# End-to-end functional test for the curated Vector image.
#
#  1. Spins up an OpenTelemetry Collector (debug exporter) in a private
#     docker network.
#  2. Runs the curated `vector-agent:dev` image in the same network with
#     agent/pipeline.yaml.template mounted at /etc/vector/vector.yaml.
#     The template is patched at runtime to read fixtures from the start;
#     the ${INSTANCE_UUID} and ${OTLP_ENDPOINT} placeholders are filled
#     in by Vector's env-var interpolation (passed via `docker run -e`).
#  3. Feeds verify/fixtures/postgresql-test.csv into Vector's file source.
#  4. Captures Vector's console output (the human-readable text sink) and
#     the collector's stdout (the OTLP debug exporter's output).
#  5. Asserts on both: console contains one record per fixture line in the
#     Postgres-stderr-like format; collector received OTLP logs with the
#     expected body and severity mapping.
#
# Requirements: docker, the agent image already loaded
# (`bazel run //agent:image_load`).
#
# Run from anywhere; cd's to the build root automatically.

set -euo pipefail

cd "$(dirname "$0")/.."

# --- Setup ---
WORK=$(mktemp -d -t vector-e2e.XXXXXX)
NETWORK="vector-e2e-net-$$"
COLLECTOR_NAME="otel-collector-e2e-$$"
VECTOR_NAME="vector-e2e-$$"
VECTOR_IMAGE="${VECTOR_IMAGE:-vector-agent:dev}"
COLLECTOR_IMAGE="${COLLECTOR_IMAGE:-otel/opentelemetry-collector-contrib:0.118.0}"

cleanup() {
    docker rm -f "$VECTOR_NAME" "$COLLECTOR_NAME" >/dev/null 2>&1 || true
    docker network rm "$NETWORK" >/dev/null 2>&1 || true
    # Vector's nonroot uid (65532) wrote checkpoints under $WORK/data with
    # ownership the host user can't unlink. Silence the rm error; /tmp gets
    # cleaned by the system tmpwatcher anyway.
    rm -rf "$WORK" 2>/dev/null || true
}
trap cleanup EXIT

mkdir -p "$WORK/pglogs" "$WORK/data" "$WORK/config"

# Vector runs as nonroot (uid 65532) inside the image and writes checkpoints
# to /var/lib/vector (the data_dir). The bind-mounted host dir is owned by
# the current user, so we open it world-writable for the test.
chmod 0777 "$WORK/data"

# Stage the fixture: must match Vector's source file glob
# (`/var/log/postgresql/postgresql-*.csv`).
cp verify/fixtures/postgresql-test.csv "$WORK/pglogs/postgresql-test.csv"

# Stage the collector config.
cp verify/fixtures/collector-config.yaml "$WORK/config/collector.yaml"

# Stage Vector's config: take agent/pipeline.yaml.template verbatim,
# except read_from is flipped (fixture files exist before Vector starts).
# ${INSTANCE_UUID} and ${OTLP_ENDPOINT} placeholders are passed to
# Vector as env vars on the docker run below — Vector interpolates at
# config load time.
sed -e 's|read_from: end|read_from: beginning|' \
    agent/pipeline.yaml.template > "$WORK/config/vector.yaml"

INSTANCE_UUID="00000000-0000-0000-0000-000000000001"
OTLP_ENDPOINT="http://${COLLECTOR_NAME}:4318/v1/logs"

# --- Run ---
echo "[setup] private docker network: $NETWORK"
docker network create "$NETWORK" >/dev/null

echo "[setup] starting OTel collector ($COLLECTOR_IMAGE)"
docker run -d --rm \
    --name "$COLLECTOR_NAME" \
    --network "$NETWORK" \
    -v "$WORK/config/collector.yaml:/etc/otel/config.yaml:ro" \
    "$COLLECTOR_IMAGE" \
    --config=/etc/otel/config.yaml >/dev/null

# Give the collector a beat to bind its listener. Watch its own log line.
for i in $(seq 1 15); do
    if docker logs "$COLLECTOR_NAME" 2>&1 | grep -q "Everything is ready"; then
        break
    fi
    sleep 1
done

echo "[setup] starting Vector ($VECTOR_IMAGE)"
docker run -d --rm \
    --name "$VECTOR_NAME" \
    --network "$NETWORK" \
    -e "INSTANCE_UUID=${INSTANCE_UUID}" \
    -e "OTLP_ENDPOINT=${OTLP_ENDPOINT}" \
    -v "$WORK/config/vector.yaml:/etc/vector/vector.yaml:ro" \
    -v "$WORK/pglogs:/var/log/postgresql:ro" \
    -v "$WORK/data:/var/lib/vector" \
    "$VECTOR_IMAGE" >/dev/null

# Vector tails the file, parses, and ships. Default batch timeout is 1s but
# OTLP sink has its own buffering; 8s is generous for 3 records to flush.
echo "[wait] giving Vector 8s to process and ship the fixture"
sleep 8

# --- Capture ---
docker logs "$VECTOR_NAME" >"$WORK/vector.stdout" 2>&1 || true
docker logs "$COLLECTOR_NAME" >"$WORK/collector.stdout" 2>&1 || true

echo
echo "================ Vector console output ================"
cat "$WORK/vector.stdout"
echo
echo "================ Collector received ================"
tail -80 "$WORK/collector.stdout"
echo

# --- Assert ---
fails=0
fail() { echo "FAIL: $*"; fails=$((fails + 1)); }

fixture_lines=$(grep -c '^' verify/fixtures/postgresql-test.csv)

# 1. Console sink emitted the Postgres-stderr-like one-liner for each fixture
#    line. Format from doc/vector-csv-otlp.yaml's pg_parse:
#      "<log_time> [<pid>] <severity>: <pg_message>"
console_records=$(grep -cE '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+ \w+ \[[0-9]+\] \w+: ' "$WORK/vector.stdout" || true)
if [[ "$console_records" -lt "$fixture_lines" ]]; then
    fail "console sink emitted $console_records records, expected $fixture_lines"
fi

# 2. Collector received OTLP logs (debug exporter prints "ResourceLog" or
#    "resource_logs" depending on collector version).
if ! grep -qE "ResourceLog|resource_logs" "$WORK/collector.stdout"; then
    fail "collector did not receive any OTLP logs"
fi

# 3. Severity mapping (LOG -> 9 INFO, ERROR -> 17, WARNING -> 13). The
#    debug exporter prints e.g. "SeverityNumber: Error(17)".
for sev_pattern in 'Info(9)' 'Error(17)' 'Warn(13)'; do
    if ! grep -qF "SeverityNumber: $sev_pattern" "$WORK/collector.stdout"; then
        fail "severity '$sev_pattern' not received by collector"
    fi
done

# 4. db.system attribute set to postgresql (from pg_to_otlp transform).
if ! grep -qE "db\.system: Str\(postgresql\)" "$WORK/collector.stdout"; then
    fail "db.system=postgresql resource attribute not present"
fi

# 5. db.statement on the ERROR record made it through.
if ! grep -qE 'db\.statement: Str\(SELECT \* FROM nonexistent\)' "$WORK/collector.stdout"; then
    fail "db.statement attribute on ERROR record not present"
fi

# 6. db.backend_type correctly parsed as the fixture's recorded value —
#    guards against column-shift bugs in the fixture that would silently
#    land bogus values in this attribute.
if ! grep -qE 'db\.backend_type: Str\(checkpointer\)' "$WORK/collector.stdout"; then
    fail "db.backend_type attribute should be 'checkpointer' on row 0 (column-shift in fixture?)"
fi

# 7. instance.uuid resource attribute present. The agent's
#    pipeline.yaml.template has the slon-substituted ${INSTANCE_UUID}
#    placeholder; this test sets it to the all-ones-1 fixture UUID via
#    the agent container's env (Vector interpolates at config load).
if ! grep -qE 'instance\.uuid: Str\(00000000-0000-0000-0000-000000000001\)' "$WORK/collector.stdout"; then
    fail "instance.uuid resource attribute (placeholder) not present at collector"
fi

# 8. Newly-added attributes from the all-26-fields expansion. Verify a
#    representative `db.*` and a representative `pg.*` arrived.
if ! grep -qE 'db\.application: Str\(psql\)' "$WORK/collector.stdout"; then
    fail "db.application attribute not present (expected 'psql')"
fi
if ! grep -qE 'pg\.leader_pid: Str\(1234\)' "$WORK/collector.stdout"; then
    fail "pg.leader_pid attribute not present (expected '1234')"
fi
if ! grep -qE 'pg\.session_line_num:' "$WORK/collector.stdout"; then
    fail "pg.session_line_num attribute not present"
fi

if [[ $fails -gt 0 ]]; then
    echo
    echo "E2E TEST FAILED ($fails check(s))"
    exit 1
fi

echo "E2E TEST PASSED"
