-- SCRIPTS
	DO $$ 
	BEGIN 
	IF NOT EXISTS (
	    SELECT 1
	    FROM  org    
	    WHERE  name = 'JobScience'
	    ) THEN
	  insert into org (name, schemaname, sfid) values ('JobScience','demo64', null);
	END IF;
	END$$;