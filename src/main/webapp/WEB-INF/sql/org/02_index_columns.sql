-- SCRIPTS
CREATE INDEX contact_ex_idx_resume_gin
  ON contact_ex
  USING gin
  (resume_tsv);
-- SCRIPTS  
CREATE INDEX contact_Title_trgm_gin ON contact USING gin ("title" gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_Name_trgm_gin ON contact USING gin ("name" gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_FirstName_trgm_gin ON contact USING gin ("firstname" gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_LastName_trgm_gin ON contact USING gin ("lastname" gin_trgm_ops);
