#!/bin/sh
# shellcheck disable=SC2039
#
# The controller of the cache of the StackGres images repository (the docir REST API).
#
# It runs next to the nginx that serves the cache (see docir-cache-configmap.yaml) and, on every
# reconciliation cycle:
#
# * refreshes, when the refresh interval expired, the whole catalog of the repository (platforms,
#   tshirt sizes, flavors, base images, versions of every flavor, extensions of every version and
#   addons) storing every response under `repository/api/stackgres/v1/<request URI>` so that nginx
#   serves it to the operator (that issues the same request URIs);
# * preloads the images configured in `DOCIR_CACHE_PRELOADED_IMAGES` (using the syntax of the cache
#   key of the operator, see DocirMetadataManager.getImagesMetadata) and the images requested by
#   the operator found in the nginx access log, resolving each of them with the `image-url`
#   endpoint of the repository and storing the response under
#   `repository/api/stackgres/v1/image-url/<md5 of the key>` so that nginx serves it to the POST
#   requests of the operator.
#
# The images themselves are not cached: they are pulled by the container runtime of the nodes
# directly from the repository (that already caches them locally).
#
# Usage: docir-cache-conciliator.sh run      (the reconciliation loop)
#        docir-cache-conciliator.sh preload  (a single cycle, used to build the offline image)

set -e

{ [ "$DOCIR_CACHE_LOG_LEVEL" != DEBUG ] && [ "$DOCIR_CACHE_LOG_LEVEL" != TRACE ]; } || set -x

DOCIR_REPOSITORY_URL="${DOCIR_REPOSITORY_URL:-https://sgcr.dev}"
DOCIR_API_PATH="${DOCIR_API_PATH:-/api/stackgres/v1}"
DOCIR_USE_PUBLISHED_IMAGES="${DOCIR_USE_PUBLISHED_IMAGES:-true}"
DOCIR_CACHE_REFRESH_INTERVAL="${DOCIR_CACHE_REFRESH_INTERVAL:-PT1H}"
DOCIR_CACHE_PRELOADED_IMAGES="${DOCIR_CACHE_PRELOADED_IMAGES:-[]}"
DOCIR_CACHE_TSHIRT_SIZE="${DOCIR_CACHE_TSHIRT_SIZE:-full}"
DOCIR_CACHE_FLAVORS="${DOCIR_CACHE_FLAVORS:-postgres babelfishpg}"
NGINX_ACCESS_LOG="${NGINX_ACCESS_LOG:-/var/log/nginx/access.log}"
REPOSITORY_PATH="repository$DOCIR_API_PATH"
IMAGE_URL_PATH="$REPOSITORY_PATH/image-url"
STATE_PATH=state

run() {
  [ -n "$DOCIR_OPERATOR_VERSION" ] || {
    echo "DOCIR_OPERATOR_VERSION (the <major>.<minor> version of the operator) must be set" >&2
    return 1
  }
  mkdir -p "$REPOSITORY_PATH" "$IMAGE_URL_PATH" "$STATE_PATH"
  while true
  do
    set +e
    (
    set -e
    reconcile
    )
    EXIT_CODE="$?"
    set -e
    if [ "$EXIT_CODE" = 0 ]
    then
      touch /tmp/docir-cache-ready
      echo
      echo "...wait for next reconciliation cycle"
      echo
      sleep 10
    else
      echo
      echo "...an error occurred during reconciliation cycle, retrying in 10 seconds"
      echo
      sleep 10
    fi
  done
}

preload() {
  [ -n "$DOCIR_OPERATOR_VERSION" ] || {
    echo "DOCIR_OPERATOR_VERSION (the <major>.<minor> version of the operator) must be set" >&2
    return 1
  }
  mkdir -p "$REPOSITORY_PATH" "$IMAGE_URL_PATH" "$STATE_PATH"
  reconcile
}

reconcile() {
  if [ "$OFFLINE" = true ]
  then
    echo "Offline mode: serving the stored catalog and images only"
    return
  fi
  if is_catalog_expired
  then
    echo "Updating catalog..."
    refresh_catalog
    date +%s > "$STATE_PATH/catalog.timestamp"
    echo "done"
    echo
  fi
  echo "Preloading images..."
  {
    printf '%s' "$DOCIR_CACHE_PRELOADED_IMAGES" | jq -r '.[]'
    get_requested_images
  } | sort | uniq | { grep -v '^$' || true; } \
    | while read -r KEY
      do
        try_function preload_image "$KEY"
        if ! "$RESULT"
        then
          echo "Warning: error while trying to preload image $KEY" >&2
        fi
      done
  echo "done"
  echo
  touch /tmp/docir-cache-ready
}

