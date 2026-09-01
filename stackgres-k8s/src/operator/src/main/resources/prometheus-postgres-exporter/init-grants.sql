REVOKE ALL ON FUNCTION df(text) FROM PUBLIC;
REVOKE ALL ON FUNCTION mounts() FROM PUBLIC;
REVOKE ALL ON FUNCTION monitor_dblink_connect(text) FROM PUBLIC;
REVOKE ALL ON FUNCTION monitor_dblink_connect_pgbouncer() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION df(text) TO monitor;
GRANT EXECUTE ON FUNCTION mounts() TO monitor;
GRANT EXECUTE ON FUNCTION monitor_dblink_connect(text) TO monitor;
GRANT EXECUTE ON FUNCTION monitor_dblink_connect_pgbouncer() TO monitor;
