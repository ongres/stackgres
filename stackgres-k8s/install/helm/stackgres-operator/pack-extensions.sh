#!/bin/sh

set -ex

merge_extensions() {
  local INDEX="$1"
  cp extensions.json extensions.json.tmp
  jq -s "$(cat << 'EOF'
    (.[0].extensions | map({key: .name, value: .})
      | map(.value.versions = (.value.versions
        | map({key: .version, value: (. 
          | .availableFor = (.availableFor | map({key: (.postgresVersion + (if .build == null then "" else "-build-" + .build end)) ,value: .})
          | from_entries))})
        | from_entries))
      | from_entries) as $l | .[1] as $r | $l * $r
EOF
    )" "$INDEX" extensions.json.tmp > extensions.json
  rm extensions.json.tmp
}

cd /var/lib/postgresql/extensions
rm -rf .keys *.tar *.tgz *.sha256
mkdir .keys
chmod 700 .keys
openssl genrsa -out .keys/private.pem 4096
cat << EOF > publishers.json
{
  "publishers": [{
    "name": "OnGres",
    "id": "com.ongres",
    "url": "https://ongres.com",
    "email": "stackgres@ongres.com",
    "publicKey": $(openssl rsa -pubout -in .keys/private.pem | jq -s -R .)
  }]
}
EOF

cat << EOF > extensions.json
{}
EOF

INDEXES="$(ls -1 index-*-build-*.json)"
for INDEX in $INDEXES
do
  merge_extensions "$INDEX"
done

jq -s "$(
  cat << 'EOF'
  .[0] as $e | .[1] | .extensions = ($e | to_entries
    | map(.value 
      | .versions = (.versions
        | to_entries | map (.value
          | .availableFor = (.availableFor | to_entries | map(.value))))))
EOF
)" extensions.json publishers.json > index.json

mkdir -p com.ongres/x86_64/linux
BUILD_VERSIONS="$(jq -r "$(
  cat << 'EOF'
  .extensions[] | . as $e | .versions[0].availableFor[]
    | (if .build != null then .build else null end)
EOF
)" index.json | sort | uniq | grep -v null)"

jq -r "$(
  cat << 'EOF'
  .extensions[] | . as $e | .versions[0].availableFor[]
    | $e.name + "-" + $e.versions[0].version + "-pg" + .postgresVersion
       + (if .build != null then "-build-" + .build else "" end)
