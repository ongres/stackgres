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
    for envvar in \$(ls "\$1")
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
