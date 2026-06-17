CREATE EXTENSION IF NOT EXISTS dblink;
CREATE EXTENSION IF NOT EXISTS plpython3u;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

CREATE OR REPLACE FUNCTION df(path text)
RETURNS SETOF text
AS
$$
  import subprocess
  try:
    result = subprocess.run(['df', '-B1', '--output=source,target,fstype,size,avail,itotal,iavail', path], timeout=1, stdout=subprocess.PIPE, stderr=subprocess.PIPE, encoding='UTF-8')
  except:
    return ['- ' + path + ' - - - - - timeout']
  if result.returncode == 0:
    return result.stdout.split('\n')[1:2]
  else:
    return ['- ' + path + ' - - - - - ' + result.stderr.replace(' ', '\\s')]
$$
LANGUAGE plpython3u;

CREATE OR REPLACE FUNCTION mounts()
RETURNS SETOF text
AS
$$
  import subprocess
  return subprocess.run(['cat', '/proc/mounts'], stdout=subprocess.PIPE, encoding='UTF-8').stdout.split('\n')
$$
LANGUAGE plpython3u;

-- Opens (or refreshes) a named dblink connection to a local database and returns
-- its name, so the postgres-exporter fan-out queries can read every database's
-- statistics while running as the low-privilege monitor role.
--
-- dblink forbids a non-superuser from opening a connection that did not actually
-- authenticate with a password (CVE-2007-6601). The exporter reaches PostgreSQL
-- over the unix socket (pg_hba: local all all trust), so the monitor role cannot
-- open those connections directly. This SECURITY DEFINER function, owned by the
-- bootstrap superuser, opens the connection on the caller's behalf -- but it
-- always connects as session_user (i.e. the monitor role itself) over the unix
-- socket, so the remote session keeps only the caller's own (pg_monitor)
-- privileges and there is no escalation. Because the returned connection is then
-- reused via dblink('<name>', ...) at the call site, dblink performs no further
-- non-superuser check and the per-query column definition list stays where it is.
--
-- A single fixed connection name is reused and re-pointed at each target database
-- in turn: dblink() fully materializes a result set before returning, so the
-- connection is idle by the time the next database is processed. This keeps at
-- most one backend connection open at a time (as the previous, superuser, exporter
-- did) instead of one per database held for the whole exporter session.
CREATE OR REPLACE FUNCTION monitor_dblink_connect(target_dbname text)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO pg_catalog, public
AS $$
DECLARE
  conn_name text := 'monitor_dblink';
  conn_str text :=
    'host=/var/run/postgresql port=5432 sslmode=disable'
    || ' user=''' || replace(replace(session_user, '\', '\\'), '''', '\''') || ''''
    || ' dbname=''' || replace(replace(target_dbname, '\', '\\'), '''', '\''') || '''';
BEGIN
  IF conn_name = ANY (COALESCE(public.dblink_get_connections(), ARRAY[]::text[])) THEN
    PERFORM public.dblink_disconnect(conn_name);
  END IF;
  PERFORM public.dblink_connect(conn_name, conn_str);
  RETURN conn_name;
END;
$$;

-- Same idea for the pgbouncer admin console (port 6432). It is parameter-free on
-- purpose: it always connects to the reserved "pgbouncer" database as the built-in
-- "pgbouncer" console user, which pgbouncer admits over the unix socket without a
-- password when the peer uid matches the pooler process (both run as the postgres
-- uid in the pod). The SECURITY DEFINER context only lifts dblink's non-superuser
-- password gate; pgbouncer still authorizes the connection itself.
CREATE OR REPLACE FUNCTION monitor_dblink_connect_pgbouncer()
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO pg_catalog, public
AS $$
DECLARE
  conn_name text := 'monitor_dblink_pgbouncer';
  conn_str text :=
    'host=/var/run/postgresql port=6432 dbname=pgbouncer user=pgbouncer sslmode=disable';
BEGIN
  IF conn_name = ANY (COALESCE(public.dblink_get_connections(), ARRAY[]::text[])) THEN
    PERFORM public.dblink_disconnect(conn_name);
  END IF;
  PERFORM public.dblink_connect(conn_name, conn_str);
  RETURN conn_name;
END;
$$;
