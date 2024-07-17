CREATE TABLE IF NOT EXISTS __migration__.indexes AS
  SELECT
      REPLACE(pg_catalog.pg_get_indexdef(pg_class_index.oid), ' WHERE ',
        COALESCE(' TABLESPACE ' || pg_catalog.quote_ident(pg_tablespace.spcname), '')
      || ' WHERE ') || ';'
      || CASE WHEN pg_index.indisclustered THEN
        E'\n' || 'ALTER TABLE ' || pg_catalog.quote_ident(pg_namespace.nspname) || '.'
        || pg_catalog.quote_ident(pg_class_table.relname) || ' CLUSTER ON '
        || pg_catalog.quote_ident(pg_class_index.relname) || ';'
      ELSE '' END
      || COALESCE(E'\n' || 'COMMENT ON INDEX ' || pg_catalog.quote_ident(pg_namespace.nspname) || '.'
      || pg_catalog.quote_ident(pg_class_index.relname) || ' IS ' || quote_literal(pg_description.description)
      || ';', '') AS statement
    FROM pg_catalog.pg_index
      INNER JOIN pg_catalog.pg_class pg_class_table ON pg_class_table.oid = pg_index.indrelid
      INNER JOIN pg_catalog.pg_class pg_class_index ON pg_class_index.oid = pg_index.indexrelid
      LEFT JOIN pg_catalog.pg_description ON pg_description.objoid = pg_class_index.oid
      INNER JOIN pg_catalog.pg_namespace ON pg_namespace.oid = pg_class_table.relnamespace
      LEFT JOIN pg_catalog.pg_tablespace ON pg_tablespace.oid = pg_class_index.reltablespace
    WHERE pg_class_table.relkind IN ('r', 'm') AND pg_class_index.relkind = 'i'
      AND indisprimary = false
      AND NOT EXISTS (SELECT * FROM pg_catalog.pg_constraint WHERE pg_constraint.conindid = pg_index.indexrelid)
      AND pg_namespace.nspname NOT IN ('pg_catalog', 'pg_toast', 'information_schema')
    ORDER BY pg_namespace.nspname DESC, pg_class_index.relname DESC;
