DO $$
  DECLARE
    statement_to_restore text;
  BEGIN
    DROP TABLE IF EXISTS __migration__indexes_to_restore; 
    CREATE TEMPORARY TABLE __migration__indexes_to_restore AS SELECT statement FROM __migration__.indexes; 
    FOR statement_to_restore IN (
        SELECT statement
        FROM __migration__indexes_to_restore) LOOP
      RAISE NOTICE 'Executing: %', statement_to_restore;
      EXECUTE statement_to_restore;
      DELETE FROM __migration__.indexes WHERE statement = statement_to_restore;
      EXIT;
    END LOOP;
  END;$$;
