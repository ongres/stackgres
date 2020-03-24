CREATE USER "fluentd";

CREATE DATABASE "$distributed-logs-base" OWNED BY "fluentd";

\c "$distributed-logs-base"

BEGIN;

CREATE EXTENSION IF NOT EXISTS plpython3u;

CREATE OR REPLACE FUNCTION split_csv(
  line text,
  delim_char char(1) = ',',
  quote_char char(1) = '"',
  escap_char char(1) = E '\\')
RETURNS SETOF text[] 
AS
$$
  import csv
  return csv.reader(line.splitlines(), quotechar=quote_char, delimiter=delim_char, skipinitialspace=True, escapechar=escap_char)
$$
LANGUAGE plpython3u IMMUTABLE;

CREATE TABLE IF NOT EXISTS postgres_log (
  log_time timestamp(3) with time zone,
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
  error_severity text,
  sql_state_code text,
  message text,
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

CREATE TABLE IF NOT EXISTS postgres_log_proxy (
  log_time timestamp(3) with time zone,
  message text
);

CREATE OR REPLACE FUNCTION postgres_log_proxy() RETURNS trigger
AS
$$
BEGIN
  WITH csvlog AS (
    SELECT
      NEW.log_time,
      split_csv(replace(NEW.message, chr(10), '#EOL')) AS fields
  )
  INSERT INTO postgres_log
    SELECT
      log_time,
      nullif(fields[01], '') AS user_name,
      nullif(fields[02], '') AS database_name,
      nullif(fields[03], '')::integer AS process_id,
      nullif(fields[04], '') AS connection_from,
      nullif(fields[05], '') AS session_id,
      nullif(fields[06], '')::bigint AS session_line_num,
      nullif(fields[07], '') AS command_tag,
      nullif(fields[08], '')::timestamptz AS session_start_time,
      nullif(fields[09], '') AS virtual_transaction_id,
      nullif(fields[10], '')::bigint AS transaction_id,
      nullif(fields[11], '') AS error_severity,
      nullif(fields[12], '') AS sql_state_code,
      nullif(fields[13], '') AS message,
      nullif(fields[14], '') AS detail,
      nullif(fields[15], '') AS hint,
      nullif(fields[16], '') AS internal_query,
      nullif(fields[17], '')::integer AS internal_query_pos,
      nullif(fields[18], '') AS context,
      nullif(fields[19], '') AS query,
      nullif(fields[20], '')::integer AS query_pos,
      nullif(fields[21], '') AS location,
      nullif(fields[22], '') AS application_name
    FROM
      csvlog;
  RETURN NULL;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS postgres_log_proxy ON postgres_log_proxy;
CREATE TRIGGER postgres_log_proxy
  BEFORE INSERT ON postgres_log_proxy
  FOR EACH ROW EXECUTE FUNCTION postgres_log_proxy();

COMMIT;
