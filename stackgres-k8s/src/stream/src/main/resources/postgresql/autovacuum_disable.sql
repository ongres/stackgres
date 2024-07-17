DO $$
DECLARE
  statement text
BEGIN
  FOR statement IN (
      SELECT
        'ALTER TABLE ' || quote_ident(pg_namespace.nspname) ||'.'|| quote_ident(pg_class.relname)
        || ' SET (autovacuum_enabled = false, toast.autovacuum_enabled = false);' AS statement
      FROM pg_class
      JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace
      WHERE pg_class.relkind IN ('r')
      ORDER BY pg_namespace.nspname, pg_class.relname) LOOP
    EXECUTE statement;
  END LOOP;
END;$$
