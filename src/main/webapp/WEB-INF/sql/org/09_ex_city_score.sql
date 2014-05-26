CREATE TABLE if not exists city_score
 (
  id serial NOT NULL,
  city_world_id bigint,
  score bigint default null,
  city character varying(150),
  region character varying(150),
  country character varying(150),
  latitude double precision,
  longitude double precision,
  CONSTRAINT city_score_pkey PRIMARY KEY (id)
 );

-- SCRIPTS
insert into city_score(city_world_id,city,region,country,latitude, longitude)
select id, city, region, country,latitude, longitude from jss_sys.city_world ;

-- SCRIPTS 
CREATE INDEX contact_earth_distance_idx ON contact USING gist(ll_to_earth("ts2__latitude__c" ,"ts2__longitude__c")); 

-- SCRIPTS 
CREATE INDEX city_score_earth_distance_idx ON city_score USING gist(ll_to_earth("latitude", "longitude")); 

-- SCRIPTS
select count(*) as count from city_score;

-- SCRIPTS
update city_score set score = 
(select count(a.id) as count from contact a 
where earth_box(ll_to_earth(city_score.latitude ,city_score.longitude), 10 * 1609.344) @> ll_to_earth(a."ts2__latitude__c" ,a."ts2__longitude__c") 
and earth_distance(ll_to_earth(city_score.latitude ,city_score.longitude),ll_to_earth(a."ts2__latitude__c" ,a."ts2__longitude__c")) < 10 * 1609 
)where city_score.id between ? and ?;

-- SCRIPTS
drop index if exists city_score_earth_distance_idx;
alter table city_score drop column latitude;
alter table city_score drop column longitude;
