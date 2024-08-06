SELECT DISTINCT
  context
FROM
  BBFCompass
  -- filter on table variable operation:
WHERE
  item LIKE '% @tableVariable'
  -- filter on function with >= 2 parameters:
  AND context IN (
    SELECT
      context
    FROM
      BBFCompass
    WHERE
      context LIKE 'FUNCTION %'
      AND item LIKE '% parameter'
    GROUP BY
      context
    HAVING
      count(*) >= 2)
    -- filter on MONEY-type parameter:
    AND context IN (
      SELECT
        context
      FROM
        BBFCompass
      WHERE
        context LIKE 'FUNCTION %'
        AND item LIKE 'MONEY %function parameter%')
      -- filter on function result type:
      AND context IN (
        SELECT
          context
        FROM
          BBFCompass
        WHERE
          context LIKE 'FUNCTION %'
          AND item LIKE 'SMALLDATETIME %scalar function result%')
