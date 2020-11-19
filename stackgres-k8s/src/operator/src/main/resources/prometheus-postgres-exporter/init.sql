CREATE EXTENSION IF NOT EXISTS dblink;
CREATE EXTENSION IF NOT EXISTS plpython3u;

CREATE OR REPLACE FUNCTION df(path text)
RETURNS SETOF text
AS
$$
  import subprocess
  return subprocess.run(['df', '-B1', '-T', path], stdout=subprocess.PIPE, encoding='UTF-8').stdout.split('\n')
$$
LANGUAGE plpython3u;