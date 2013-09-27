-- SCRIPTS
CREATE INDEX contact_ex_idx_resume_gin
  ON contact_ex
  USING gin
  (resume_tsv);
-- SCRIPTS  
CREATE INDEX contact_Title_trgm_gin ON contact USING gin ("title" public.gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_Name_trgm_gin ON contact USING gin ("name" public.gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_FirstName_trgm_gin ON contact USING gin ("firstname" public.gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_LastName_trgm_gin ON contact USING gin ("lastname" public.gin_trgm_ops);
