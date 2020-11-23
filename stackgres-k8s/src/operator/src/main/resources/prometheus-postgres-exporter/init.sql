CREATE EXTENSION IF NOT EXISTS dblink;
CREATE EXTENSION IF NOT EXISTS plpython3u;

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