# The query parameters the operator adds to every catalog request
catalog_query() {
  printf 'operator-version=%s' "$DOCIR_OPERATOR_VERSION"
  if [ "$DOCIR_USE_PUBLISHED_IMAGES" != false ]
  then
    printf '&published=true'
  fi
}

repository_base_url() {
  printf '%s' "${DOCIR_REPOSITORY_URL%%\?*}" | sed 's#/\+$##'
}

remote_url() {
  printf '%s%s/%s' "$(repository_base_url)" "$DOCIR_API_PATH" "$1"
}

curl_extra_opts() {
  local PROXY_URL SKIP_HOSTNAME_VERIFICATION
  if is_proxied_repository_url "$DOCIR_REPOSITORY_URL"
  then
    PROXY_URL="$(get_proxy_from_url "$DOCIR_REPOSITORY_URL")"
    printf ' --proxy %s' "$PROXY_URL"
  fi
  if is_skip_hostname_verification_repository_url "$DOCIR_REPOSITORY_URL"
  then
    SKIP_HOSTNAME_VERIFICATION="$(get_skip_hostname_verification_from_url "$DOCIR_REPOSITORY_URL")"
    if [ "$SKIP_HOSTNAME_VERIFICATION" = true ]
    then
      printf ' -k'
    fi
  fi
}

# Retrieve a request of the catalog and store it under the request URI
store_request() {
  local REQUEST="$1"
  local FILE="$REPOSITORY_PATH/$REQUEST"
  local TEMP="$FILE.tmp.$$"
  echo " * $(remote_url "$REQUEST")"
  # shellcheck disable=SC2046
  curl -f -s -L $(curl_extra_opts) "$(remote_url "$REQUEST")" > "$TEMP"
  if ! jq -e . "$TEMP" > /dev/null
  then
    rm -f "$TEMP"
    echo "Invalid JSON returned by $(remote_url "$REQUEST")" >&2
    return 1
  fi
  mv "$TEMP" "$FILE"
}

refresh_catalog() {
  local Q FLAVOR FLAVORS VERSION
  Q="$(catalog_query)"
  store_request platforms
  store_request tshirt-sizes
  store_request flavors
  store_request "platforms?$Q"
  store_request "tshirt-sizes?$Q"
  store_request "flavors?$Q"
  store_request "base-images?$Q"
  store_request "addons?$Q"
  FLAVORS="$(
    {
      printf '%s\n' $DOCIR_CACHE_FLAVORS
      jq -r '.flavors[].flavor' "$REPOSITORY_PATH/flavors?$Q"
    } | sort | uniq)"
  for FLAVOR in $FLAVORS
  do
    store_request "versions?$Q&flavor=$FLAVOR&tshirt-size=$DOCIR_CACHE_TSHIRT_SIZE"
    for VERSION in $(jq -r '.versions[].version' \
      "$REPOSITORY_PATH/versions?$Q&flavor=$FLAVOR&tshirt-size=$DOCIR_CACHE_TSHIRT_SIZE" | sort -u)
    do
      store_request "extensions?$Q&flavor=$FLAVOR&major=${VERSION%%.*}&minor=${VERSION#*.}"
    done
  done
}

is_catalog_expired() {
  local INTERVAL
  INTERVAL="$(interval_seconds "$DOCIR_CACHE_REFRESH_INTERVAL")"
  ! test -f "$STATE_PATH/catalog.timestamp" \
    || [ "$(date +%s)" -ge "$(( $(cat "$STATE_PATH/catalog.timestamp") + INTERVAL ))" ]
}

# Convert an ISO-8601 duration (PT1H, PT30M, P1D, PT3600S) or a number of seconds to seconds
interval_seconds() {
  local INTERVAL="$1"
  if printf '%s' "$INTERVAL" | grep -q '^[0-9]\+$'
  then
    printf '%s' "$INTERVAL"
    return
  fi
  python3 -c '
import re, sys
value = sys.argv[1]
match = re.match(r"^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$", value)
if not match or not any(match.groups()):
    sys.exit("Invalid interval " + value)
days, hours, minutes, seconds = (int(group or 0) for group in match.groups())
print(days * 86400 + hours * 3600 + minutes * 60 + seconds)
' "$INTERVAL"
}

