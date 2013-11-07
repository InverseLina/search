-- SCRIPTS
	 DO $$
	  BEGIN
	  IF NOT EXISTS (
        select 1 from information_schema.columns where table_name ='contact_ex' and table_schema=current_schema  and column_name='sfid'
	      ) THEN
	     alter table contact_ex add column sfid character varying(18);
	  END IF;
	  update contact_ex ex  set sfid = (select sfid from contact c where c.id= ex.id ) 
    where ex.id in (select id from contact_ex   where sfid is null limit 10000);
	  END$$;  
	  
