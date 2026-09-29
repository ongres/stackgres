DO $$
DECLARE
  -- Citus connects to a node using the host and port of pg_dist_poolinfo, when set, instead of
  -- the ones of pg_dist_node. Only the port is set, so that the host keeps following the one that
  -- Patroni updates in pg_dist_node after a failover. The entries created here are the ones with
  -- only the port, the others (created by the user) are only replaced when they belong to a node
  -- that has to connect through the pooler.
  update_poolinfo text := $update_poolinfo$
    WITH ports(groupid, port) AS (VALUES %1$s),
    desired AS (
      SELECT nodeid, 'port=' || port AS poolinfo
      FROM pg_catalog.pg_dist_node JOIN ports USING (groupid)),
    deleted AS (
      DELETE FROM pg_catalog.pg_dist_poolinfo
      WHERE poolinfo ~ '^port=[0-9]+$'
      AND nodeid NOT IN (SELECT nodeid FROM desired))
    INSERT INTO pg_catalog.pg_dist_poolinfo (nodeid, poolinfo)
    SELECT nodeid, poolinfo FROM desired
    ON CONFLICT (nodeid) DO UPDATE SET poolinfo = EXCLUDED.poolinfo
    WHERE pg_dist_poolinfo.poolinfo IS DISTINCT FROM EXCLUDED.poolinfo
  $update_poolinfo$;
BEGIN
  EXECUTE update_poolinfo;
  -- pg_dist_poolinfo is not synced to the nodes with metadata, that also connect to the other
  -- nodes (the query routers in the first place)
  PERFORM pg_catalog.run_command_on_workers(update_poolinfo);
END$$;
