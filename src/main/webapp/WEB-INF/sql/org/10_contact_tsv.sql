-- SCRIPTS
     DO $$
    BEGIN
    IF NOT EXISTS (
        select 1 from information_schema.columns
        where table_name ='jss_contact' and table_schema=current_schema  and column_name='contact_tsv'
        ) THEN
        alter table jss_contact add column contact_tsv tsvector ;
    END IF;
    END$$;  
-- SCRIPTS
  update jss_contact  set contact_tsv = to_tsvector(con."name"||' '||con."title") 
  from (select case when c.name is null then '' else c.name end as name ,
  case when c.title is null then '' else c.title end as title,c.id
  from jss_contact ex join contact c on ex.id=c.id and ex.contact_tsv is null limit 1000) con
  where jss_contact.id=con.id
