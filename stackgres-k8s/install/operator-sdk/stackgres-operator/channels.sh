#!/bin/sh

VERSION="$1"
shift

if [ "x$VERSION" = x ]
then
  >&2 echo "Error: you must specifiy the version as first parameter"
  exit 1
fi

if [ "$#" -lt 1 ]
then
  >&2 echo "Error: you must specifiy at least a channel with a grep regular expression separated by a semicolon"
  exit 1
fi

MATCHED_CHANNELS=0
CHANNELS=""
while [ "$#" -gt 0 ]
do
  CHANNELS="$CHANNELS $1"
  CHANNEL="${1%%:*}"
  REGEXP="${1#*:}"
  shift
  if printf %s "$VERSION" | grep -q "$REGEXP"
  then
    if [ "$MATCHED_CHANNELS" -gt 0 ]
    then
      printf ,
    fi
    printf '%s' "$CHANNEL"
    MATCHED_CHANNELS="$((MATCHED_CHANNELS + 1))"
  fi
done

if [ "$MATCHED_CHANNELS" -eq 0 ]
then
  >&2 echo "Error: version $VERSION does not match any of the channels grep regexp $CHANNELS"
  exit 1
fi