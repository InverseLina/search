 -- SCRIPTS
	 CREATE OR REPLACE FUNCTION multiplydata(offsetNum integer,limitNum integer,iteration_num integer, out result integer)  AS $Body$
	        DECLARE
	         newcolumns text;
	         sql text;
	    BEGIN
	    newcolumns:=(select string_agg(column_name,',') from information_schema.columns
	         where table_schema='public' and table_name='contact' and column_name not in ('id','name','sfid'));
	   sql:='insert into contact (name,sfid,'||newcolumns|| ')  select (name||'' ''||'||iteration_num||') as name, ('||iteration_num||'||substring(sfid from 2)) as sfid,'|| newcolumns||' from contact offset '||offsetNum||' limit '||limitNum;

	    EXECUTE  sql; 
	   result:=1;
	    END;
	    $Body$
	    LANGUAGE 'plpgsql';