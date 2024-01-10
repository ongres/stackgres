CREATE EXTENSION IF NOT EXISTS dblink;
CREATE EXTENSION IF NOT EXISTS postgres_fdw;

CREATE OR REPLACE FUNCTION ddp_create_shard_connection (shard_name text, shard_string_connection text[], um_data text[]  ) RETURNS BOOLEAN AS 
$$
DECLARE
new_shard text := '';
new_um text := '';
--msg error variables
msg text;
msg_detail text;
sqlerror text;
BEGIN
   new_shard:='CREATE SERVER ' || quote_ident ('ddp_shard_'||$1)|| ' FOREIGN DATA WRAPPER postgres_fdw
                OPTIONS('||
                (CASE WHEN $2[1] IS NOT NULL THEN ' host '''||$2[1]||''',' ELSE ' '::text END) 
                || ' port '''|| $2[2] ||''', dbname '''|| $2[3] ||''', fetch_size '''||
                coalesce ($2[4],10000::text)
               --(CASE WHEN $2[4] IS NOT NULL THEN $2[4] ELSE 10000::text END) 
               || ''', use_remote_estimate  ''on'')';
   IF current_setting('log_min_messages')='debug1' THEN
    RAISE NOTICE  'new_shard: %',new_shard;
   END IF;
              
   EXECUTE new_shard;
   new_um='CREATE USER MAPPING FOR '|| quote_ident(coalesce ($3[3],$3[1])) ||' 
                 SERVER '||quote_ident('ddp_shard_'||$1)|| ' 
                 OPTIONS(user '''|| $3[1] || ''', password '''|| $3[2] ||''')';
   IF current_setting('log_min_messages')='debug1' THEN
    RAISE NOTICE  'new_um: %',new_um;
   END IF;
  EXECUTE new_um;
 
  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'Error adding new shard : %, %, %, %', sqlerror, msg,msg_detail,new_shard || new_um;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;    

CREATE OR REPLACE FUNCTION ddp_change_shard_connection (shard_name text, shard_string_connection text[], um_data text[]  ) RETURNS BOOLEAN AS 
$$
DECLARE
new_shard text;
new_um text;
--msg error variables
msg text;
msg_detail text;
sqlerror text;
BEGIN
   IF $2 IS NOT NULL THEN 	
	   new_shard:='ALTER SERVER '|| quote_ident ('ddp_shard_'||$1)||' OPTIONS(SET host '''|| $2[1] || ''', SET port '''|| $2[2] ||''', SET dbname '''|| $2[3] ||''', 
                   SET fetch_size '''||(CASE WHEN $2[4] IS NOT NULL THEN $2[4] ELSE 10000::text END)||''')';
	
	    IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE  'new_shard: %',new_shard;
	    END IF;
      
	    EXECUTE new_shard;
		IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE 'server % changed', 'ddp_shard_'||$1;
	    END IF;  
	   
  END IF;
  IF $3 IS NOT NULL THEN 
	   new_um='ALTER  USER MAPPING FOR '|| quote_ident(coalesce ($3[3],$3[1])) ||' 
	                 SERVER '||quote_ident('ddp_shard_'||$1)|| ' 
	                 OPTIONS( SET user '''|| $3[1] || ''', SET password '''|| $3[2] ||''')';
	                
	   IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE  'new_um: %',new_um;
	   END IF;  
	  EXECUTE new_um;
	  IF current_setting('log_min_messages')='debug1' THEN
	    RAISE NOTICE 'User mapping % changed', 'shard_'||$1;
	  END IF;
  END IF; 
 
  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'Error adding new shard : %, %, %, %', sqlerror, msg,msg_detail,new_shard;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ddp_create_shard_connection (shard_name text, shard_string_connection text, um_data text, local_user text,fetch_size int default 10000  ) RETURNS BOOLEAN AS 
$$
DECLARE
new_shard text := '';
new_um text := '';
--msg error variables
msg text;
msg_detail text;
sqlerror text;
BEGIN
   new_shard:='CREATE SERVER ' || quote_ident ('ddp_shard_'||$1)|| ' FOREIGN DATA WRAPPER postgres_fdw
                OPTIONS('|| $2 ||', fetch_size '''||
                coalesce ($5::text,10000::text)
               || ''', use_remote_estimate  ''on'')';
   IF current_setting('log_min_messages')='debug1' THEN
    RAISE NOTICE  'new_shard: %',new_shard;
   END IF;
              
   EXECUTE new_shard;
   new_um='CREATE USER MAPPING FOR '|| quote_ident(coalesce ($4,'public')) ||' 
                 SERVER '||quote_ident('ddp_shard_'||$1)|| ' 
                 OPTIONS('|| $3 ||')';
   IF current_setting('log_min_messages')='debug1' THEN
    RAISE NOTICE  'new_um: %',new_um;
   END IF;
  EXECUTE new_um;
 
  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'Error adding new shard : %, %, %, %', sqlerror, msg,msg_detail,new_shard || new_um;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;  

CREATE OR REPLACE FUNCTION ddp_change_shard_connection (shard_name text, shard_string_connection text, um_data text, local_user text,fetch_size int default 10000  ) RETURNS BOOLEAN AS 
$$
DECLARE
new_shard text;
new_um text;
--msg error variables
msg text;
msg_detail text;
sqlerror text;
BEGIN
   IF $2 IS NOT NULL THEN 	
	   new_shard:='ALTER SERVER '|| quote_ident ('ddp_shard_'||$1)||' OPTIONS('|| $2 || ', 
                   SET fetch_size '''||coalesce ($5::text,10000::text)||''')';
	
	    IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE  'new_shard: %',new_shard;
	    END IF;
      
	    EXECUTE new_shard;
		IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE 'server % changed', 'ddp_shard_'||$1;
	    END IF;  
	   
  END IF;
  IF $3 IS NOT NULL THEN 
	   new_um='ALTER  USER MAPPING FOR '|| quote_ident($4) ||' 
	                 SERVER '||quote_ident('ddp_shard_'||$1)|| ' 
	                 OPTIONS( '|| $3 || ')';
	                
	   IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE  'new_um: %',new_um;
	   END IF;  
	  EXECUTE new_um;
	  IF current_setting('log_min_messages')='debug1' THEN
	    RAISE NOTICE 'User mapping % changed', 'shard_'||$1;
	  END IF;
  END IF; 
 
  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'Error adding new shard : %, %, %, %', sqlerror, msg,msg_detail,new_shard || new_um;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;


CREATE TYPE t_shard_status_connection AS (shard_name text, status text) ;

CREATE OR REPLACE FUNCTION ddp_get_shard_status_connection () RETURNS 
setof t_shard_status_connection AS 
$$
DECLARE
r record;
result t_shard_status_connection;
status text;
--msg error variables
msg text;
msg_detail text;
sqlerror text;
BEGIN
    
   
	  for r in select srvname  from pg_catalog.pg_foreign_server loop
		begin  
		result.shard_name:=  r.srvname;
	  	result.status:= (select  dblink_connect('get_shard_status_connection', r.srvname));
	    perform dblink_disconnect('get_shard_status_connection');
	    exception
	      WHEN OTHERS THEN
	       result.status:='FAIL';
	        GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
            RAISE NOTICE  'get_shard_status_connection  %, %, %', sqlerror, msg,msg_detail;
	    end;
	   return next result;
	  end loop;
	  
 
 
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'get_shard_status_connection  %, %, %', sqlerror, msg,msg_detail;
  
END;
$$
LANGUAGE plpgsql;     




CREATE OR REPLACE FUNCTION ddp_add_vs_in_shard(sch text, tab text,remote_tab_name text,shard text ) RETURNS BOOLEAN AS 
$$
DECLARE 
tab_def text;
idx_def text;
new_remote_tab text:='';
rec bigint;
msg text;
msg_detail text;
sqlerror text;
BEGIN 
  --get table definition
  -- it is possible to create the PK, if exists, in  shards 
  tab_def := (SELECT string_agg(cols,' , ')|| coalesce( (SELECT  ' , '|| pg_get_constraintdef(pgc.oid) 
					FROM pg_constraint pgc
					JOIN pg_namespace nsp on nsp.oid = pgc.connamespace
					JOIN pg_class  cls on pgc.conrelid = cls.oid
					LEFT JOIN information_schema.constraint_column_usage ccu
					          ON pgc.conname = ccu.constraint_name
					          AND nsp.nspname = ccu.constraint_schema
					WHERE contype ='p' AND ccu.table_schema=$1 AND ccu.table_name=$2),' ')
				FROM (
				SELECT a.attname ||' '|| pg_catalog.format_type(a.atttypid, a.atttypmod)||' '|| CASE WHEN a.attnotnull THEN 'not null' ELSE '' END AS cols
				FROM pg_catalog.pg_attribute a
				WHERE a.attrelid= ($1||'.'||$2)::regclass  AND a.attnum > 0 AND NOT a.attisdropped
				ORDER BY a.attnum) as sub );	
  --create remote table 
  new_remote_tab :='CREATE TABLE IF NOT EXISTS '||$3::text||' ('||tab_def||');';
       IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE  'new_remote_tab: %',new_remote_tab;
	   END IF; 
 
  
  --verify the open connectios if exists
  IF (SELECT (dblink_get_connections()) @>array['conn']) THEN 
     PERFORM dblink_disconnect('conn');
  END IF;
  --connect to shard
    IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE 'connect to shard: %',$4;
	END IF;
  
  PERFORM dblink_connect('conn', $4);
  PERFORM dblink_exec('conn', new_remote_tab);
  --create indexes if exists
  --get indexex definition
  FOR tab_def IN  (SELECT 'CREATE INDEX '||$3::text||'_'||indexname|| ' ON '||$3::text || substring(indexdef from position (' USING ' in indexdef) for length(indexdef) ) 
               FROM pg_catalog.pg_indexes WHERE schemaname =$1 AND tablename =$2) LOOP
  	IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE  'indexes: %',tab_def;
	END IF;
	                          
  	PERFORM dblink_exec('conn', tab_def);
  END LOOP;
  
  
  PERFORM dblink_disconnect('conn');

  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'add_vs_in_shard: Error adding vs in worker: %, %, %, %',$4, sqlerror, msg,msg_detail;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ddp_create_vs (sch text, tab text, col_part text,workers text[],num_vs int default 120  ) RETURNS BOOLEAN AS 
$$
DECLARE
--compute the interval of each paritition
--range for int 
vs_interval_int int:=(2147483648  +2147483647)/$5;	--range of integer -2147483648 to +2147483647, https://www.postgresql.org/docs/current/datatype-numeric.html
--range for bigint 
vs_interval_bigint bigint:=(CASE WHEN $5=1 THEN 0 WHEN $5=2 THEN 9223372036854775807 ELSE  (9223372036854775808  +9223372036854775807)/$5 END);
from_interval_int int:=-2147483648; 
from_interval_big bigint:=-9223372036854775808;
from_interval text := null;
vs_interval text:= null;
num_part int :=1;
num_part_interval int :=1;
worker_pos int:=1;
new_partition text:='';
vs_name text;
col_def text;
tab_def text;
sufix text :=to_char(now(),'YYYYMMDD_HHMMSS');
--msg error variables
msg text;
msg_detail text;
sqlerror text;
begin
  --initial verifications
  IF ($5=1) THEN
    RAISE EXCEPTION 'create_vs: 1 worker is not allowed';
  END IF;	
 
  IF ($5<array_length($4, 1)) THEN
    RAISE EXCEPTION 'create_vs: The num of shard can not be minor than number of workers';
  END IF;
  --setting variables according to datatype and create the partition table
      --get column definition
	  col_def := (SELECT  pg_catalog.format_type(a.atttypid, a.atttypmod) AS cols
					FROM pg_catalog.pg_attribute a
					WHERE a.attrelid= ($1||'.'||$2)::regclass  AND a.attnum > 0 AND  a.attname=$3  AND  NOT a.attisdropped
					ORDER BY a.attnum );
				
	  IF col_def not in ('integer','bigint') THEN 
	    RAISE EXCEPTION 'create_vs: Column data type [%] is not  allowed, please use (integer,bigint)',col_def;
	  else
	    case
		    when col_def = 'integer' then from_interval='-2147483648';
		    when col_def = 'bigint' then from_interval='-9223372036854775808';
		    else RAISE EXCEPTION 'create_vs: numeric Intervals errors 1';
		end case; 
	    case
		    when col_def = 'integer' then vs_interval=vs_interval_int::text;
		    when col_def = 'bigint' then vs_interval=vs_interval_bigint::text;
		    else RAISE EXCEPTION 'create_vs: numeric Intervals errors 2';
		end case;
	  END IF ;
	  --get table definition, 
	  --you can not create the foreign table as a partition of the parent table if there are UNIQUE indexes on the parent table 
	  --https://www.postgresql.org/docs/current/sql-createforeigntable.html
	 tab_def := (SELECT string_agg(cols,' , ') from (
				SELECT a.attname ||' '|| pg_catalog.format_type(a.atttypid, a.atttypmod)||' '|| CASE WHEN a.attnotnull THEN 'not null' ELSE '' END AS cols
				FROM pg_catalog.pg_attribute a
				WHERE a.attrelid= ($1||'.'||$2)::regclass  AND a.attnum > 0 AND NOT a.attisdropped
				ORDER BY a.attnum) as sub );
	
	 tab_def := 'CREATE TABLE '||$1||'.'||$2||'_temp('||  tab_def|| ') PARTITION BY RANGE('||$3||');';	
	 IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE  'new_tab_def: %',tab_def;
	 END IF;  
	
     EXECUTE tab_def;
  --loop to create the num of virtual shard (FDW)
  WHILE num_part <= $5  loop
	--setting the name of vs_FDW_table  
	vs_name:=(CASE WHEN num_part<10 THEN  $2||'_vs_00'||num_part::text WHEN num_part<100 then  $2||'_vs_0'||num_part::text else $2||'_vs_'||num_part::text END);
  	--create partition localy
    new_partition:='CREATE FOREIGN TABLE  IF NOT EXISTS '||vs_name || ' PARTITION OF '||$1||'.'||$2||'_temp' ||
             ' FOR VALUES FROM ('||(from_interval::bigint::text)||') TO ('|| CASE WHEN num_part=$5 THEN 'MAXVALUE' ELSE (from_interval::bigint+vs_interval::bigint)::text END  ||')
               SERVER '|| CASE WHEN workers[worker_pos] IS NULL THEN workers[worker_pos-1] ELSE workers[worker_pos] END ||' OPTIONS (schema_name '''||$1||''', table_name '''||vs_name||''');';
     
     IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE  'new_partition: %',new_partition;
	 END IF;
              
   EXECUTE new_partition;
   --create the tables in shard
   --PERFORM add_vs_in_shard ($1, $2,vs_name,workers[worker_pos] ) ;
   PERFORM ddp_add_vs_in_shard ($1, $2,vs_name,case when workers[worker_pos] is not null then workers[worker_pos] else workers[worker_pos-1] end ) ;
	
   --inc num_part
   num_part:=num_part+1;
   
   --inc from_interval if not is the last interval and the limit of bigint
   IF num_part <= $5 THEN
    from_interval:=(from_interval::bigint+vs_interval::bigint)::text;
   END IF;
   --analyze if change to another worker, inc num_part_interval
  num_part_interval:=num_part_interval+1;
  IF num_part_interval>($5/(array_length($4, 1))) THEN
     num_part_interval:=1; --restart internal counter and inc worker_pos
     worker_pos:=worker_pos+1;
  END IF;
	  
  END LOOP;
  --rename _temp table to original
  new_partition := 'ALTER TABLE '||($1||'.'||$2)|| ' RENAME TO '||($2)||'_old_'||sufix;     
  EXECUTE new_partition;
  new_partition := 'ALTER TABLE '||($1||'.'||$2)|| '_temp RENAME TO '||($2);     
  EXECUTE new_partition;
 
  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'create_vs: Error adding new partition for main table: %, %, %, %', sqlerror, msg,msg_detail,new_partition;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION ddp_tables_distribution (sch text, tab text ) RETURNS 
TABLE (main_table text, vs_foreign_table text, worker text, worker_dsn text[], pointing_to text[], range_interval text) AS 
$$
SELECT ptree.parentrelid as main_table, c.relname foreign_table , fs.srvname as  shard,fs.srvoptions as server_dsn, ft.ftoptions  as pointing_to,
    replace (replace (replace ( pg_catalog.pg_get_expr(c.relpartbound, c.oid),'FOR VALUES',''),'(''',''),''')','' ) as range_interval FROM pg_catalog.pg_foreign_table ft 
	JOIN pg_catalog.pg_foreign_server fs on ft.ftserver =fs.oid 
	JOIN pg_catalog.pg_class c on ftrelid =c.oid
	JOIN (SELECT relid::oid as oid,parentrelid  FROM pg_partition_tree($1||'.'||$2) WHERE parentrelid is not null ) as ptree on c.oid=ptree.oid
ORDER BY 1;
$$
LANGUAGE sql;



CREATE OR REPLACE FUNCTION ddp_drop_vs_from_shard(foreign_table text,shard text, mode text DEFAULT 'soft' , sufix text default '') RETURNS BOOLEAN AS 
$$
DECLARE 
cmd text;
msg text;
msg_detail text;
sqlerror text;
BEGIN 
  
  IF ($3 NOT IN ('soft','full')) THEN
    RAISE EXCEPTION 'drop_vs_from_shard: mode  not allowed, please use soft or full';
  END IF;	
  --drop remote table 
  IF $3 = 'soft' THEN 
    cmd :='ALTER TABLE '||$1::text||' RENAME TO ' ||$1::text||'_soft_del_'||$4;
    IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE 'drop_vs_from_shard : shards: % -> %',$2,cmd;
	END IF; 
   
  ELSE
    cmd :='DROP TABLE IF EXISTS '||$1::text;
    IF current_setting('log_min_messages')='debug1' THEN
	      RAISE NOTICE 'drop_vs_from_shard : shards: % -> %',$2,cmd;
	END IF; 
    
  END IF;
    
  --verify the open connectios if exists
  IF (SELECT (dblink_get_connections()) @>array['conn']) THEN 
     PERFORM dblink_disconnect('conn');
  END IF;
  --connect to shard
  PERFORM dblink_connect('conn', $2);
  PERFORM dblink_exec('conn', cmd);
    
  PERFORM dblink_disconnect('conn');

  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'drop_vs_from_shard: Error drop vs from worker: %, %, %, %',$4, sqlerror, msg,msg_detail;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION ddp_drop_vs (sch text, tab text,mode text DEFAULT 'soft'  ) RETURNS BOOLEAN AS 
$$
DECLARE
r record;
cmd text;
sufix text :=to_char(now(),'YYYYMMDD_HHMMSS');
--msg error variables
msg text;
msg_detail text;
sqlerror text;
begin
  --initial verifications
  IF ($3 NOT IN ('soft','full')) THEN
    RAISE EXCEPTION 'drop_vs: mode  not allowed, please use soft or full';
  END IF;	
 
  for r in  SELECT ptree.parentrelid as main_table, c.relname foreign_table , fs.srvname as  shard,fs.srvoptions as server_dsn, ft.ftoptions  as pointing_to,
    replace (replace (replace ( pg_catalog.pg_get_expr(c.relpartbound, c.oid),'FOR VALUES',''),'(''',''),''')','' ) as range_interval FROM pg_catalog.pg_foreign_table ft 
	JOIN pg_catalog.pg_foreign_server fs on ft.ftserver =fs.oid 
	JOIN pg_catalog.pg_class c on ftrelid =c.oid
	JOIN (SELECT relid::oid as oid,parentrelid  FROM pg_partition_tree($1||'.'||$2) WHERE parentrelid is not null ) as ptree on c.oid=ptree.oid loop
		if $3 = 'soft' then 
		  cmd:='ALTER TABLE '||($1||'.'||$2)|| ' DETACH PARTITION '||r.foreign_table;
		  IF current_setting('log_min_messages')='debug1' THEN
	        RAISE NOTICE 'drop soft: %',cmd;
	      END IF;  
		 
		  execute cmd;
		  cmd:='ALTER TABLE '||r.foreign_table ||' RENAME TO '||r.foreign_table ||'_soft_del_'||sufix;
		  IF current_setting('log_min_messages')='debug1' THEN
	        RAISE NOTICE 'drop soft: %',cmd;
	      END IF; 
		  execute cmd;
		  PERFORM ddp_drop_vs_from_shard (r.foreign_table,r.shard,$3,sufix ) ;
		  cmd:='ALTER TABLE '||r.foreign_table ||'_soft_del_'||sufix ||' OPTIONS ( SET  table_name '''|| r.foreign_table ||'_soft_del_'||sufix ||''')';
		  IF current_setting('log_min_messages')='debug1' THEN
	        RAISE NOTICE 'drop soft: %',cmd;
	      END IF; 
          execute cmd;
		else 
		  cmd:='DROP FOREIGN TABLE IF EXISTS '||r.foreign_table;
		  IF current_setting('log_min_messages')='debug1' THEN
	        RAISE NOTICE 'drop full: %',cmd;
	      END IF; 
		  execute cmd;
		  PERFORM ddp_drop_vs_from_shard (r.foreign_table,r.shard,$3,sufix ) ;
		  
		end if;
	end loop;
	
 
 
  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'drop_vs: Error cleaning vs partition for table: %, %, %, %', sqlerror, msg,msg_detail,$2;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;


CREATE TYPE t_partition_size AS (foreign_table text, shard text, tab text, t_size text) ;
CREATE OR REPLACE FUNCTION ddp_get_partition_size (sch text, tab text) RETURNS 
setof t_partition_size AS 
$$
DECLARE
r record;
result t_partition_size;
--msg error variables
msg text;
msg_detail text;
sqlerror text;
BEGIN
    
   
	  FOR r IN SELECT ptree.parentrelid as main_table, c.relname foreign_table , fs.srvname as  shard, 
				  replace(ft.ftoptions[1],'schema_name=','') ||'.'||replace (ft.ftoptions [2],'table_name=','') pointing_to
				     FROM pg_catalog.pg_foreign_table ft 
					JOIN pg_catalog.pg_foreign_server fs on ft.ftserver =fs.oid 
					JOIN pg_catalog.pg_class c on ftrelid =c.oid
					JOIN (SELECT relid::oid as oid,parentrelid  FROM pg_partition_tree($1||'.'||$2) WHERE parentrelid is not null ) as ptree on c.oid=ptree.oid LOOP
		begin  
		result.foreign_table:=  r.foreign_table;
	    result.shard:=  r.shard;
	    result.tab:=  r.pointing_to;
	  	PERFORM  dblink_connect('get_partition_size', r.shard);
	    result.t_size:= pg_size_pretty((SELECT * FROM dblink('get_partition_size', 'SELECT pg_table_size('''||r.pointing_to ||''')') AS t(z bigint)));
	    perform dblink_disconnect('get_partition_size');
	    exception
	      WHEN OTHERS THEN
	        GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
            RAISE NOTICE  'get_partition_size  %, %, %', sqlerror, msg,msg_detail;
            perform dblink_disconnect('get_partition_size');
	    end;
	   return next result;
	  end loop;
	  
 
 
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'get_partition_size  %, %, %', sqlerror, msg,msg_detail;
  
END;
$$
LANGUAGE plpgsql; 

CREATE OR REPLACE  function   ddp_create_restore_point (rp_name text) RETURNS pg_lsn
AS 
$$
DECLARE
r record;
failed_node text := '0';
cmd text;
result pg_lsn;
shard_result pg_lsn;
sufix text :=to_char(now(),'YYYYMMDD_HHMMSS');
--msg error variables
msg text;
msg_detail text;
sqlerror text;
BEGIN
    --verifying the connection with all shards
	SELECT count(*)::text into failed_node FROM ddp_get_shard_status_connection () WHERE status<>'OK'; 
    IF failed_node <>'0' THEN
      RAISE EXCEPTION 'ddp_create_restore_point  failed connection with shards, please review the connection with: % ', (SELECT string_agg(shard_name,',') FROM get_shard_status_connection () WHERE status<>'OK') ;
    END IF;
   
   --verifying if pg_is_in_recovery
	SELECT pg_is_in_recovery() into failed_node ; 
    IF failed_node ='t' THEN
      RAISE EXCEPTION 'ddp_create_restore_point  recovery is in progress, this function can not be executed in recovery mode ' ;
    END IF;
   
   --verifying wal_level
	SELECT  current_setting('wal_level') into failed_node ; 
    IF failed_node NOT IN ('replica','logical') THEN
      RAISE EXCEPTION 'ddp_create_restore_point  WAL level not sufficient for creating a restore point, please use  replica or logical' ;
    END IF;
   
   --verifying length of rp_name
    IF length($1)>=64 THEN
      RAISE EXCEPTION 'ddp_create_restore_point  value too long for restore point, please use less that 64 characters' ;
    END IF;
    --2PC technique to create the Restore Point
    --local Restore Point
    result:= (SELECT pg_create_restore_point($1));
	/* perform pg_create_restore_point on all shards */
  --if connections exist drop them
  IF (SELECT (dblink_get_connections()) @>array['ddp_create_restore_point']) THEN 
     PERFORM dblink_disconnect('ddp_create_restore_point');
  END IF;
 
	FOR r IN SELECT fs.srvname AS shard
	    FROM pg_catalog.pg_foreign_server fs
	    WHERE fs.srvname LIKE 'ddp_shard_%'
			ORDER BY 1 LOOP
		BEGIN			
	
		   PERFORM  dblink_connect('ddp_create_restore_point', r.shard);
		    --send begin
		   PERFORM dblink_exec('ddp_create_restore_point', 'BEGIN');
		    -- do Restore Point
		   shard_result:= (SELECT * FROM dblink('ddp_create_restore_point', 'SELECT pg_create_restore_point('''||$1 ||''')') AS t(rp pg_lsn));
		    --prepare TX
	     PERFORM dblink_exec('ddp_create_restore_point', 'PREPARE TRANSACTION  ''ddp_shard_'||sufix||'''');
	        --commit TX
	     PERFORM dblink_exec('ddp_create_restore_point', 'COMMIT PREPARED  ''ddp_shard_'||sufix||'''');
       IF current_setting('log_min_messages')='debug1' THEN
	        RAISE NOTICE  'ddp_create_restore_point , restored point in shard % , % -> %',r.shard, $1, shard_result;
	     END IF;  
		   
		   PERFORM dblink_disconnect('ddp_create_restore_point');
	    EXCEPTION
	      WHEN OTHERS THEN
	        GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
	        PERFORM dblink_exec('ddp_create_restore_point', 'ROLLBACK PREPARED  ''ddp_shard_'||sufix||''''); 
	        PERFORM dblink_disconnect('ddp_create_restore_point');
          RAISE NOTICE  'ddp_create_restore_point  %, %, %', sqlerror, msg,msg_detail;
            
	    END;
	END LOOP;
       IF current_setting('log_min_messages')='debug1' THEN
	        RAISE NOTICE  'ddp_create_restore_point , restored point in coordinator, % -> %', $1, result; 
	     END IF;
	  
    RETURN result;
 
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'ddp_create_restore_point  %, %, %', sqlerror, msg,msg_detail;
  
END;
$$
LANGUAGE plpgsql;  

CREATE TYPE t_has_shard_connection AS (shard_name text, status text, pointing_to text) ;


CREATE OR REPLACE FUNCTION ddp_has_shard_connection (shard_name text) RETURNS 
t_has_shard_connection AS 
$$
DECLARE
r record;
result t_has_shard_connection;
status text;
--msg error variables
msg text;
msg_detail text;
sqlerror text;
BEGIN
    
	  SELECT * INTO r  from pg_catalog.pg_foreign_server WHERE srvname = quote_ident ('ddp_shard_'||$1);
	  IF NOT FOUND THEN
       RAISE NOTICE 'shard_name not found % ', $1;
	  ELSE
	    BEGIN
	    result.shard_name:=  r.srvname;
	  	result.status:= (select  dblink_connect('get_shard_status_connection', r.srvname)); 
	    result.pointing_to:= r.srvoptions[1]||' '||r.srvoptions[2]||' '||r.srvoptions[3];
	    perform dblink_disconnect('get_shard_status_connection');
	   exception
	      WHEN OTHERS THEN
	       result.status:='FAIL';
	       result.pointing_to:= r.srvoptions[1]||' '||r.srvoptions[2]||' '||r.srvoptions[3];
	        GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
            RAISE NOTICE  'has_shard_connection  %, %, %', sqlerror, msg,msg_detail;
	    END;
      END IF;
   return result;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'has_shard_connection  %, %, %', sqlerror, msg,msg_detail;
  
END;
$$
LANGUAGE plpgsql;   


CREATE OR REPLACE FUNCTION ddp_drop_shard_connection (shard_name text  ) RETURNS BOOLEAN AS 
$$
DECLARE
cmd text := '';
r record ;
--msg error variables
msg text;
msg_detail text;
sqlerror text;
begin
	
   SELECT srvname ,coalesce (au.rolname,'public') as username INTO r   FROM pg_catalog.pg_foreign_server fs JOIN pg_catalog.pg_user_mapping um  ON (um.umserver=fs.oid) 
    LEFT JOIN  pg_authid au ON (au.oid=umuser)	where srvname = quote_ident ('ddp_shard_'||$1);
   cmd:='DROP USER MAPPING IF EXISTS FOR ' || quote_ident (r.username) || ' SERVER '|| r.srvname;
   IF current_setting('log_min_messages')='debug1' THEN
    RAISE NOTICE  'user removed: %',cmd;
   END IF;
   EXECUTE cmd;
   
   cmd:='DROP SERVER IF EXISTS  ' || quote_ident (r.srvname) ;
   IF current_setting('log_min_messages')='debug1' THEN
    RAISE NOTICE  'shard removed: %',cmd;
   END IF;
   EXECUTE cmd;
  
  
 
  RETURN true;
  EXCEPTION
  WHEN OTHERS THEN 
   GET STACKED DIAGNOSTICS msg = message_text, msg_detail =pg_exception_detail, sqlerror = returned_sqlstate;
    RAISE EXCEPTION 'ddp_drop_shard_connection Error removing  shard : %,  %, %, %, %', $1, sqlerror, msg,msg_detail,cmd ;
    RETURN false;
  
END;
$$
LANGUAGE plpgsql;  