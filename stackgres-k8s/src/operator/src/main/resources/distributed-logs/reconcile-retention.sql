DO $reconcileretention$
DECLARE database_entry text array;
DECLARE table_name text;
BEGIN
  FOREACH database_entry SLICE 1 IN ARRAY ARRAY[%1$s]
  LOOP
    FOR table_name IN SELECT name::text FROM unnest(ARRAY[%2$s]) AS name
    LOOP
      PERFORM dblink(
        'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=' || quote_literal(database_entry[0]),
        'SELECT set_chunk_time_interval(' || quote_literal(table_name) || ', CAST(' || quote_literal(database_entry[1]) || ' AS INTERVAL))');
      IF EXISTS (SELECT * FROM pg_extension WHERE extname = 'timescaledb' AND SPLIT_PART(extversion,'.',1) = '2') THEN
        PERFORM dblink(
          'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=' || quote_literal(database_entry[0]),
          'SELECT drop_retention_policy(' || quote_literal(table_name) || ')');
        PERFORM dblink(
          'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=' || quote_literal(database_entry[0]),
          'SELECT add_retention_policy(' || quote_literal(table_name) || ', CAST(' || quote_literal(database_entry[1]) || ' AS INTERVAL');
      ELSIF EXISTS (SELECT * FROM pg_extension WHERE extname = 'timescaledb' AND SPLIT_PART(extversion,'.',1) = '1') THEN
        -- next reconciliation will take place on %3$s
        PERFORM dblink(
          'host=/var/run/postgresql port=5432 user=' || CURRENT_USER || ' sslmode=disable dbname=' || quote_literal(database_entry[0]),
          'SELECT drop_chunks(date_trunc(' || quote_literal(split_part(database_entry[1], ' ', 2)) || ', now()) - CAST(' || quote_literal(database_entry[1]) || ' AS INTERVAL), ' || quote_literal(table_name) || ')');
      END IF;
    END LOOP;
  END LOOP;
END$reconcileretention$;
