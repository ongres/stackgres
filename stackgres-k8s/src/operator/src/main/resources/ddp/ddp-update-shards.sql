DO $updateworkers$
DECLARE worker_index text;
BEGIN
  FOR worker_index IN SELECT worker::text FROM generate_series(0, %1$d - 1) AS worker
  LOOP
    IF ddp_has_shard_connection(shard_name => worker_index) IS NULL THEN
      PERFORM ddp_create_shard_connection(
        shard_name => worker_index,
        shard_string_connection => format($quoted$%2$s$quoted$, worker_index),
        um_data => $quoted$%3$s$quoted$,
        local_user => $quoted$%4$s$quoted$,
        fetch_size => %5d);
    END IF;
  END LOOP;
END$updateworkers$;
