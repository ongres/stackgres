CREATE EXTENSION IF NOT EXISTS dblink;

DO $createdatabase$
BEGIN
  IF NOT EXISTS (SELECT * FROM pg_database WHERE datname = %1$s) THEN
    PERFORM dblink_exec(
        'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=' || current_database(),
        'CREATE DATABASE ' || quote_ident(%1$s));
  END IF;
END$createdatabase$;
