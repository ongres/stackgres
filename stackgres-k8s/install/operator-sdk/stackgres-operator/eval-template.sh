#!/bin/sh

TEMPLATE="$1"

mkdir -p target/"${TEMPLATE%/*}"

cat << GENERATE_MAKE_EOF > target/"$TEMPLATE".sh
cat << MAKE_EOF
$(cat "$TEMPLATE")
MAKE_EOF
GENERATE_MAKE_EOF

sh target/"$TEMPLATE".sh 2> target/"$TEMPLATE".err

if [ -s target/"$TEMPLATE".err ]
then
  cat target/"$TEMPLATE".err >&2
  exit 1
fi
