-- TimescaleDB version installed %2$s (pending restart: %3$s)
DO $upgradeextensions$
DECLARE database_name text;
BEGIN
  FOREACH database_name IN ARRAY ARRAY[%1$s]
  LOOP
    IF EXISTS (SELECT * FROM pg_database WHERE datname = database_name) THEN
      PERFORM dblink(
        'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=' || quote_literal(database_name),
        $upgradedatabaseextensions$
          DO $innerupgradedatabaseextensions$
          BEGIN
            CREATE EXTENSION IF NOT EXISTS dblink;
            ALTER EXTENSION dblink UPDATE;

            PERFORM dblink(
             'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=' || quote_literal(current_database()),
              'CREATE EXTENSION IF NOT EXISTS timescaledb');
            PERFORM dblink(
             'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=' || quote_literal(current_database()),
              'ALTER EXTENSION timescaledb UPDATE');
          END$innerupgradedatabaseextensions$;
        $upgradedatabaseextensions$);
    END IF;
  END LOOP;
END$upgradeextensions$;
