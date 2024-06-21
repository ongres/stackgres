DO $$
  DECLARE
    statement_to_restore text;
  BEGIN
    DROP TABLE IF EXISTS __migration__constraints_to_restore; 
    CREATE TEMPORARY TABLE __migration__constraints_to_restore AS SELECT statement FROM __migration__.constraints; 
    FOR statement_to_restore IN (
        SELECT statement
        FROM __migration__constraints_to_restore) LOOP
      EXECUTE statement_to_restore;
      DELETE FROM __migration__.constraints WHERE statement = statement_to_restore;
    END LOOP;
  END;$$;

DROP SCHEMA IF EXISTS __migration__ CASCADE;
