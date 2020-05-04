#!/bin/bash
set -e
IFS=$'\n'
INIT_SCRIPT_PATH=/etc/patroni/init-script.d

# check if path exist
if [ -d "$INIT_SCRIPT_PATH" -a "$(ls -1 "$INIT_SCRIPT_PATH" 2>/dev/null|wc -l)" -ge 1 ]
then
  for file in $(ls -1 "$INIT_SCRIPT_PATH")
  do
    file="$(realpath "$INIT_SCRIPT_PATH/$file")"
    if [ "${file: -3}" == ".sh" -a -f "$file" ]
    then
      echo "Executing shell script $file"
      bash "$file"
      echo "Shell script $file executed"
    fi
  done
  # search for .sql file and execute them
  for file in $(ls -1 "$INIT_SCRIPT_PATH")
  do
    file="$(realpath "$INIT_SCRIPT_PATH/$file")"
    if [ "${file: -4}" == ".sql" -a -f "$file" ]
    then
      database="$([ "$(basename "$file" | tr '.' '\n' | wc -l)" -gt 2 ] \
        && echo "$(basename "$file" | tr '.' '\n' | tail -n 2 | head -n -1 | tr '\n' '.')" \
        || echo postgres)"
      database="${database%.}"
      echo "Executing SQL script $file for database $database with user postgres on port ${POSTGRES_PORT}"
      cat "$file" | python3 -c '
import psycopg2,sys
c=psycopg2.connect("user=postgres dbname='"'$database'"' port='"${POSTGRES_PORT}"'")
s=c.cursor()
s.execute(sys.stdin.read())
c.commit()
try: print(s.fetchall())
except: print()'
      echo "Shell SQL $file executed"
    fi
  done
fi
