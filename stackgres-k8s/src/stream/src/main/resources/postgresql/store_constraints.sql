CREATE SCHEMA IF NOT EXISTS __migration__;
CREATE TABLE IF NOT EXISTS __migration__.constraints AS
  SELECT
      'ALTER TABLE ' || quote_ident(pg_namespace.nspname) ||'.'|| quote_ident(pg_class.relname)
      || ' ADD CONSTRAINT ' || quote_ident(pg_constraint.conname) || ' '|| pg_get_constraintdef(pg_constraint.oid)
      || ';' AS statement
    FROM pg_constraint
      JOIN pg_class ON pg_class.oid = pg_constraint.conrelid
      JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace
    WHERE contype IN ('c', 'u', 'r', 'm', 'f', 'x')
      AND pg_namespace.nspname NOT IN ('pg_catalog', 'pg_toast', 'information_schema')
    ORDER BY pg_constraint.contype DESC,pg_namespace.nspname DESC,pg_class.relname DESC,pg_constraint.conname DESC;
