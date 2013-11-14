-- SCRIPTS
     DO $$
    BEGIN
    IF NOT EXISTS (
        select 1 from information_schema.columns where table_name ='contact_ex' and table_schema=current_schema  and column_name='contact_tsv'
        ) THEN
        alter table contact_ex add column contact_tsv tsvector ;
        CREATE INDEX contact_ex_contact_tsv_gin  ON contact_ex  USING gin  (contact_tsv);
    END IF;
    END$$;  
-- SCRIPTS
  update contact_ex  set contact_tsv = to_tsvector('english',con."name"||' '||con."title") 
  from (select c.name,c.title,c.id from contact_ex ex join contact c on ex.id=c.id and ex.contact_tsv is null limit 1000) con
  where contact_ex.id=con.id
