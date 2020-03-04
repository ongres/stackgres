cat << 'EOF' > /usr/local/bin/exec-with-env
#!/bin/sh

set -e

die() {
  >&2 echo "$@"
  exit 1
}

REPLACES=""
OVERWRITE=false

while [ "$#" -gt 0 ]
do
  case "$1" in
  -r|--replace)
    shift
    if [ -z "$REPLACES" ]
    then
      REPLACES="$1"
    else
      REPLACES="$REPLACES,$1"
    fi
    shift
    ;;
  -o|--overwrite)
    shift
    OVERWRITE=true
    ;;
  --)
    shift
    break
    ;;
  *)
    if echo "$1" | grep -q "^/"
    then
      plain_envdir="$1"
      secret_envdir=""
      [ -d "$plain_envdir" ] \
        || die "$plain_envdir is not a directory"
    else
      secret_envdir="${BASE_SECRET_PATH}/$1"
      plain_envdir="${BASE_ENV_PATH}/$1"
      [ -d "$plain_envdir" -o -d "$secret_envdir" ] \
        || die "None of $plain_envdir or $secret_envdir is a directory"
    fi
    shift
    for envdir in "$plain_envdir" "$secret_envdir"
    do
      [ -d "$envdir" ] || continue
      # When md5sum of values of environment variables ordered alphabetically (excluding variable
      # MD5SUM) does not match variable MD5SUM we fail since in transition state
      [ "$(ls -1a "$envdir" | grep -v "^MD5SUM$" \
        | while read envvar; do [ ! -f "$envdir/$envvar" ] || cat "$envdir/$envvar"; done \
        | md5sum | cut -d ' ' -f 1 | tr 'a-z' 'A-Z')" = "$(cat "$envdir/MD5SUM")" ] \
        || die "Environment variable in transient state"
      for envvar in $(ls -1a "$envdir")
      do
        # Only export if "$envdir/$envvar" is a file
        # and environment variable with name $envvar is not set
        [ ! -f "$envdir/$envvar" ] || [ "$OVERWRITE" != "true" -a -n "$(eval "echo \"\$$envvar\"")" ] \
          || eval "export $envvar='$(cat "$envdir/$envvar")'"
      done
    done
    ;;
  esac
done

if [ -n "$REPLACES" ]
then
  for REPLACE in $(echo "$REPLACES" | tr ',' '\n')
  do
    eval "export ${REPLACE%=*}=\"\$$${REPLACE#*=}\""
  done
fi

if [ -n "$1" ]
then
  exec "$@"
fi
EOF

sed -i "s#\${BASE_ENV_PATH}#${BASE_ENV_PATH}#g" /usr/local/bin/exec-with-env
sed -i "s#\${BASE_SECRET_PATH}#${BASE_SECRET_PATH}#g" /usr/local/bin/exec-with-env

chmod a+x /usr/local/bin/exec-with-env
