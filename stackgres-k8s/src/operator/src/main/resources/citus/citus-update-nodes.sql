DO $$
DECLARE
  node record;
  router_groupid integer;
  activated boolean := false;
BEGIN
  -- Do not wait for the default 30 seconds on each Pod that is gone
  PERFORM set_config('citus.node_connection_timeout', '5000', true);

  -- Patroni only maintains the Citus groups that are present in the DCS, so the workers and the
  -- query routers removed by decreasing workers.clusters or queryRouterClusters stay forever in
  -- pg_dist_node. Being metadata nodes that can not be synced they make Citus reject any node
  -- addition and distributed DDL (Patroni included, that can not even remove the replicas that are
  -- gone from the groups that are left). A node is only removed when its group holds no shard of a
  -- distributed table (reference tables are replicated to every node) and it can not be reached,
  -- so that a node that Patroni has just added, while the ranges of this script are not yet
  -- updated, is not removed (Patroni would not add it back since its cache of pg_dist_node is only
  -- reloaded after an error). citus_remove_node alone needs to connect to the node, so it is
  -- disabled first. The secondaries are removed before the primary of their group.
  IF %4$s THEN
    FOR node IN
      SELECT nodename, nodeport, isactive FROM pg_dist_node
      WHERE (groupid BETWEEN %3$s + 1 AND %1$s OR groupid > %2$s)
      AND NOT EXISTS (
        SELECT FROM pg_dist_placement
        JOIN pg_dist_shard USING (shardid)
        JOIN pg_dist_partition USING (logicalrelid)
        WHERE pg_dist_placement.groupid = pg_dist_node.groupid AND partmethod <> 'n')
      ORDER BY noderole = 'primary'
    LOOP
      IF NOT citus_check_connection_to_node(node.nodename, node.nodeport) THEN
        IF node.isactive THEN
          PERFORM citus_disable_node(node.nodename, node.nodeport, synchronous => true);
        END IF;
        PERFORM citus_remove_node(node.nodename, node.nodeport);
      END IF;
    END LOOP;
  END IF;

  -- A query router registered by Patroni before this script registered it has shouldhaveshards
  -- set to true by citus_add_node
  PERFORM citus_set_node_property(nodename, nodeport, 'shouldhaveshards', false)
  FROM pg_dist_node
  WHERE shouldhaveshards AND groupid BETWEEN %1$s + 1 AND %2$s;

  -- Register the query router groups before Patroni does, since citus_add_node always registers a
  -- node with shouldhaveshards = true. An inactive node never gets shards and Patroni, finding the
  -- group already registered, replaces the placeholder host with the address of the query router
  -- using citus_update_node, that keeps both shouldhaveshards and isactive. The Patroni of a query
  -- router is only started once its group is returned by the script that reads the query routers
  -- without shards (see SGCluster.spec.configurations.patroni.startGateAnnotations).
  FOR router_groupid IN
    SELECT groupid FROM generate_series(%1$s + 1, %2$s) AS groupid
    WHERE NOT EXISTS (SELECT FROM pg_dist_node WHERE pg_dist_node.groupid = groupid.groupid)
  LOOP
    PERFORM citus_add_inactive_node(
      'stackgres-query-router-' || router_groupid || '.invalid', 5432, groupid => router_groupid);
    PERFORM citus_set_node_property(
      'stackgres-query-router-' || router_groupid || '.invalid', 5432, 'shouldhaveshards', false);
  END LOOP;

  -- Patroni never activates a node, so activate the registered query routers once they can be
  -- reached (the placeholder host never resolves). Nodes that have metadata were disabled by
  -- someone else and are left alone.
  FOR node IN
    SELECT nodename, nodeport, shouldhaveshards FROM pg_dist_node
    WHERE NOT isactive AND NOT hasmetadata AND noderole = 'primary'
    AND groupid BETWEEN %1$s + 1 AND %2$s
  LOOP
    IF citus_check_connection_to_node(node.nodename, node.nodeport) THEN
      PERFORM citus_activate_node(node.nodename, node.nodeport);
      activated := true;
    END IF;
  END LOOP;
  -- citus_add_node replicates the reference tables to the new node, citus_activate_node does not
  IF activated THEN
    PERFORM replicate_reference_tables('block_writes');
  END IF;
END$$;
