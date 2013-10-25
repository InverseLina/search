-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'contact_ex_idx_resume_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	   CREATE INDEX contact_ex_idx_resume_gin  ON contact_ex  USING gin(resume_tsv);
	END IF;
	END$$;
-- SCRIPTS  
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'contact_title_trgm_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	  CREATE INDEX contact_title_trgm_gin ON contact USING gin ("title" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'contact_name_trgm_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	  CREATE INDEX contact_name_trgm_gin ON contact USING gin ("name" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'contact_firstname_trgm_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	   CREATE INDEX contact_firstname_trgm_gin ON contact USING gin ("firstname" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'contact_lastname_trgm_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	   CREATE INDEX contact_lastname_trgm_gin ON contact USING gin ("lastname" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'ts2__skill__c_name_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	   CREATE INDEX ts2__skill__c_name_gin  ON ts2__skill__c  USING gin ("ts2__skill_name__c" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'ts2__skill__c_contact_c_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	   CREATE INDEX ts2__skill__c_contact_c_gin ON ts2__skill__c  USING gin ("ts2__contact__c" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'ts2__employment_history__c_contact_c_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	   CREATE INDEX ts2__employment_history__c_contact_c_gin  ON ts2__employment_history__c  USING gin ("ts2__contact__c" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'ts2__employment_history__c_name_c_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	   CREATE INDEX ts2__employment_history__c_name_c_gin  ON ts2__employment_history__c  USING gin ("ts2__name__c" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'ts2__education_history__c_contact_c_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	   CREATE INDEX ts2__education_history__c_contact_c_gin  ON ts2__education_history__c  USING gin ("ts2__contact__c" public.gin_trgm_ops);
	END IF;
	END$$;

-- SCRIPTS
	DO $$
	BEGIN
	IF NOT EXISTS (
	    SELECT 1
	    FROM   pg_class c
	    JOIN   pg_namespace n ON n.oid = c.relnamespace
	    WHERE  c.relname = 'ts2__education_history__c_name_c_gin'
	    AND    n.nspname =   current_schema
	    ) THEN
	  CREATE INDEX ts2__education_history__c_name_c_gin  ON ts2__education_history__c  USING gin ("ts2__name__c" public.gin_trgm_ops);
	END IF;
	END$$;
