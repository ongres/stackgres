BEGIN;

CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE DOMAIN log_type AS char(2) CHECK (VALUE IN ('pg','pa'));

CREATE SEQUENCE log_postgres_log_time_index_seq INCREMENT 2 MINVALUE 0 MAXVALUE 1000000 CACHE 1000 CYCLE;

CREATE TABLE log_postgres (
  log_time timestamp(3) with time zone NOT NULL,
  log_time_index integer NOT NULL DEFAULT nextval('log_postgres_log_time_index_seq'),
  pod_name text NOT NULL,
  role text,
  error_severity text,
  message text,
  user_name text,
  database_name text,
  process_id integer,
  connection_from text,
  session_id text,
  session_line_num bigint,
  command_tag text,
  session_start_time timestamp with time zone,
  virtual_transaction_id text,
  transaction_id bigint,
  sql_state_code text,
  detail text,
  hint text,
  internal_query text,
  internal_query_pos integer,
  context text,
  query text,
  query_pos integer,
  location text,
  application_name text,
  PRIMARY KEY (log_time, log_time_index)
);

SELECT create_hypertable('log_postgres', 'log_time', chunk_time_interval => INTERVAL '1 year');

ALTER SEQUENCE log_postgres_log_time_index_seq OWNED BY log_postgres.log_time_index;

CREATE FUNCTION log_postgres_set_log_time_index() RETURNS trigger
AS
$$
BEGIN
  NEW.log_time_index := nextval('log_postgres_log_time_index_seq');
  RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER log_postgres_set_log_time_index
  BEFORE INSERT ON log_postgres
  FOR EACH ROW EXECUTE FUNCTION log_postgres_set_log_time_index();

CREATE FUNCTION log_postgres_tsvector(log_postgres) RETURNS tsvector
AS
$$
  SELECT to_tsvector('simple',
  coalesce($1.message, '')       || ' ' ||
  coalesce($1.query, '')         || ' ' ||
  coalesce($1.detail, '')        || ' ' ||
  coalesce($1.hint, '')          || ' ' ||
  coalesce($1.database_name, '') || ' ' ||
  coalesce($1.user_name, '')     || ' ' ||
  coalesce($1.application_name, ''));
$$
LANGUAGE sql
IMMUTABLE;

CREATE INDEX log_postgres_fts_idx ON log_postgres USING GIN (log_postgres_tsvector(log_postgres));

CREATE SEQUENCE log_patroni_log_time_index_seq INCREMENT 2 MINVALUE 1 MAXVALUE 1000001 CACHE 1000 CYCLE;

CREATE TABLE log_patroni (
  log_time timestamp(3) with time zone NOT NULL,
  log_time_index integer NOT NULL DEFAULT nextval('log_patroni_log_time_index_seq'),
  pod_name text NOT NULL,
  role text,
  error_severity text,
  message text,
  PRIMARY KEY (log_time, log_time_index)
);

SELECT create_hypertable('log_patroni', 'log_time', chunk_time_interval => INTERVAL '1 year');

ALTER SEQUENCE log_patroni_log_time_index_seq OWNED BY log_patroni.log_time_index;

CREATE FUNCTION log_patroni_set_log_time_index() RETURNS trigger
AS
$$
BEGIN
  NEW.log_time_index := nextval('log_patroni_log_time_index_seq');
  RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER log_patroni_set_log_time_index
  BEFORE INSERT ON log_patroni
  FOR EACH ROW EXECUTE FUNCTION log_patroni_set_log_time_index();

CREATE FUNCTION log_patroni_tsvector(log_patroni) RETURNS tsvector
AS
$$
  SELECT to_tsvector('simple', coalesce($1.message, ''));
$$
LANGUAGE sql
IMMUTABLE;

CREATE INDEX log_patroni_fts_idx ON log_patroni USING GIN (log_patroni_tsvector(log_patroni));

CREATE EXTENSION plpython3u;

CREATE FUNCTION split_csv(
  line text,
  delim_char char(1) = ',',
  quote_char char(1) = '"',
  escap_char char(1) = E'\\')
RETURNS SETOF text[]
AS
$$
  import csv
  return csv.reader(line.splitlines(), quotechar=quote_char, delimiter=delim_char, skipinitialspace=True, escapechar=escap_char)
$$
LANGUAGE plpython3u IMMUTABLE;

CREATE FUNCTION log_postgres_extract_fields() RETURNS trigger
AS
$$
DECLARE
  fields text[];
BEGIN
  fields := split_csv(NEW.message);
  NEW.user_name := nullif(fields[01], '');
  NEW.database_name := nullif(fields[02], '');
  NEW.process_id := nullif(fields[03], '')::integer;
  NEW.connection_from := nullif(fields[04], '');
  NEW.session_id := nullif(fields[05], '');
  NEW.session_line_num := nullif(fields[06], '')::bigint;
  NEW.command_tag := nullif(fields[07], '');
  NEW.session_start_time := nullif(fields[08], '')::timestamptz;
  NEW.virtual_transaction_id := nullif(fields[09], '');
  NEW.transaction_id := nullif(fields[10], '')::bigint;
  NEW.error_severity := nullif(fields[11], '');
  NEW.sql_state_code := nullif(fields[12], '');
  NEW.message := nullif(fields[13], '');
  NEW.detail := nullif(fields[14], '');
  NEW.hint := nullif(fields[15], '');
  NEW.internal_query := nullif(fields[16], '');
  NEW.internal_query_pos := nullif(fields[17], '')::integer;
  NEW.context := nullif(fields[18], '');
  NEW.query := nullif(fields[19], '');
  NEW.query_pos := nullif(fields[20], '')::integer;
  NEW.location := nullif(fields[21], '');
  NEW.application_name := nullif(fields[22], '');
  RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER log_postgres_extract_fields
  BEFORE INSERT ON log_postgres
  FOR EACH ROW EXECUTE FUNCTION log_postgres_extract_fields();

COMMIT;
