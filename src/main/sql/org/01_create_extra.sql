CREATE EXTENSION pg_trgm;


CREATE TABLE contact_ex
(
  id bigint NOT NULL,
  resume_tsv tsvector,
  CONSTRAINT contact_ex_pKey PRIMARY KEY (id )
);

ALTER TABLE contact_ex
  ADD CONSTRAINT fk_contact_ex_contact
    FOREIGN KEY (id)  REFERENCES  contact(id) ON DELETE cascade;
    

CREATE OR REPLACE FUNCTION update_context_ex_resume() RETURNS trigger AS $Body$
BEGIN
IF( TG_OP='INSERT' ) THEN
    insert into contact_ex(id,resume_tsv) values(new.id,to_tsvector('english', new."ts2__text_resume__c" ));
ELSIF (TG_OP = 'UPDATE') THEN
    UPDATE contact_ex SET resume_tsv=to_tsvector('english', new."ts2__text_resume__c" )  where id = new.id;
END IF;
RETURN NEW;
END;
$Body$
LANGUAGE 'plpgsql';


CREATE TRIGGER contact_trg_resume_tsv
  BEFORE INSERT OR UPDATE OF "ts2__text_resume__c"
  ON contact
  FOR EACH ROW
  EXECUTE PROCEDURE update_context_ex_resume();
  
  
DROP TABLE if EXISTS savedsearches;

CREATE TABLE savedsearches
(
  id bigserial NOT NULL,
  user_id bigint,
  name character varying(64) NOT NULL,
  create_date timestamp ,
  update_date timestamp,
  search character varying(255) NOT NULL,
  CONSTRAINT pk_savedsearched PRIMARY KEY (id),
  CONSTRAINT unq_name UNIQUE (name)
);

DROP TABLE if EXISTS "user";
CREATE TABLE "user"
(
  id bigserial NOT NULL,
  sfid character varying(255) NULL,
  ctoken character varying(255) NOT NULL,
  CONSTRAINT pk_user PRIMARY KEY (id)
);

