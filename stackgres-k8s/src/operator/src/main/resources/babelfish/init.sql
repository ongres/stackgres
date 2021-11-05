CREATE EXTENSION IF NOT EXISTS babelfishpg_tds CASCADE;
CREATE EXTENSION IF NOT EXISTS babelfishpg_money;

CREATE EXTENSION IF NOT EXISTS plpython3u;

CREATE OR REPLACE FUNCTION append_system_parameter_line(line text)
RETURNS void
AS
$$
  file_object = open('/var/lib/postgresql/data/postgresql.auto.conf', 'a')
  file_object.write(line + '\n')
  file_object.close()
$$
LANGUAGE plpython3u;

SELECT append_system_parameter_line('babelfishpg_tsql.database_name = ''babelfish''');
SELECT append_system_parameter_line('babelfish_pg_tsql.sql_dialect = ''tsql''');

DROP FUNCTION append_system_parameter_line(line text);

ALTER DATABASE babelfish SET babelfishpg_tsql.migration_mode = 'single-db';
CALL SYS.INITIALIZE_BABELFISH('babelfish');

SELECT pg_reload_conf();
