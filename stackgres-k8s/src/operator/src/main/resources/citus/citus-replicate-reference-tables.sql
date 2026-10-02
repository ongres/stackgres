DO $$
BEGIN
  -- Replicate the reference tables to the active nodes that miss any of them (like the query
  -- routers activated by the previous script entry). Writes to the reference tables are blocked
  -- while they are copied, so replicate_reference_tables is only called when needed.
  IF EXISTS (
    SELECT FROM pg_dist_node
    CROSS JOIN pg_dist_partition
    WHERE pg_dist_node.isactive AND pg_dist_node.noderole = 'primary'
    AND pg_dist_partition.partmethod = 'n' AND pg_dist_partition.repmodel = 't'
    AND NOT EXISTS (
      SELECT FROM pg_dist_shard
      JOIN pg_dist_placement USING (shardid)
      WHERE pg_dist_shard.logicalrelid = pg_dist_partition.logicalrelid
      AND pg_dist_placement.groupid = pg_dist_node.groupid)) THEN
    PERFORM replicate_reference_tables('block_writes');
  END IF;
END$$;
