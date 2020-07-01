#!/bin/bash
set -e
IFS=$'\n'
INIT_SCRIPT_PATH=/etc/patroni/init-script.d

# check if path exist
if [ -d "$INIT_SCRIPT_PATH" -a "$(ls -1 "$INIT_SCRIPT_PATH" 2>/dev/null|wc -l)" -ge 1 ]
then
  for FILE in $(ls -1 "$INIT_SCRIPT_PATH")
  do
    FILE="$(realpath "$INIT_SCRIPT_PATH/$FILE")"
    if [ "${FILE: -3}" == ".sh" -a -f "$FILE" ]
    then
      echo "Executing shell script $FILE"
      bash "$FILE"
      echo "Shell script $FILE executed"
    fi
  done
  # search for .sql file and execute them
  for FILE in $(ls -1 "$INIT_SCRIPT_PATH")
  do
    FILE="$(realpath "$INIT_SCRIPT_PATH/$FILE")"
    if [ "${FILE: -4}" == ".sql" -a -f "$FILE" ]
    then
      DATABASE="$([ "$(basename "$FILE" | tr '.' '\n' | wc -l)" -gt 2 ] \
        && echo "$(basename "$FILE" | tr '.' '\n' | tail -n 2 | head -n -1 | tr '\n' '.')" \
        || echo postgres)"
      DATABASE="${DATABASE%.}"
      echo "Executing SQL script $FILE for DATABASE $DATABASE with user postgres on port ${POSTGRES_PORT}"
      cat "$FILE" | python3 -c "$(cat << EOF
import psycopg2,sys
connection = psycopg2.connect("user=postgres dbname='$DATABASE' port=${POSTGRES_PORT}")
connection.autocommit = True
cursor = connection.cursor()
cursor.execute(sys.stdin.read())
try: print(cursor.fetchall())
except: print()
EOF
)"
      echo "Shell SQL $FILE executed"
    fi
  done
fi
