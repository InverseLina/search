-- SCRIPTS
insert into jss_contact_jss_groupby_skills (jss_contact_id,jss_groupby_skills_id,rating)
select  c.id as jss_contact_id, gskill.id as jss_groupby_skills_id, max(skill."ts2__rating__c") from ts2__skill__c skill 
inner join contact c on skill."ts2__contact__c" = c.sfid and c.id between ? and ?
inner join jss_grouped_skills gskill on gskill.name = skill."ts2__skill_name__c"
group by c.id, gskill.id order by c.id

-- SCRIPTS
select sum(count)::bigint as count from jss_grouped_skills