# The keys of the images requested by the operator that have not been stored yet (the POST
# requests logged by nginx with the query that describes the layers of the image)
get_requested_images() {
  if ! test -f "$NGINX_ACCESS_LOG"
  then
    return
  fi
  grep -o '"POST '"$DOCIR_API_PATH"'/image-url?[^ "]* HTTP/[0-9.]*" 200 ' "$NGINX_ACCESS_LOG" \
    | sed 's#^"POST '"$DOCIR_API_PATH"'/image-url?\([^ "]*\) HTTP/[0-9.]*" 200 $#\1#' \
    | sort -u \
    | while read -r KEY
      do
        if ! test -f "$IMAGE_URL_PATH/$(image_file "$KEY")"
        then
          printf '%s\n' "$KEY"
        fi
      done
}

# The responses of the image-url endpoint are stored under the MD5 of the key (the query that
# describes the layers of the image) since the key can exceed the maximum length of a file name;
# nginx computes the same MD5 of the query of the POST requests (see docir-cache-configmap.yaml)
image_file() {
  printf '%s' "$1" | md5sum | cut -c1-32
}

# Resolve an image request (normalizing the key: omitted versions and revisions are replaced
# by the latest of the catalog) and store the response
preload_image() {
  local KEY="$1"
  local NORMALIZED_KEY BODY FILE TEMP
  NORMALIZED_KEY="$(image_key_tool normalize "$KEY")"
  FILE="$IMAGE_URL_PATH/$(image_file "$NORMALIZED_KEY")"
  if ! test -f "$FILE"
  then
    echo " * $NORMALIZED_KEY"
    BODY="$(image_key_tool body "$NORMALIZED_KEY")"
    TEMP="$FILE.tmp.$$"
    # shellcheck disable=SC2046
    curl -f -s -L $(curl_extra_opts) -X POST -H 'Content-Type: application/json' \
      -d "$BODY" "$(remote_url "image-url?$NORMALIZED_KEY")" > "$TEMP"
    if ! jq -e '.image.urlDigest | strings' "$TEMP" > /dev/null
    then
      echo "   ! Invalid image returned for $NORMALIZED_KEY: $(head -c 300 "$TEMP")" >&2
      rm -f "$TEMP"
      return 1
    fi
    printf '%s\n' "$NORMALIZED_KEY" > "$FILE.key"
    mv "$TEMP" "$FILE"
  else
    echo " . $NORMALIZED_KEY already stored"
  fi
}

