DO $$
  DECLARE
    statement_to_drop text;
  BEGIN
    FOR statement_to_drop IN (
        SELECT
          'ALTER TABLE IF EXISTS ONLY ' || quote_ident(pg_namespace.nspname) ||'.'|| quote_ident(pg_class.relname)
          || ' DROP CONSTRAINT IF EXISTS ' || quote_ident(pg_constraint.conname) || ';'  AS statement
        FROM pg_constraint
          JOIN pg_class ON pg_class.oid = pg_constraint.conrelid
          JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace
        WHERE contype IN ('c', 'u', 'r', 'm', 'f', 'x')
          AND pg_namespace.nspname NOT IN ('pg_catalog', 'pg_toast', 'information_schema')
        ORDER BY pg_constraint.contype,pg_namespace.nspname,pg_class.relname,pg_constraint.conname) LOOP
      EXECUTE statement_to_drop;
    END LOOP;
  END;$$;
