-- SCRIPTS
	   DO $$
    BEGIN
    IF NOT EXISTS (
        select 1 from information_schema.columns where table_name ='jss_contact' and table_schema=current_schema  and column_name='resume_lower'
        ) THEN
       alter table jss_contact add column resume_lower text;
    END IF;
    END$$;
-- SCRIPTS
	update jss_contact  set resume_lower = con.resume_lower
	from (select case when c.ts2__text_resume__c is null then '' else lower(c.ts2__text_resume__c) end as resume_lower, c.sfid from jss_contact ex join contact c on ex.sfid=c.sfid and ex.resume_lower is null limit 1000) con
	where jss_contact.sfid=con.sfid