# Tool that reads the stored catalog to normalize an image key (see the syntax in
# DocirMetadataManager.getImageCacheUriBuilder) and to build the request body of the image-url
# endpoint (see DocirMetadataManager.toImageUrlRequest) and the name of the image (see
# DocirImageIndex)
image_key_tool() {
  DOCIR_REPOSITORY_PATH="$REPOSITORY_PATH" DOCIR_CATALOG_QUERY="$(catalog_query)" \
    DOCIR_CACHE_TSHIRT_SIZE="$DOCIR_CACHE_TSHIRT_SIZE" python3 -c '
import json, os, re, sys

repository = os.environ["DOCIR_REPOSITORY_PATH"]
query = os.environ["DOCIR_CATALOG_QUERY"]
tshirt = os.environ["DOCIR_CACHE_TSHIRT_SIZE"]


def load(request):
    with open(os.path.join(repository, request)) as f:
        return json.load(f)


def version_key(version):
    return [int(p) if p.isdigit() else p for p in re.split(r"[.-]", version)]


def max_revision(platforms):
    revisions = [p["revision"] for p in platforms or [] if p.get("revision") is not None]
    if not revisions:
        return None
    return max(revisions, key=lambda r: int(r) if str(r).isdigit() else 0)


def split(value, parts):
    fields = value.split("@")
    return fields + [None] * (parts - len(fields))


def resolve_base(value):
    name, version, revision = split(value, 3)
    bases = [b for b in load("base-images?" + query)["baseImages"] if b["name"] == name]
    if not bases:
        sys.exit("Base image %s not found in the catalog" % name)
    if version is None:
        base = max(bases, key=lambda b: (b["major"], b["minor"]))
        version = "%s.%s" % (base["major"], base["minor"])
    else:
        matching = [b for b in bases if "%s.%s" % (b["major"], b["minor"]) == version]
        if not matching:
            sys.exit("Base image %s version %s not found in the catalog" % (name, version))
        base = matching[0]
    if revision is None:
        revision = max_revision(base.get("platforms"))
    return name, version, revision


def resolve_flavor(value):
    name, version, revision = split(value, 3)
    versions = load("versions?%s&flavor=%s&tshirt-size=%s" % (query, name, tshirt))["versions"]
    if version is None:
        version = max((v["version"] for v in versions), key=version_key)
    matching = [v for v in versions if v["version"] == version]
    if not matching:
        sys.exit("Version %s of flavor %s not found in the catalog" % (version, name))
    if revision is None:
        revision = max_revision([p for v in matching for p in v.get("platforms") or []])
    return name, version, revision


def resolve_extension(flavor, flavor_version, value):
    name, version, revision = split(value, 3)
    extensions = load("extensions?%s&flavor=%s&major=%s&minor=%s" % (
        query, flavor, flavor_version.split(".")[0], flavor_version.split(".")[1]))["extensions"]
    matching = [e for e in extensions if e["name"] == name]
    if not matching:
        sys.exit("Extension %s not found in the catalog for %s %s" % (name, flavor, flavor_version))
    extension_versions = matching[0]["extensionVersions"]
    if version is None:
        version = max((v["version"] for v in extension_versions), key=version_key)
    matching_versions = [v for v in extension_versions if v["version"] == version]
    if not matching_versions:
        sys.exit("Extension %s version %s not found in the catalog for %s %s" % (
            name, version, flavor, flavor_version))
    if revision is None:
        revision = max_revision([p for v in matching_versions for p in v.get("platforms") or []])
    return name, version, revision


def resolve_addon(value):
    name, version, revision = split(value, 3)
    addons = [a for a in load("addons?" + query)["addons"] if a["name"] == name]
    if not addons:
        sys.exit("Addon %s not found in the catalog" % name)
    addon_versions = addons[0]["versions"]
    if version is None:
        version = max((v["version"] for v in addon_versions), key=version_key)
    matching = [v for v in addon_versions if v["version"] == version]
    if not matching:
        sys.exit("Addon %s version %s not found in the catalog" % (name, version))
    if revision is None:
        revision = max_revision([p for v in matching for p in v.get("platforms") or []])
    return name, version, revision


def parse(key):
    params = [p.split("=", 1) for p in key.split("&") if p]
    parsed = {"tshirt-size": tshirt, "partial": False, "base": None, "flavors": [], "addons": []}
    for name, value in params:
        if name == "tshirt-size":
            parsed["tshirt-size"] = value
        elif name == "flavorAndBaseOmmitted":
            parsed["partial"] = value == "true"
        elif name == "base":
            parsed["base"] = value
        elif name == "flavors":
            parsed["flavors"].append({"flavor": value, "extensions": []})
        elif name == "extensions":
            if not parsed["flavors"]:
                sys.exit("extensions must follow a flavors parameter in " + key)
            parsed["flavors"][-1]["extensions"].append(value)
        elif name == "addons":
            parsed["addons"].append(value)
        else:
            sys.exit("Unknown parameter %s in %s" % (name, key))
    if not parsed["flavors"]:
        sys.exit("A flavors parameter is required in " + key)
    return parsed


def resolve(key):
    parsed = parse(key)
    resolved = {"tshirt-size": parsed["tshirt-size"], "partial": parsed["partial"]}
    resolved["base"] = None if parsed["partial"] else resolve_base(parsed["base"] or "debian")
    resolved["flavors"] = []
    for entry in parsed["flavors"]:
        name, version, revision = resolve_flavor(entry["flavor"])
        resolved["flavors"].append({
            "name": name, "version": version, "revision": revision,
            "extensions": [resolve_extension(name, version, e) for e in entry["extensions"]]})
    resolved["addons"] = [resolve_addon(a) for a in parsed["addons"]]
    return resolved


def join(name, version, revision):
    return "%s@%s@%s" % (name, version, revision)


def normalize(resolved):
    parts = ["tshirt-size=" + resolved["tshirt-size"]]
    if resolved["partial"]:
        parts.append("flavorAndBaseOmmitted=true")
    else:
        parts.append("base=" + join(*resolved["base"]))
    for flavor in resolved["flavors"]:
        parts.append("flavors=" + join(flavor["name"], flavor["version"], flavor["revision"]))
        parts.extend("extensions=" + join(*e) for e in flavor["extensions"])
    parts.extend("addons=" + join(*a) for a in resolved["addons"])
    return "&".join(parts)


def image_name(resolved):
    def sanitize(*tokens):
        return "-".join(re.sub(r"[^a-z0-9._-]", "-", str(t).lower()) for t in tokens)
    flavor = resolved["flavors"][0]
    addons = {a[0]: a for a in resolved["addons"]}
    if resolved["partial"] and flavor["extensions"]:
        extension = flavor["extensions"][0]
        return sanitize(flavor["name"], flavor["version"], extension[0], extension[1], extension[2])
    if "patroni" in addons:
        patroni = addons["patroni"]
        if len(resolved["flavors"]) > 1:
            return sanitize("patroni", patroni[1], patroni[2], flavor["name"],
                            resolved["flavors"][1]["version"], "to", flavor["version"])
        return sanitize("patroni", patroni[1], patroni[2], flavor["name"], flavor["version"])
    if len(resolved["addons"]) == 1:
        return sanitize(*resolved["addons"][0])
    return sanitize(flavor["name"], flavor["version"])


def body(resolved):
    revisions = [r for r in [resolved["base"][2] if resolved["base"] else None]
                 + [f["revision"] for f in resolved["flavors"]]
                 + [e[2] for f in resolved["flavors"] for e in f["extensions"]]
                 + [a[2] for a in resolved["addons"]] if r is not None]
    upper_bound = max(revisions, key=lambda r: int(r) if str(r).isdigit() else 0) if revisions else None
    base = None
    if resolved["base"]:
        name, version, revision = resolved["base"]
        base = {"name": name, "majorVersion": version.split(".")[0],
                "minorVersion": version.split(".")[1], "revision": revision}
    return json.dumps({
        "name": image_name(resolved),
        "tshirtSize": resolved["tshirt-size"],
        "flavorAndBaseOmmitted": resolved["partial"],
        "revision": upper_bound,
        "base": base,
        "flavor": {
            "name": resolved["flavors"][0]["name"],
            "versions": [{
                "majorVersion": f["version"].split(".")[0],
                "minorVersion": f["version"].split(".")[1],
                "revision": f["revision"],
                "extensions": [{"name": e[0], "version": e[1], "revision": e[2]}
                               for e in f["extensions"]],
            } for f in resolved["flavors"]],
        },
        "addons": [{"name": a[0], "version": a[1], "revision": a[2]} for a in resolved["addons"]],
    }, separators=(",", ":"))


command, key = sys.argv[1], sys.argv[2]
resolved = resolve(key)
if command == "normalize":
    print(normalize(resolved))
elif command == "body":
    print(body(resolved))
elif command == "name":
    print(image_name(resolved))
else:
    sys.exit("Unknown command " + command)
' "$@"
}

is_proxied_repository_url() {
  printf '%s' "$1" | grep -q "[?&]proxyUrl="
}

is_skip_hostname_verification_repository_url() {
  printf '%s' "$1" | grep -q "[?&]skipHostnameVerification="
}

get_proxy_from_url() {
  printf '%s' "$1" | sed 's/^.*[?&]proxyUrl=\([^?&]\+\)\([?&].*\)\?$/\1/' | urldecode
}

get_skip_hostname_verification_from_url() {
  printf '%s' "$1" | sed 's/^.*[?&]skipHostnameVerification=\([^?&]\+\)\([?&].*\)\?$/\1/' | urldecode
}

urldecode() {
  python3 -c 'import sys, urllib.parse; sys.stdout.write(urllib.parse.unquote_plus(sys.stdin.read()))'
}

try_function() {
  local E_UNSET=true
  if echo "$-" | grep -q e
  then
    E_UNSET=false
  fi
  "$E_UNSET" || set +e
  (set -e; "$@")
  EXIT_CODE="$?"
  "$E_UNSET" || set -e
  RESULT=false
  if [ "$EXIT_CODE" = 0 ]
  then
    RESULT=true
  fi
}

if [ -n "$1" ]
then
  "$@"
fi
