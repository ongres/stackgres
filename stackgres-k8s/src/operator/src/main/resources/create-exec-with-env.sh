cat << 'EOF' > /usr/local/bin/exec-with-env
#!/bin/sh

set -e

die() {
  >&2 echo "$@"
  exit 1
}

while [ "$#" -gt 0 ]
do
  case "$1" in
  --)
    shift
    break
    ;;
  *)
    envdir="$1"
    shift
    [ -d "$envdir" ] || die "$envdir is not a directory"
    [ "$(ls -1a "$envdir" | grep -v "^MD5SUM$" \
      | while read envvar; do [ ! -f "$envdir/$envvar" ] || cat "$envdir/$envvar"; done \
      | md5sum | cut -d ' ' -f 1 | tr 'a-z' 'A-Z')" == "$(cat "$envdir/MD5SUM")" ] \
      || die "Environment variable in transient state"
    for envvar in $(ls -1a "$envdir")
    do
      [ -n "$(eval "echo \"\$$envvar\"")" -o ! -f "$envdir/$envvar" ] \
        || eval "export $envvar='$(cat "$envdir/$envvar")'"
    done
  esac
done

if [ -n "$1" ]
then
  exec "$@"
fi
EOF

chmod a+x /usr/local/bin/exec-with-env
