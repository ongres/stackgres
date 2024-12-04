DO $updatedatabases$
DECLARE database_name text;
BEGIN
  FOREACH database_name IN ARRAY ARRAY[%1$s]
  LOOP
    IF NOT EXISTS (SELECT * FROM pg_database WHERE datname = database_name) THEN
      PERFORM dblink(
        'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=''postgres''',
        'CREATE DATABASE ' || quote_ident(database_name));
    END IF;
  END LOOP;
END$updatedatabases$;
