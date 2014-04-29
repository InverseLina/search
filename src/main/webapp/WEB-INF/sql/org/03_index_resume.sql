-- SCRIPTS
INSERT INTO jss_contact(id,resume_tsv) 
select contact.id, to_tsvector(contact."ts2__text_resume__c" ) from 
contact left join jss_contact on  jss_contact.id = contact.id where jss_contact.id is null
-- SCRIPTS
update jss_contact  set resume_tsv = to_tsvector(coalesce(con."ts2__text_resume__c",' ') )
	from (select c."ts2__text_resume__c",c.id from jss_contact ex join contact c on ex.id=c.id and ex."resume_tsv" is null limit 1000) con
	where jss_contact.id=con.id