EOF
)" index.json \
  | xargs -r -n 1 -I % sh -exc "$(
    echo "BUILD_VERSIONS='$BUILD_VERSIONS'"
    cat << 'EOF'
EXTENSION_FILE='%'
EXTENSION_NAME="$(echo "$EXTENSION_FILE" | sed 's/^\(.\+\)-[^-]\+-pg[^-]\+\(-build-.\+\)\?$/\1/')"
POSTGRES_VERSION="$(echo "$EXTENSION_FILE" | sed 's/^.\+-[^-]\+-pg\([^-]\+\)\(-build-.\+\)\?$/\1/')"
BUILD_VERSION="$(echo "$EXTENSION_FILE" | sed 's/^.\+-[^-]\+-pg[^-]\+\(-build-\([^.-]\+\).*\)\?$/\2/')"
if [ -n "$BUILD_VERSION" ]
then
  CURRENT_BUILD_VERSIONS="$BUILD_VERSION"
  NO_BUILD_VERSION=false
else
  CURRENT_BUILD_VERSIONS="$BUILD_VERSIONS"
  NO_BUILD_VERSION=true
fi
for BUILD_VERSION in $CURRENT_BUILD_VERSIONS
do
  if "$NO_BUILD_VERSION" && [ ! -d "$POSTGRES_VERSION/$BUILD_VERSION" ]
  then
    continue
  fi
  tar czf "com.ongres/x86_64/linux/$EXTENSION_FILE.tgz" -C "$POSTGRES_VERSION/$BUILD_VERSION/$EXTENSION_NAME/" .
  openssl dgst -sha256 -sign .keys/private.pem -out "com.ongres/x86_64/linux/$EXTENSION_FILE.sha256" \
    "com.ongres/x86_64/linux/$EXTENSION_FILE.tgz"
  tar cf "com.ongres/x86_64/linux/$EXTENSION_FILE.tar" -C com.ongres/x86_64/linux/ \
    "$EXTENSION_FILE.sha256" "$EXTENSION_FILE.tgz"
  if [ "$EXTENSION_NAME" = "pageinspect" ]
  then
    EXTENSION_FILE="$(echo "$EXTENSION_FILE" | sed 's/-1\.7-/-1.8-/')"
    sed -i "s/'1\.7'/'1.8'/" "$POSTGRES_VERSION/$BUILD_VERSION/$EXTENSION_NAME/usr/share/postgresql/$POSTGRES_VERSION/extension/pageinspect.control"
    cat << 'SCRIPT_EOF' > "$POSTGRES_VERSION/$BUILD_VERSION/$EXTENSION_NAME/usr/share/postgresql/$POSTGRES_VERSION/extension/pageinspect--1.7--1.8.sql"
/* contrib/pageinspect/pageinspect--1.7--1.8.sql */

-- complain if script is sourced in psql, rather than via ALTER EXTENSION
\echo Use "ALTER EXTENSION pageinspect UPDATE TO '1.8'" to load this file. \quit
SCRIPT_EOF
    tar czf "com.ongres/x86_64/linux/$EXTENSION_FILE.tgz" -C "$POSTGRES_VERSION/$BUILD_VERSION/$EXTENSION_NAME/" .
    openssl dgst -sha256 -sign .keys/private.pem -out "com.ongres/x86_64/linux/$EXTENSION_FILE.sha256" \
      "com.ongres/x86_64/linux/$EXTENSION_FILE.tgz"
    tar cf "com.ongres/x86_64/linux/$EXTENSION_FILE.tar" -C com.ongres/x86_64/linux/ \
      "$EXTENSION_FILE.sha256" "$EXTENSION_FILE.tgz"
  fi
  if [ "$EXTENSION_NAME" = "intagg" ]
  then
    EXTENSION_FILE="$(echo "$EXTENSION_FILE" | sed 's/-1\.1-/-1.2-/')"
    sed -i "s/'1\.1'/'1.2'/" "$POSTGRES_VERSION/$BUILD_VERSION/$EXTENSION_NAME/usr/share/postgresql/$POSTGRES_VERSION/extension/intagg.control"
    cat << 'SCRIPT_EOF' > "$POSTGRES_VERSION/$BUILD_VERSION/$EXTENSION_NAME/usr/share/postgresql/$POSTGRES_VERSION/extension/intagg--1.1--1.2.sql"
/* contrib/intagg/intagg--1.1--1.2.sql */

-- complain if script is sourced in psql, rather than via ALTER EXTENSION
\echo Use "ALTER EXTENSION intagg UPDATE TO '1.2'" to load this file. \quit
SCRIPT_EOF
    tar czf "com.ongres/x86_64/linux/$EXTENSION_FILE.tgz" -C "$POSTGRES_VERSION/$BUILD_VERSION/$EXTENSION_NAME/" .
    openssl dgst -sha256 -sign .keys/private.pem -out "com.ongres/x86_64/linux/$EXTENSION_FILE.sha256" \
      "com.ongres/x86_64/linux/$EXTENSION_FILE.tgz"
    tar cf "com.ongres/x86_64/linux/$EXTENSION_FILE.tar" -C com.ongres/x86_64/linux/ \
      "$EXTENSION_FILE.sha256" "$EXTENSION_FILE.tgz"
  fi
done
EOF
)"

cp index.json index.json.orig
jq "$(cat << 'EOF'
  . as $index
    | .extensions | map(.name) | index("pageinspect") as $pageinspect_index
    | $index
    | .extensions[$pageinspect_index].versions += [.extensions[$pageinspect_index].versions[0]]
    | .extensions[$pageinspect_index].versions[1].version = "1.8"
    | . as $index
    | .extensions | map(.name) | index("intagg") as $intagg_index
    | $index
    | .extensions[$intagg_index].versions += [.extensions[$intagg_index].versions[0]]
    | .extensions[$intagg_index].versions[1].version = "1.2"
EOF
)" index.json.orig > index.json
rm index.json.orig
