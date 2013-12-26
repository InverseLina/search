-- SCRIPTS
	   DO $$
    BEGIN
    IF NOT EXISTS (
        select 1 from information_schema.columns where table_name ='contact_ex' and table_schema=current_schema  and column_name='sfid'
        ) THEN
       alter table contact_ex add column sfid character varying(18);
    END IF;
    END$$;
-- SCRIPTS
	update contact_ex  set sfid = con.sfid
	from (select c.sfid,c.id from contact_ex ex join contact c on ex.id=c.id and ex.sfid is null ) con
	where contact_ex.id=con.id
