 -- SCRIPTS
	 CREATE OR REPLACE FUNCTION multiplydata(offsetNum integer,limitNum integer,iteration_num integer,tableName varchar, out result integer)  AS $Body$
          DECLARE
           newcolumns text;
           sql text;
      BEGIN
        
        IF tableName='contact' then
           newcolumns:=(select string_agg(column_name,',') from information_schema.columns
           where table_schema='public' and table_name=tableName and column_name not in ('id','name','sfid'));
           sql:='insert into contact (name,sfid,'||newcolumns|| ')  select (name||'' ''||'||iteration_num||') as name, ('||iteration_num||'||substring(sfid from 4)) as sfid,'|| newcolumns||' from contact order by id asc  offset '||offsetNum||' limit '||limitNum;
           EXECUTE  sql; 
        else
           newcolumns:=(select string_agg(column_name,',') from information_schema.columns
           where table_schema='public' and table_name=tableName and column_name not in ('id','sfid'));
           sql:='insert into '||tableName||' (sfid,'||newcolumns|| ')  select ('||iteration_num||'||substring(sfid from 4)) as sfid,'|| newcolumns||' from '||tableName||' order by id asc  offset '||offsetNum||' limit '||limitNum;
           EXECUTE  sql; 
        end IF;
           
    
     result:=1;
      END;
      $Body$
      LANGUAGE 'plpgsql';

      