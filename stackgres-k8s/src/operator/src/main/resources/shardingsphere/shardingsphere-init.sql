CREATE EXTENSION IF NOT EXISTS dblink;
CREATE EXTENSION IF NOT EXISTS postgres_fdw;

DO $createlink$
BEGIN
  IF NOT EXISTS (SELECT * FROM pg_foreign_server WHERE srvname = 'shardingsphere') THEN
    CREATE SERVER shardingsphere FOREIGN DATA WRAPPER postgres_fdw OPTIONS (host %1$s, port %2$s, dbname 'postgres');
  END IF;
  IF NOT EXISTS (SELECT * FROM pg_user_mappings JOIN pg_roles ON (umuser = pg_roles.oid)
      WHERE rolname = %4$s AND srvname = 'shardingsphere') THEN
    CREATE USER MAPPING FOR %6$s SERVER shardingsphere OPTIONS (user %4$s, password %5$s);
  ELSE
    ALTER USER MAPPING FOR %6$s SERVER shardingsphere OPTIONS (user %4$s, password %5$s);
  END IF;
END$createlink$;
