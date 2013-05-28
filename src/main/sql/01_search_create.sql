CREATE EXTENSION pg_trgm; 

ALTER TABLE contact disable trigger c5_c5_contact_logtrigger;

ALTER TABLE contact
   ADD COLUMN resume_tsv tsvector;
   
CREATE INDEX contact_idx_resume_gin
  ON contact
  USING gin
  (resume_tsv);

UPDATE contact SET resume_tsv = to_tsvector('english', "ts2__Text_Resume__c" );

CREATE INDEX contact_idx_Title_trgm_gin ON contact USING gin ("Title" gin_trgm_ops);
  
CREATE TRIGGER contact_trg_resume_tsv
  BEFORE INSERT OR UPDATE
  ON contact
  FOR EACH ROW
  EXECUTE PROCEDURE tsvector_update_trigger('resume_tsv', 'pg_catalog.english', 'ts2__Text_Resume__c');
  
CREATE INDEX contact_Title_trgm_gin ON contact USING gin ("Title" gin_trgm_ops);
CREATE INDEX contact_Name_trgm_gin ON contact USING gin ("Name" gin_trgm_ops);
CREATE INDEX contact_FirstName_trgm_gin ON contact USING gin ("FirstName" gin_trgm_ops);
CREATE INDEX contact_LastName_trgm_gin ON contact USING gin ("LastName" gin_trgm_ops);
