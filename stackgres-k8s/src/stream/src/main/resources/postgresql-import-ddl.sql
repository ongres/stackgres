DO $$
  DECLARE
    connname text;
    command text;
    previous text;
    err text;
    msg text;
  BEGIN
    IF EXISTS (SELECT * FROM (
        SELECT datname, unnest(setconfig) AS setconfig
        FROM pg_database JOIN pg_db_role_setting ON (pg_database.oid = pg_db_role_setting.setdatabase))
      WHERE datname = $datname$%3$s$datname$ AND setconfig = 'sgstream.ddl_import_completed=true')
    THEN
      RETURN;
    END IF;
    connname = 'migration-' || to_char(CURRENT_TIMESTAMP, 'YYYY-MM-DD-HH24-MI-SS') || '-' || (RANDOM() * 2147483647)::::int;
    previous = '';
    PERFORM dblink_disconnect(conn) FROM unnest(dblink_get_connections()) AS conn WHERE conn = connname;
    PERFORM dblink_connect(connname,'dbname=' || $datname$%3$s$datname$);
    FOR command IN (
        SELECT line
        FROM dblink(
          '%1$s',
          $dump$
          DROP TABLE IF EXISTS input; CREATE TEMPORARY TABLE input (line text);
          COPY input FROM PROGRAM $cmd$pg_dumpall --clean --if-exists --roles-only$cmd$ DELIMITER E'\1';
          COPY input FROM PROGRAM $cmd$pg_dump --clean --if-exists --schema-only --dbname=%2$s --no-publications --no-subscriptions$cmd$ DELIMITER E'\1';
          SELECT line FROM input
            WHERE line NOT LIKE '-- %%' AND line NOT LIKE '--' AND line != '' -- Skip comments and empty lines
            AND line NOT SIMILAR TO '(CREATE|ALTER|DROP) ROLE(| IF EXISTS) %4$s(;| )%%' -- Skip SGCluster existing roles
            ;
          $dump$)
        AS _(line text))
    LOOP
      BEGIN
        EXECUTE format(
          $execute_imported_command$
          SELECT dblink_exec($imported_command_connection$%%3$s$imported_command_connection$,
            $imported_command_do$
              DO $imported_command_body$
                BEGIN RETURN;
                %%1$s%%2$s
                END;$imported_command_body$;
            $imported_command_do$);
          $execute_imported_command$, previous, command, connname);
        EXECUTE format(
          $execute_imported_command$
          SELECT dblink_exec($imported_command_connection$%%3$s$imported_command_connection$,
            $imported_command_do$
              DO $imported_command_body$
                BEGIN
                  EXECUTE $imported_command$%%1$s%%2$s$imported_command$;
                END;$imported_command_body$;
            $imported_command_do$);
          $execute_imported_command$, previous, command, connname);
        previous = '';
      EXCEPTION
        WHEN SQLSTATE '42601' OR SQLSTATE '42804' THEN
          GET STACKED DIAGNOSTICS err = RETURNED_SQLSTATE, msg = MESSAGE_TEXT;
          previous = previous || command || E'\n';
        WHEN OTHERS THEN
          GET STACKED DIAGNOSTICS err = RETURNED_SQLSTATE, msg = MESSAGE_TEXT;
          PERFORM dblink_disconnect(connname);
          RAISE EXCEPTION E'%%: %%\n\n%%\n', err, msg, previous || command;
      END;
    END LOOP;
    PERFORM dblink_disconnect(connname);
    IF previous != '' THEN
      RAISE EXCEPTION E'%%: %%\n\n%%\n', err, msg, previous;
    END IF;
    EXECUTE 'ALTER DATABASE "%3$s" SET sgstream.ddl_import_completed = true';
  END;$$;
