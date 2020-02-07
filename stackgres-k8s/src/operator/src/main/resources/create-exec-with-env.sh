cat << EOF > /usr/local/bin/exec-with-env
#!/bin/sh

die() {
  >&2 echo "\$@"
  exit 1
}

while true
do
  case "\$1":
  --)
    shift
    break
    ;;
  *)
    [ -d "\$1" ] || die "\$1 is not a directory"
    [ "\$(ls -lv1a "\$1" | grep -v "^MD5SUM\$" | xargs -r -n 1 cat | md5sum)" == "\$(cat "\$1/MD5SUM")" ] \
      || die "Environment variable in transient state"
    for envvar in \$(ls -lv1a "\$1")
    do
      eval "export \$envvar='\$(cat "\$1/\$envvar")'"
    done
    ;;
done

if [ ! -z "\$1"]
then
  exec "\$@"
fi
EOF
chmod a+x /usr/local/bin/exec-with-env
