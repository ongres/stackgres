#!/bin/sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="../../$SCRIPT_DIR"
GLAB="${GLAB:-$PROJECT_DIR/stackgres-k8s/ci/utils/glabw}"
TARGET_DIR="$SCRIPT_DIR/target"
STATS_FILE="$SCRIPT_DIR/test.stats"
ARTIFACT_ZIP="/tmp/job_artifact_$$.zip"
COLLECTED_DURATIONS="/tmp/collected_durations_$$.txt"
TEMP_STATS="/tmp/temp_stats_$$.txt"

usage() {
  echo "Usage: $0 <pipeline_id>"
  echo ""
  echo "Updates test.stats file with test durations from CI/CD pipeline artifacts."
  echo ""
  echo "Arguments:"
  echo "  pipeline_id    GitLab pipeline ID to fetch artifacts from"
  echo ""
  echo "Requirements:"
  echo "  - Docker installed"
  exit 1
}

# Get duration for a test from collected durations file
get_collected_duration() {
  local test_name="$1"
  grep "^${test_name}:" "$COLLECTED_DURATIONS" 2>/dev/null | cut -d: -f2 | head -1
}

# Check if test exists in collected durations
has_collected_duration() {
  local test_name="$1"
  grep -q "^${test_name}:" "$COLLECTED_DURATIONS" 2>/dev/null
}

# Get duration for a test from stats file
get_stat_duration() {
  local test_name="$1"
  local file="$2"
  grep "^${test_name}:" "$file" 2>/dev/null | cut -d: -f2 | head -1
}

# Clean up target directory
clean_target() {
  rm -rf "$TARGET_DIR"
}

# Cleanup on exit
cleanup() {
  rm -f "$ARTIFACT_ZIP" "$COLLECTED_DURATIONS" "$TEMP_STATS"
}
trap cleanup EXIT

if [ -z "$1" ]; then
  usage
fi

PIPELINE_ID="$1"

if ! "$GLAB" auth status > /dev/null 2>&1; then
  echo "Error: glab is not authenticated. Run '$GLAB auth login' first."
  exit 1
fi

# Initialize collected durations file
: > "$COLLECTED_DURATIONS"

echo "Fetching e2e job IDs from pipeline $PIPELINE_ID..."

# Get all e2e job IDs including retried ones
e2e_jobs=$("$GLAB" api "projects/:id/pipelines/${PIPELINE_ID}/jobs?per_page=100&include_retried=true" 2>/dev/null \
  | jq -r '.[] | select(.name | contains("e2e")) | .id')

if [ -z "$e2e_jobs" ]; then
  echo "Error: No e2e jobs found in pipeline $PIPELINE_ID"
  exit 1
fi

total_jobs=$(echo "$e2e_jobs" | wc -l | tr -d ' ')
echo "Found $total_jobs e2e jobs"
echo ""

current=0
for job_id in $e2e_jobs; do
  current=$((current + 1))
  echo "[$current/$total_jobs] Processing job ID: $job_id"

  # Clean target directory
  clean_target

  # Download artifacts
  rm -f "$ARTIFACT_ZIP"
  if ! "$GLAB" api "projects/:id/jobs/${job_id}/artifacts" 2>/dev/null > "$ARTIFACT_ZIP"; then
    echo "  Failed to download artifacts"
    continue
  fi

  # Check if it's a valid zip file
  if ! file "$ARTIFACT_ZIP" | grep -q "Zip archive"; then
    echo "  No valid artifacts"
    continue
  fi

  # Extract artifacts
  if ! unzip -o "$ARTIFACT_ZIP" -d "$SCRIPT_DIR/../.." > /dev/null 2>&1; then
    echo "  Failed to extract artifacts"
    continue
  fi

  # Find duration files with matching success files
  found=0
  for duration_file in "$TARGET_DIR"/*.duration; do
    if [ -f "$duration_file" ]; then
      test_name=$(basename "$duration_file" .duration)
      success_file="$TARGET_DIR/${test_name}.success"

      if [ -f "$success_file" ]; then
        duration=$(tr -d '[:space:]' < "$duration_file")
        if [ -n "$duration" ]; then
          # Only update if not already set (first match wins - latest run)
          if ! has_collected_duration "$test_name"; then
            echo "${test_name}:${duration}" >> "$COLLECTED_DURATIONS"
            echo "  Found: $test_name -> $duration"
            found=$((found + 1))
          fi
        fi
      fi
    fi
  done
  echo "  Total found in this job: $found"
done

# Clean up target directory
clean_target

echo ""
echo "=== Updating $STATS_FILE ==="

# Copy existing stats to temp file, or create empty
if [ -f "$STATS_FILE" ]; then
  cp "$STATS_FILE" "$TEMP_STATS"
else
  : > "$TEMP_STATS"
fi

existing_count=$(wc -l < "$TEMP_STATS" | tr -d ' ')
echo "Existing entries: $existing_count"

# Update stats with collected durations
updated=0
added=0
collected_count=$(wc -l < "$COLLECTED_DURATIONS" | tr -d ' ')

while IFS=: read -r name duration; do
  if [ -n "$name" ]; then
    old_duration=$(get_stat_duration "$name" "$TEMP_STATS")
    if [ -z "$old_duration" ]; then
      echo "  Added: $name -> $duration"
      echo "${name}:${duration}" >> "$TEMP_STATS"
      added=$((added + 1))
    elif [ "$old_duration" != "$duration" ]; then
      echo "  Updated: $name: $old_duration -> $duration"
      sed -i "s/^${name}:.*/${name}:${duration}/" "$TEMP_STATS"
      updated=$((updated + 1))
    fi
  fi
done < "$COLLECTED_DURATIONS"

# Write sorted output to test.stats
sort "$TEMP_STATS" > "$STATS_FILE"

final_count=$(wc -l < "$STATS_FILE" | tr -d ' ')

echo ""
echo "Summary:"
echo "  Tests collected from pipeline: $collected_count"
echo "  New tests added: $added"
echo "  Tests updated: $updated"
echo "  Total entries in test.stats: $final_count"
