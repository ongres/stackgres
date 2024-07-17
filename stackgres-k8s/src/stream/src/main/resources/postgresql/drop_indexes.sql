DO $$
  DECLARE
    statement_to_drop text;
  BEGIN
    FOR statement_to_drop IN (
        SELECT
          'DROP INDEX IF EXISTS ' || quote_ident(pg_namespace.nspname) ||'.'|| quote_ident(pg_class_index.relname)
          || ';' AS statement
        FROM pg_index
          JOIN pg_class pg_class_table ON pg_class_table.oid = pg_index.indrelid
          JOIN pg_class pg_class_index ON pg_class_index.oid = pg_index.indexrelid
          JOIN pg_namespace ON pg_namespace.oid = pg_class_table.relnamespace
        WHERE pg_class_table.relkind IN ('r', 'm') AND pg_class_index.relkind = 'i'
          AND indisprimary = false
          AND NOT EXISTS (SELECT * FROM pg_catalog.pg_constraint WHERE pg_constraint.conindid = pg_index.indexrelid)
          AND pg_namespace.nspname NOT IN ('pg_catalog', 'pg_toast', 'information_schema')
        ORDER BY pg_namespace.nspname, pg_class_index.relname) LOOP
      EXECUTE statement_to_drop;
    END LOOP;
  END;$$;
