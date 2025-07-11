#/bin/sh

set -eu

if ! [ -f "$PGBOUNCER_AUTH_FILE_PATH" ]
then
  mkdir -p "$PGBOUNCER_AUTH_PATH"
  cp "$PGBOUNCER_AUTH_TEMPLATE_FILE_PATH" "$PGBOUNCER_AUTH_FILE_PATH".tmp
  mv "$PGBOUNCER_AUTH_FILE_PATH".tmp "$PGBOUNCER_AUTH_FILE_PATH"
fi

until {
  ! grep -q '^client_tls_cert_file = ' "$PGBOUNCER_CONFIG_FILE_PATH" \
    || test -f "$(grep '^client_tls_cert_file = ' "$PGBOUNCER_CONFIG_FILE_PATH" | cut -d ' ' -f 3-)"
  } && {
  ! grep -q '^client_tls_key_file = ' "$PGBOUNCER_CONFIG_FILE_PATH" \
    || test -f "$(grep '^client_tls_key_file = ' "$PGBOUNCER_CONFIG_FILE_PATH" | cut -d ' ' -f 3-)"
  }
do
  sleep 1
done  

exec /usr/local/bin/pgbouncer "$PGBOUNCER_CONFIG_FILE_PATH"
