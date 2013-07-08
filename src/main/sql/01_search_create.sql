CREATE EXTENSION pg_trgm; 

--ALTER TABLE contact disable trigger c5_c5_contact_logtrigger;

CREATE TABLE contact_ex
(
  id bigint NOT NULL,
  resume_tsv tsvector,
  CONSTRAINT contact_ex_pKey PRIMARY KEY (id )
);
ALTER TABLE contact_ex
  ADD CONSTRAINT fk_contact_ex_contact
    FOREIGN KEY (id)  REFERENCES  contact(id) ON DELETE cascade;
    
    
CREATE INDEX contact_ex_idx_resume_gin
  ON contact_ex
  USING gin
  (resume_tsv);
  
INSERT INTO contact_ex(id,resume_tsv) select id, to_tsvector('english', "ts2__Text_Resume__c" ) from contact;


CREATE OR REPLACE FUNCTION update_context_ex_resume() RETURNS trigger AS $Body$
BEGIN
IF( TG_OP='INSERT' ) THEN
    insert into contact_ex(id,resume_tsv) values(new.id,to_tsvector('english', new."ts2__Text_Resume__c" ));
ELSIF (TG_OP = 'UPDATE') THEN
    UPDATE contact_ex SET resume_tsv=to_tsvector('english', new."ts2__Text_Resume__c" )  where id = new.id;
END IF;
RETURN NEW;
END;
$Body$
LANGUAGE 'plpgsql';


CREATE TRIGGER contact_trg_resume_tsv
  BEFORE INSERT OR UPDATE OF "ts2__Text_Resume__c"
  ON contact
  FOR EACH ROW
  EXECUTE PROCEDURE update_context_ex_resume();
  
  
CREATE INDEX contact_Title_trgm_gin ON contact USING gin ("Title" gin_trgm_ops);
CREATE INDEX contact_Name_trgm_gin ON contact USING gin ("Name" gin_trgm_ops);
CREATE INDEX contact_FirstName_trgm_gin ON contact USING gin ("FirstName" gin_trgm_ops);
CREATE INDEX contact_LastName_trgm_gin ON contact USING gin ("LastName" gin_trgm_ops);

  
-- ALTER TABLE contact enable trigger c5_c5_contact_logtrigger;


DROP TABLE if EXISTS zipcode_us;

CREATE TABLE zipcode_us
(
  zip character(5) NOT NULL,
  city character varying(64),
  state character(2),
  latitude double precision,
  longitude double precision,
  timezone integer,
  dst integer,
  CONSTRAINT zcta_pkey PRIMARY KEY (zip)
);
