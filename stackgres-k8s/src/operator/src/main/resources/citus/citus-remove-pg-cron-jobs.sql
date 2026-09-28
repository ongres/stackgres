-- The pg_dist_node maintenance was performed by pg_cron jobs that are replaced by scheduled
-- SGScript entries (see SGScript.spec.scripts[].cron). pg_cron keeps its jobs in the database
-- specified by cron.database_name, that is set to postgres.
DO $$BEGIN
  IF EXISTS (SELECT FROM pg_catalog.pg_extension WHERE extname = 'pg_cron') THEN
    PERFORM cron.unschedule(jobid)
    FROM cron.job
    WHERE jobname IN ('update-query-routers-flags', 'update-query-routers-nodes');
  END IF;
END$$;
