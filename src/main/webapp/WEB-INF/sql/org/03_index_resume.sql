-- SCRIPTS
INSERT INTO jss_contact(id,resume_tsv) 
select contact.id, to_tsvector('english', contact."ts2__text_resume__c" ) from 
contact left join jss_contact on  jss_contact.id = contact.id where jss_contact.id is null