-- SCRIPTS
insert into jss_contact_jss_groupby_employers (jss_contact_id,jss_groupby_employers_id,year)
select  c.id as jss_contact_id, gemployer.id as jss_groupby_employers_id, max(EXTRACT(year from age(employer.ts2__employment_end_date__c,employer.ts2__employment_start_date__c))) from ts2__employment_history__c employer 
inner join contact c on employer."ts2__contact__c" = c.sfid 
inner join jss_grouped_employers gemployer on gemployer.name = employer."ts2__name__c"
group by c.id, gemployer.id

-- SCRIPTS
select count(*) from (select  c.id as jss_contact_id, gemployer.id as jss_groupby_employers_id, max(EXTRACT(year from age(employer.ts2__employment_end_date__c,employer.ts2__employment_start_date__c))) from ts2__employment_history__c employer 
inner join contact c on employer."ts2__contact__c" = c.sfid 
inner join jss_grouped_employers gemployer on gemployer.name = employer."ts2__name__c"
group by c.id, gemployer.id)c
