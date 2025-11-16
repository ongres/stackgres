CREATE SCHEMA IF NOT EXISTS __migration__;
CREATE TABLE IF NOT EXISTS __migration__.constraints AS
  SELECT statement FROM (
    SELECT
      pg_constraint.contype AS type,
      pg_namespace.nspname AS schema_name,
      pg_class.relname AS table_name,
      pg_constraint.conname AS name,
      'ALTER TABLE ' || quote_ident(pg_namespace.nspname) ||'.'|| quote_ident(pg_class.relname)
      || ' ADD CONSTRAINT ' || quote_ident(pg_constraint.conname) || ' '|| pg_get_constraintdef(pg_constraint.oid)
      || ';' AS statement
    FROM pg_constraint
      JOIN pg_class ON pg_class.oid = pg_constraint.conrelid
      JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace
    WHERE contype IN ('c', 'f', 'u', 't', 'x')
      AND pg_namespace.nspname NOT IN ('pg_catalog', 'pg_toast', 'information_schema')
      AND pg_class.relkind = 'r'
    UNION ALL
    SELECT
      'n' AS type,
      pg_namespace.nspname AS schema_name,
      pg_class.relname AS table_name,
      attname AS name,
      'ALTER TABLE ' || quote_ident(pg_namespace.nspname) ||'.'|| quote_ident(pg_class.relname)
      || ' ALTER COLUMN ' || quote_ident(pg_attribute.attname) || ' SET NOT NULL;' AS statement
    FROM pg_attribute
      JOIN pg_class ON pg_class.oid = pg_attribute.attrelid
      JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace
      LEFT JOIN pg_index ON pg_index.indisprimary
        AND pg_index.indrelid = pg_attribute.attrelid
        AND pg_attribute.attnum = ANY(pg_index.indkey)
    WHERE indisprimary IS NULL AND attnum > 0 AND attnotnull
      AND pg_namespace.nspname NOT IN ('pg_catalog', 'pg_toast', 'information_schema', '__migration__')
      AND pg_class.relkind = 'r'
    ORDER BY type DESC,schema_name DESC,table_name DESC,name DESC
  );
