ALTER TABLE contact
   ADD COLUMN resume_tsv tsvector;
   
CREATE INDEX contact_idx_resume_gin
  ON contact
  USING gin
  (resume_tsv);

UPDATE contact SET resume_tsv = to_tsvector('english', "ts2__Text_Resume__c" );
  
CREATE TRIGGER contact_resume_tsv_trigger
  BEFORE INSERT OR UPDATE
  ON contact
  FOR EACH ROW
  EXECUTE PROCEDURE tsvector_update_trigger('resume_tsv', 'pg_catalog.english', 'ts2__Text_Resume__c');
  