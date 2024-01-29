DO $updateworkers$
DECLARE worker_index text;
BEGIN
  FOR worker_index IN SELECT worker::text FROM generate_series(0, %1$d - 1) AS worker
  LOOP
    PERFORM dblink_exec('shardingsphere',
      format(
        $rdl$
        REGISTER STORAGE UNIT IF NOT EXISTS %%1s (
          HOST = '%%2s',
          PORT = %3$d,
          DB = '%4$s',
          USER = '%5$s',
          PASSWORD = '%6$s');
        $rdl$,
        'ds_' || worker_index,
        format('%2$s', worker_index)));
  END LOOP;
END$updateworkers$;
