DO $updateworkers$
DECLARE worker_index text;
BEGIN
  FOR worker_index IN SELECT worker::text FROM generate_series(0, %1$d - 1) AS worker
  LOOP
    IF ddp_has_shard_connection(shard_name => worker_index) IS NULL THEN
      PERFORM ddp_create_shard_connection(
        shard_name => worker_index,
        shard_string_connection => format(%2$s, worker_index),
        um_data => %3$s,
        local_user => %4$s,
        fetch_size => %5$d);
    END IF;
  END LOOP;
END$updateworkers$;
