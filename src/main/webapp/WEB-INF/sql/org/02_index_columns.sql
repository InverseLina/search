-- SCRIPTS
CREATE INDEX contact_ex_idx_resume_gin
  ON contact_ex
  USING gin
  (resume_tsv);
-- SCRIPTS  
CREATE INDEX contact_title_trgm_gin ON contact USING gin ("title" public.gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_name_trgm_gin ON contact USING gin ("name" public.gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_firstname_trgm_gin ON contact USING gin ("firstname" public.gin_trgm_ops);
-- SCRIPTS
CREATE INDEX contact_lastname_trgm_gin ON contact USING gin ("lastname" public.gin_trgm_ops);
