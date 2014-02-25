-- SCRIPTS
	   DO $$
    BEGIN
    IF NOT EXISTS (
        select 1 from information_schema.columns where table_name ='jss_contact' and table_schema=current_schema  and column_name='sfid'
        ) THEN
       alter table jss_contact add column sfid character varying(18);
    END IF;
    END$$;
-- SCRIPTS
	update jss_contact  set sfid = con.sfid
	from (select c.sfid,c.id from jss_contact ex join contact c on ex.id=c.id and ex.sfid is null limit 1000) con
	where jss_contact.id=con.id
