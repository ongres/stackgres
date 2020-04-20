BEGIN;

CREATE DOMAIN log_type AS char(2) CHECK (VALUE IN ('pg','pa'));

CREATE TABLE log_postgres (
  log_time timestamp(3) with time zone NOT NULL,
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
  application_name text
);

CREATE INDEX log_postgres_main_idx ON log_postgres(log_time);

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

CREATE TABLE log_patroni (
  log_time timestamp(3) with time zone NOT NULL,
  pod_name text NOT NULL,
  role text,
  error_severity text,
  message text
);

CREATE INDEX log_patroni_main_idx ON log_patroni(log_time);

CREATE FUNCTION log_patroni_tsvector(log_patroni) RETURNS tsvector
AS
$$
  SELECT to_tsvector('simple', coalesce($1.message, ''));
$$
LANGUAGE sql
IMMUTABLE;

CREATE INDEX log_patroni_fts_idx ON log_patroni USING GIN (log_patroni_tsvector(log_patroni));

COMMIT;
