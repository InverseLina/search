-- SCRIPTS
INSERT INTO contact_ex(id,resume_tsv) 
select contact.id, to_tsvector('english', contact."ts2__text_resume__c" ) from 
contact left join contact_ex on  contact_ex.id = contact.id where contact_ex.id is null