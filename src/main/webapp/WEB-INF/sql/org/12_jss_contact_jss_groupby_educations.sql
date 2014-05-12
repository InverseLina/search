-- SCRIPTS
insert into jss_contact_jss_groupby_educations (jss_contact_id,jss_groupby_educations_id)
select  c.id as jss_contact_id, geducation.id as jss_groupby_educations_id from ts2__education_history__c education 
inner join contact c on education."ts2__contact__c" = c.sfid 
inner join jss_grouped_educations geducation on geducation.name = education."ts2__name__c"
group by c.id, geducation.id

-- SCRIPTS
select count(*) as count from (
select  c.id as jss_contact_id, geducation.id as jss_groupby_educations_id from ts2__education_history__c education 
inner join contact c on education."ts2__contact__c" = c.sfid 
inner join jss_grouped_educations geducation on geducation.name = education."ts2__name__c"
group by c.id, geducation.id) c