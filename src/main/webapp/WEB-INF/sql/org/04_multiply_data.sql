 -- SCRIPTS
    CREATE OR REPLACE FUNCTION multiplydata(offsetNum integer,limitNum integer,iteration_num integer,tableName varchar, out result integer)  AS $Body$
          DECLARE
           newcolumns text;
           sql text;
      BEGIN
        IF tableName='contact' then
           newcolumns:=(select string_agg(column_name,',') from information_schema.columns
           where table_schema=current_schema and table_name=tableName and column_name not in ('id','name','sfid'));
           sql:='insert into contact (name,sfid,'||newcolumns|| ')  select (name||'' ''||'||iteration_num||') as name, ('||iteration_num||'||substring(sfid from 4)) as sfid,'|| newcolumns||' from contact where id in (select id from contact order by id asc offset '||offsetNum||' limit '||limitNum||')'; 
           EXECUTE  sql; 
        else
           newcolumns:=(select string_agg(column_name,',') from information_schema.columns
           where table_schema=current_schema and table_name=tableName and column_name not in ('id','sfid','ts2__contact__c'));
           sql:='insert into '||tableName||' (ts2__contact__c,sfid,'||newcolumns|| ')  select ('||iteration_num||'||substring(ts2__contact__c from 4)) as ts2__contact__c,('||iteration_num||'||substring(sfid from 4)) as sfid,'|| newcolumns||' from '||tableName||'   where id in (select id from contact order by id asc offset '||offsetNum||' limit '||limitNum||')'; 
           EXECUTE  sql; 
        end IF;
           
    
     result:=1;
      END;
      $Body$
      LANGUAGE 'plpgsql';

      
      