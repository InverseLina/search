package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct ";

    static private String QUERY_COUNT  = "select count (distinct a.id) ";

    private Logger log = LoggerFactory.getLogger(SearchDao.class);
    @Inject
    private DBHelper      dbHelper;


    @Inject
    private CurrentOrgHolder orgHolder;

    public SearchResult search(String searchColumns,Map<String, String> searchValues,  Integer pageIdx, Integer pageSize,String orderCon) {
        Connection con = dbHelper.getPublicConnection();
        //builder statements
        SearchStatements statementAndValues = buildSearchStatements(con,searchColumns,searchValues, pageIdx, pageSize,orderCon);

        //excute query and caculate times
        long start = System.currentTimeMillis();
        List<Map> result = dbHelper.preparedStatementExecuteQuery(statementAndValues.queryStmt, statementAndValues.values);
        long mid = System.currentTimeMillis();
        int count = dbHelper.preparedStatementExecuteCount(statementAndValues.countStmt, statementAndValues.values);
        long end = System.currentTimeMillis();

        SearchResult searchResult = new SearchResult(result, count).setDuration(end - start).setSelectDuration(mid - start).setCountDuration(end - mid);
        try {
            statementAndValues.countStmt.close();
            statementAndValues.queryStmt.close();
            con.close();
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
        searchResult.setPageIdx(pageIdx);
        searchResult.setPageSize(pageSize);
        return searchResult;
    }
    
    public List<Map> getGroupValuesForAdvanced(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount,String min,Integer pageSize,Integer pageNum) throws SQLException {
        //the select query  that will query data
        StringBuilder querySql = new StringBuilder();
        StringBuilder groupBy = new StringBuilder();
        String column = null;
        String baseTableIns = null;
        querySql.append("select result.name, count(*) as count from ( select ");
        String baseTable = new String();
        List values = new ArrayList();
        String schemaname = orgHolder.getSchemaName();
        
        if(type.equals("company")){
            baseTable = schemaname+".ts2__employment_history__c ";
            baseTableIns = "c";
            column = " c.\"ts2__contact__c\" as id, c.\"ts2__name__c\" as name";
            groupBy.append(" group by c.\"ts2__contact__c\", c.\"ts2__name__c\" ");
        }else if(type.equals("education")){
            baseTableIns = "d";
            baseTable = schemaname+".ts2__education_history__c ";
            groupBy.append(" group by d.\"ts2__contact__c\", d.\"ts2__name__c\" ");
            column = " d.\"ts2__contact__c\" as id, d.\"ts2__name__c\" as name";
        }else if(type.equals("skill")){
            baseTableIns = "b";
            baseTable = schemaname+".ts2__skill__c ";
            groupBy.append(" group by b.\"ts2__contact__c\", b.\"ts2__skill_name__c\" ");
            column = " b.\"ts2__contact__c\" as id, b.\"ts2__skill_name__c\" as name";
        }else if(type.equals("location")){
            baseTableIns = "z";
            baseTable = " jss_sys.zipcode_us ";
           // groupBy.append(" group by z.\"city\" ");
            column = " z.\"city\" as name ";
        }
        
        querySql.append(column);
        querySql.append(" from ");
            
        querySql.append(renderSearchCondition(searchValues,"advanced",baseTable,baseTableIns,values));
        
        if(min!=null&&!"0".equals(min)){
	        if(type.equals("company")){
	        	 querySql.append("  AND EXTRACT(year from age(c.\"ts2__employment_end_date__c\",c.\"ts2__employment_start_date__c\"))>="+min);
	        }else if(type.equals("education")){
	        	 querySql.append("  AND EXTRACT(year from age(now(),d.\"ts2__graduationdate__c\"))>="+min);
	        }else if(type.equals("skill")){
	        	 querySql.append("  AND b.\"ts2__rating__c\" >="+min);
	        }else if(type.equals("location")){
	        	 querySql.append("   AND  public.earth_distance(public.ll_to_earth(z.\"latitude\",z.\"longitude\"),public.ll_to_earth(a.\"ts2__latitude__c\",a.\"ts2__longitude__c\"))/1000<="+min);
	        }
        }
        if(!"".equals(groupBy.toString())){
            querySql.append(groupBy);
        }

        if(orderByCount){
            querySql.append(") result where result.name != '' and result.name ilike '"+(queryString.length()>2?"%":"")+queryString+"%' group by result.name order by result.count desc offset "+(pageNum-1)*pageSize+" limit "+pageSize);
        }else{
            querySql.append(") result where result.name != '' and result.name ilike '"+(queryString.length()>2?"%":"")+queryString+"%' group by result.name order by result.name offset "+(pageNum-1)*pageSize+" limit "+pageSize);
        }
        if(log.isDebugEnabled()){
            log.debug(querySql.toString());
        }
        Connection con = dbHelper.getPublicConnection();
        PreparedStatement prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        List<Map> result = dbHelper.preparedStatementExecuteQuery(prepareStatement, values.toArray());
        prepareStatement.close();
        con.close();
        return result;
    }
      
    private String renderSearchCondition(Map<String, String> searchValues,String type,String baseTable,String baseTableIns,List values){
        StringBuilder joinTables = new StringBuilder();
        StringBuilder searchConditions = new StringBuilder();
        StringBuilder querySql = new StringBuilder();
    	StringBuilder conditions = new StringBuilder();
    	List subValues = new ArrayList();
    	boolean advanced = "advanced".equals(type);
    	//String tableAliases = advanced?" a ":" contact ";
    	boolean hasCondition = false;
    	 String schemaname = orgHolder.getSchemaName();
    	if(baseTable==null){
    		baseTable = "";
    	}
    	if(baseTableIns==null){
    		baseTableIns = "";
    	}
    	   if (searchValues != null) {
               // for all search mode, we preform the same condition
               String search = searchValues.get("search");
               if (!Strings.isNullOrEmpty(search)) {
            	   if(advanced){
            		   if(baseTableIns.indexOf("z")>-1){
	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
	       				}else{
	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
	       			
	       				}
            		   baseTable =  schemaname+".contact " ;
	                   baseTableIns = "a";
	                   searchConditions.append(getSearchValueJoinTable(search, values,"a"));
            	   }else{
            		   querySql.append(getSearchValueJoinTable(search, values,"contact"));
            	   }
            	   hasCondition = true;
               }
           	   
               JSONArray contacts = JSONArray.fromObject(searchValues.get("contacts"));
               if(contacts.size()>0)
               conditions.append(" AND (1!=1 ");
               for(int i=0,j=contacts.size();i<j;i++){
            	   JSONObject contact = JSONObject.fromObject(contacts.get(i));
            	   conditions.append(" OR (1=1 ");
            	   String value ;
            	  if(contact.containsKey("firstName")&&!"".equals(contact.getString("firstName"))){
            		  value = contact.getString("firstName");
            		  if(advanced){
   	                   if(baseTable.indexOf("contact") ==-1){
	   	                	 if(baseTableIns.indexOf("z")>-1){
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
	 	       				}else{
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
	 	       			
	 	       				}
	   	                	baseTable =  schemaname+".contact " ;
   	        	            baseTableIns = "a";
   	                    }
   	                   conditions.append(" and a.\"firstname\" ilike ? ");
   	                   if(!value.contains("%")){
   	                       value = "%" + value + "%";
   	                   }
   	                   values.add(value);
                      }else{
	            		  conditions.append("  and contact.\"firstname\" ilike ? ");
	            		  if(!value.contains("%")){
	            			  value = "%"+value+"%";	
	              		  }
	                      subValues.add(value);
                      }
	                   hasCondition = true;
            	  }
            	  if(contact.containsKey("lastName")&&!"".equals(contact.getString("lastName"))){
            		  value = contact.getString("lastName");
            		  if(advanced){
   	                   if(baseTable.indexOf("contact") ==-1){
   	                	 if(baseTableIns.indexOf("z")>-1){
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
	 	       				}else{
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
	 	       			
	 	       				}
   	        	            baseTable =  schemaname+".contact " ;
   	        	            baseTableIns = "a";
   	                    }
   	                   
   	                   conditions.append(" and a.\"lastname\" ilike ? ");
   	                   if(!value.contains("%")){
   	                       value = "%" + value + "%";
   	                   }
   	                   values.add(value);
                      }else{
	            		  conditions.append("  and contact.\"lastname\" ilike ? ");
	            		  if(!value.contains("%")){
	            			  value = "%"+value+"%";	
	              		  }
	                      subValues.add(value);
                      }
                      hasCondition = true;
            	  }
            	  if(contact.containsKey("email")&&!"".equals(contact.getString("email"))){
            		  value = contact.getString("email");
            		  if(advanced){
   	                   if(baseTable.indexOf("contact") ==-1){
   	                	 if(baseTableIns.indexOf("z")>-1){
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
	 	       				}else{
	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
	 	       			
	 	       				}
   	                	 	baseTable =  schemaname+".contact " ;
                           baseTableIns = "a";
   	                   }
   	                   conditions.append(" and a.\"email\" ilike ? ");
   	                   if(!value.contains("%")){
   	                       value = "%" + value + "%";
   	                   }
   	                   values.add(value);
                      }else{
	            		  conditions.append("  and contact.\"email\" ilike ? ");
	            		  if(!value.contains("%")){
	            			  value = "%"+value+"%";	
	              		  }
	                      subValues.add(value);
                      }
            		  hasCondition = true;
            	  }
            	  if(contact.containsKey("title")&&!"".equals(contact.getString("title"))){
            		  value = contact.getString("title");
            		  if(advanced){
      	                   if(baseTable.indexOf("contact") ==-1){
	      	                	 if(baseTableIns.indexOf("z")>-1){
	 	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
	 	 	       				}else{
	 	 	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
	 	 	       			
	 	 	       				}
	      	                   baseTable =  schemaname+".contact " ;
                               baseTableIns = "a";
      	                   }
      	                   conditions.append(" and a.\"title\" ilike ? ");
      	                   if(!value.contains("%")){
      	                       value = "%" + value + "%";
      	                   }
      	                   values.add(value);
                         }else{
	            		  conditions.append("  and contact.\"title\" ilike ? ");
	            		  if(!value.contains("%")){
	            			  value = "%"+value+"%";	
	              		  }
	                      subValues.add(value);
                         }
                      hasCondition = true;
            	  }
            	  conditions.append(" ) ");
               }
               
               if(contacts.size()>0)
               conditions.append(" ) ");
         
               // add the 'educations' filter, and join Education table
               if (searchValues.get("educations") != null && !"".equals(searchValues.get("educations"))) {
            	   String value = searchValues.get("educations");
            	   JSONArray educationValues = JSONArray.fromObject(value);
            	   if(educationValues!=null){
            		   if(advanced){
	                       if(baseTable.indexOf("ts2__education_history__c") == -1 && joinTables.indexOf("ts2__education_history__c") == -1){
	                    	   joinTables.append( " inner join    "+schemaname+".ts2__education_history__c d on " );
	                           joinTables.append("a.\"sfid\" = d.\"ts2__contact__c\" ");
	                       }
	                       conditions.append(getConditionForThirdNames(educationValues, "education"));
                	   }else{
	            		   querySql.append( " inner join (select ed.\"ts2__contact__c\" from  "+schemaname+".ts2__education_history__c ed where (1!=1 " );
	            		   for(int i=0,j=educationValues.size();i<j;i++){
	            			   JSONObject educationValue = JSONObject.fromObject(educationValues.get(i));
	            			   querySql.append(" OR ( ed.\"ts2__name__c\" = ")
	            			   		   .append("'"+educationValue.get("name").toString().replaceAll("\'", "\'\'")+"'");
	            			   if(educationValue.containsKey("minYears")){
	            				   Integer minYears = educationValue.getInt("minYears");
	            				   if(!minYears.equals(0)){
	            					   querySql.append(" AND EXTRACT(year from age(now(),ed.\"ts2__graduationdate__c\"))>="+minYears);
	            				   }
	            			   }
	            			   querySql.append(" ) ");		   
	            		   }
		                   querySql.append(") ) ed1 on contact.\"sfid\" = ed1.\"ts2__contact__c\" ");
                	   }
	                   hasCondition = true;
            	   }
               }
               // add the 'companies' filter, and join Education table
               if (searchValues.get("companies") != null && !"".equals(searchValues.get("companies"))) {
                   String value = searchValues.get("companies");
            	   JSONArray companyValues = JSONArray.fromObject(value);
            	   if (companyValues!=null){
	            		   if(advanced){
		            		   if(baseTable.indexOf("ts2__employment_history__c") == -1 && joinTables.indexOf("ts2__employment_history__c") == -1){
		                           joinTables.append( " inner join  "+schemaname+".ts2__employment_history__c c on a.\"sfid\" =c.\"ts2__contact__c\" " );
		                       }
		            		  conditions.append(getConditionForThirdNames(companyValues, "company"));
		            	   }else{
		                	   querySql.append( " join (select em.\"ts2__contact__c\",em.\"ts2__job_title__c\" from   "+schemaname+".ts2__employment_history__c em where (1!=1  " );
		            		   for(int i=0,j=companyValues.size();i<j;i++){
		            			   JSONObject educationValue = JSONObject.fromObject(companyValues.get(i));
		            			   querySql.append(" OR ( em.\"ts2__name__c\" = ")
		            			   		   .append("'"+educationValue.get("name").toString().replaceAll("\'", "\'\'")+"'");
		            			   if(educationValue.containsKey("minYears")){
		            				   Integer minYears = educationValue.getInt("minYears");
		            				   if(!minYears.equals(0)){
		            					   querySql.append(" AND EXTRACT(year from age(em.\"ts2__employment_end_date__c\",em.\"ts2__employment_start_date__c\"))>="+minYears);
		            				   }
		            			   }
		            			   querySql.append(" ) ");		   
		            		   }
		            			querySql.append(" ) ) em1 on contact.\"sfid\" = em1.\"ts2__contact__c\"");
		            	   
		            	   }
	            		   hasCondition = true;
            	   }
               }
               
               // add the 'skillNames' filter, and join Education table
               if (searchValues.get("skills") != null && !"".equals(searchValues.get("skills"))) {
            	   String value = searchValues.get("skills");
            	   JSONArray skillValues = JSONArray.fromObject(value);
            	   if(skillValues!=null){
                	   if(advanced){
	            		   if(baseTable.indexOf("ts2__skill__c") == -1 && joinTables.indexOf("ts2__skill__c") == -1){
	            			   joinTables.append( " inner join  "+schemaname+".ts2__skill__c b on " );
                			   joinTables.append("a.\"sfid\" = b.\"ts2__contact__c\" ");
	                       }
	            		   conditions.append(getConditionForThirdNames(skillValues, "skill"));
	            	   }else{
	                	   querySql.append( " join (select sk.\"ts2__contact__c\" from   "+schemaname+".ts2__skill__c sk where (1!=1  " );
	            		   for(int i=0,j=skillValues.size();i<j;i++){
	            			   JSONObject skillValue = JSONObject.fromObject(skillValues.get(i));
	            			   querySql.append(" OR ( sk.\"ts2__skill_name__c\" = ")
	            			   		   .append("'"+skillValue.get("name").toString().replaceAll("\'", "\'\'")+"'");
	            			   if(skillValue.containsKey("minYears")){
	            				   Integer minYears = skillValue.getInt("minYears");
	            				   if(!minYears.equals(0)){
	            					   querySql.append(" AND sk.\"ts2__rating__c\" >="+minYears);
	            				   }
	            			   }
	            			   querySql.append(" ) ");		   
	            		   }
	            		    querySql.append(" )) sk1 on contact.\"sfid\" = sk1.\"ts2__contact__c\" ");
	            	   }
                	   hasCondition = true;
            	   }
               }
               
              // boolean hasLocationCondition = false;
               //add the 'radius' filter
               if (searchValues.get("locations") != null && !"".equals(searchValues.get("locations"))) {
               	//StringBuilder condition = new StringBuilder();
               	String value = searchValues.get("locations");
         	    JSONArray locationValues = JSONArray.fromObject(value);
	         	    if(locationValues!=null){
		         	    if(advanced){
	                       if(baseTable.indexOf("zipcode_us") == -1 && joinTables.indexOf("zipcode_us") == -1){
	                    	   joinTables.append("  join jss_sys.zipcode_us z on ");
	                           joinTables.append("a.\"mailingpostalcode\" =z.\"zip\"");
	                       }
                            JSONObject ol;
                            conditions.append(" AND¡¡(1!=1  ");
                            for (Object location : locationValues) {
                                ol = (JSONObject) location;
                                String name = (String) ol.get("name");
                                
                                conditions.append(" OR ( z.\"city\"='").append(name).append("'");
                                 if(ol.containsKey("minRadius")){
	            				   double minRadius = ol.getDouble("minRadius");
	            				   conditions.append(" AND  earth_distance(ll_to_earth(z.\"latitude\",z.\"longitude\"),ll_to_earth(a.\"ts2__latitude__c\",a.\"ts2__longitude__c\"))<=").append(minRadius*1000);
                                 }
                                 conditions.append(")");
                            }
                            
                            conditions.append(" ) ");
	                     //  conditions.append(getConditionForThirdNames(locationValues,minRadius, values, "location"));
                	   }else{
                		   querySql.append(" join jss_sys.zipcode_us z on ");
                		   querySql.append(" contact.\"mailingpostalcode\" =z.\"zip\"");
                		   JSONObject ol;
                           conditions.append(" AND (1!=1  ");
                           for (Object location : locationValues) {
                               ol = (JSONObject) location;
                               String name = (String) ol.get("name");
                               
                               conditions.append(" OR ( z.\"city\"='").append(name).append("'");
                                if(ol.containsKey("minRadius")){
	            				   double minRadius = ol.getDouble("minRadius");
	            				   conditions.append(" AND  earth_distance(ll_to_earth(z.\"latitude\",z.\"longitude\"),ll_to_earth(contact.\"ts2__latitude__c\",contact.\"ts2__longitude__c\"))<=").append(minRadius*1000);
                                }
                                conditions.append(")");
                           }
                           
                           conditions.append(" ) ");
                           hasCondition = true;
                		  /* conditions.append(" AND (1!=1 ");
                		   
                		   for(int i=0,j=locationValues.size();i<j;i++){
	            			   JSONObject locationValue = JSONObject.fromObject(locationValues.get(i));
	            			   condition.append(" AND zipcode_us.City = '"+locationValue.get("name")+"'");
	            			   if(locationValue.containsKey("minRadius")){
	            				   double minRadius = locationValue.getDouble("minRadius");
	            				   if(minRadius>0){
	            					   Double[] latLong = getLatLong(condition.toString());
						               if(latLong[0]==null||latLong[1]==null|| !hasLocationCondition){
						                 conditions.append(" OR ( 1!=1) ");
						               }else{
						     	          double[] latLongAround = getAround(latLong[0], latLong[1],minRadius);
						     	          conditions.append(" OR (  "+tableAliases+".\"ts2__latitude__c\" >"+latLongAround[0]);
						     	          conditions.append(" and "+tableAliases+".\"ts2__latitude__c\" <"+latLongAround[2]);
						     	          conditions.append(" and "+tableAliases+".\"ts2__longitude__c\" >"+latLongAround[1]);
						     	          conditions.append(" and "+tableAliases+".\"ts2__longitude__c\" <"+latLongAround[3]+")");
						              }
	            				   }else{
	            					   List<Map> zipcodes = getZipCode(condition.toString());
		            				   for(Map m:zipcodes){
			                			   conditions.append(" OR "+tableAliases+".\"mailingpostalcode\" = '")
			                			   		   .append(m.get("zip"))
			                					   .append("' ");
			                		   }
	            				   }
	            				   hasCondition = true;
	            			   }else{
	            				   List<Map> zipcodes = getZipCode(condition.toString());
	            				   condition = new StringBuilder();
	            				   for(Map m:zipcodes){
	            					   hasCondition = true;
		                			   conditions.append(" OR "+tableAliases+".\"mailingpostalcode\" = '")
		                			   		   .append(m.get("zip"))
		                					   .append("' ");
		                		   }
	            			   }
	            		   }
                		   conditions.append(")");*/
                	   }
        	      }
               } 
           }
    	   if(advanced){
    		   if(joinTables.indexOf(" contact ")==-1&&!baseTable.contains("contact")){
    			if(joinTables.indexOf(baseTable)==-1){
    				if(baseTableIns.indexOf("z")>-1){
    					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"zip\" = a.\"mailingpostalcode\" ");
    				}else{
    					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__contact__c\" = a.\"sfid\" ");
    			
    				}
    			}
   	            baseTable =   schemaname+".contact " ;
   	            baseTableIns = "a";
	           }
    		   
	    	   querySql.append(baseTable);
	           querySql.append(baseTableIns);
	           querySql.append(searchConditions);
	           querySql.append(joinTables);
	           querySql.append("  where 1=1 ");
	           querySql.append(conditions);
    	   }else{
    		   querySql.append(" where 1=1 "+conditions);
    		   values.addAll(subValues);
    	   }
    	   if(!hasCondition&&!advanced){
    		   querySql.append(" and 1!=1 ");
    	   }
           return querySql.toString();
    }
    
    private String getQueryColumnName(String orginalName ,List<String> columnJoinTables,StringBuilder groupBy){
    	if(orginalName.toLowerCase().equals("name")){
    		return "lower(a.\"name\") as \"lname\"";
    	}else if(orginalName.toLowerCase().equals("id")){
    		return "";
    	}else if(orginalName.toLowerCase().equals("resume")){
            if(groupBy.length()>0){
                groupBy.append(",");
            }
            groupBy.append("a.resume");
            return " a.resume as resume";
    	}else if(orginalName.toLowerCase().equals("email")){
     		if(groupBy.length()>0){
	     			groupBy.append(",");
     		}
     		groupBy.append("a.\"email\"");
    		return " a.\"email\" as email,lower(a.\"email\") as \"lemail\" ";
    	}else if(orginalName.toLowerCase().equals("title")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"title\"");
    		return "case   when a.\"title\" is null then '' " +
        			        " else a.\"title\" END title ";
    	}else if(orginalName.toLowerCase().equals("createddate")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"createddate\"");
    		return "to_char(a.\"createddate\",'yyyy-mm-dd') as createddate";
    	}else if(orginalName.toLowerCase().equals("company")){
    		columnJoinTables.add(getAdvancedJoinTable("company"));
    		return " case when string_agg(distinct c.\"ts2__name__c\",',') is null then '' else string_agg(distinct c.\"ts2__name__c\",',') end  company";
    	}else if(orginalName.toLowerCase().equals("skill")){
    		columnJoinTables.add(getAdvancedJoinTable("skill"));
    		return "case when string_agg(distinct b.\"ts2__skill_name__c\",',') is null then '' else string_agg(distinct b.\"ts2__skill_name__c\",',') end  skill";
    	}else if(orginalName.toLowerCase().equals("education")){
    		columnJoinTables.add(getAdvancedJoinTable("education"));
    		return " case when string_agg(distinct d.\"ts2__name__c\",',') is null then '' else string_agg(distinct d.\"ts2__name__c\",',') end  education";
    	}else if(orginalName.toLowerCase().equals("location")){
    		columnJoinTables.add(getAdvancedJoinTable("location"));
    		 if(groupBy.length()>0){
                 groupBy.append(",");
             }
             groupBy.append("z.\"city\"");
    		return "  z.\"city\" as location ";
    	}
    	
    	return orginalName;
    }
    
    private String getSearchColumnsForOuter(String searchColumns){
    	StringBuilder sb = new StringBuilder();
    	if(searchColumns==null){
    		sb.append("id,name,lower(name) as \"lname\",email,lower(\"email\") as \"lemail\"lower(title) as \"ltitle\",title ,createddate");
    	}else{
	    	for(String column:searchColumns.split(",")){
		    	if(column.toLowerCase().equals("name")){
		    		sb.append("lower(name) as \"lname\",");
		    	}/*else if(column.toLowerCase().equals("id")){
		    		sb.append("id,");
		    	}*/else if(column.toLowerCase().equals("title")){
		    		sb.append("title,lower(title) as \"ltitle\",");
		    	}else if(column.toLowerCase().equals("email")){
		    		sb.append( " email ,lower(email) as \"lemail\",");
		    	}else if(column.toLowerCase().equals("createddate")){
		    		sb.append("createddate as \"createddate\",");
		    	}else if(column.toLowerCase().equals("company")){
		    		sb.append("company,lower(company) as \"lcompany\",");
		    	}else if(column.toLowerCase().equals("skill")){
		    		sb.append("skill,lower(skill) as \"lskill\",");
		    	}else if(column.toLowerCase().equals("education")){
		    		sb.append("education,lower(education) as \"leducation\",");
		    	}else if(column.toLowerCase().equals("resume")){
		    		sb.append("resume,");
		    	}else if(column.toLowerCase().equals("location")){
		    		sb.append("location as \"location\",");
		    	}else if(column.toLowerCase().equals("contact")){
		    		sb.append("name,lower(name) as \"lname\",");
		    		sb.append("title,lower(title) as \"ltitle\",");
		    		sb.append( " email ,lower(email) as \"lemail\",");
		    	}
	    	}
	    	sb.append("id,name");
    	}
    	sb.append(",sfid,phone");
        return sb.toString();
    }
    
    private String getSearchColumns(String searchColumns,List columnJoinTables,StringBuilder groupBy){
    	StringBuilder columnsSql = new StringBuilder();
    	 if(searchColumns==null){
             columnsSql.append("a.sfid,a.phone,  a.\"id\" as id,a.\"name\" as name,lower(a.\"name\") as lname,case   when a.\"title\" is null then ''  else a.\"title\" end title ,to_char(a.\"createddate\",'yyyy-mm-dd') as createddate");
             groupBy.append(",a.sfid,a.phone, a.\"name\",a.\"title\",a.\"createddate\"");
    	 }else{
    		 String temp = "";
 	        for(String column:searchColumns.split(",")){
 	        	temp = getQueryColumnName(column,columnJoinTables,groupBy);
 	        	if(!temp.trim().equals("")){
	 	            columnsSql.append(temp);
	 	            columnsSql.append(",");
 	        	}
 	        }
 	        columnsSql.append("a.id,a.name,a.sfid,a.phone");
 	        if(groupBy.length()>0){
 	        	groupBy.append(",");
 	        }
 	        groupBy.append("a.name,a.sfid,a.phone");
// 	        columnsSql.deleteCharAt(columnsSql.length()-1);
         }
    	 return columnsSql.toString();
    }
    
    /**
     * @param searchValues
     * @return SearchStatements
     */
    private SearchStatements buildSearchStatements(Connection con,String searchColumns, Map<String, String> searchValues,
                                                   Integer pageIdx, Integer pageSize,String orderCon) {
        SearchStatements ss = new SearchStatements();

        if(pageIdx < 1){
            pageIdx = 1;
        }
        int offset = (pageIdx -1) * pageSize;
        String schemaname = orgHolder.getSchemaName();
        
        //the select query  that will query data
        StringBuilder querySql = new StringBuilder();
        //the count query sql that will query the count of data
        StringBuilder countSql = new StringBuilder();
        // the part of query that build join tables sql
        StringBuilder joinTables = new StringBuilder();
        // the part of query that build join tables sql
        List<String> columnJoinTables = new ArrayList<String>();
        // the part of query that build conditions sql
        StringBuilder conditions = new StringBuilder();
        // the part of query that build group by sql
        StringBuilder groupBy= new StringBuilder("a.\"id\"");
        // the params will be put in sql
        List values = new ArrayList();
        querySql.append("select ");
        querySql.append(getSearchColumnsForOuter(searchColumns));
        querySql.append(" from ( ");
        querySql.append(QUERY_SELECT);
        countSql.append(QUERY_COUNT);
        querySql.append(getSearchColumns(searchColumns,columnJoinTables,groupBy));
        querySql.append(" from ( select  distinct contact.\"mailingpostalcode\",contact.id,contact.\"email\",contact.\"sfid\",contact.\"name\",contact.\"lastname\",contact.\"phone\",contact.\"firstname\",contact.\"title\",contact.\"createddate\", case  when contact.\"ts2__text_resume__c\" is null  or char_length(contact.\"ts2__text_resume__c\") = 0 then -1  else contact.id end as resume  ");
        if(orderCon.contains("title")){
        	querySql.append(",case   when contact.\"title\" is null then '' " +
        			        " else lower(contact.\"title\") END \"ltitle\" ");
        }else if(orderCon.contains("name")){
        	querySql.append(",lower(contact.\"name\") as \"lname\" ");
        }else if(orderCon.contains("email")){
        	querySql.append(",lower(contact.\"email\") as \"lemail\" ");
        }
        
        querySql.append( " from  "+schemaname+".contact contact  " );
        countSql.append( " from ( select  contact.\"mailingpostalcode\",contact.id,contact.\"email\",contact.\"sfid\",contact.\"name\",contact.\"lastname\",contact.\"phone\",contact.\"firstname\",contact.\"title\",contact.\"createddate\" from  "+schemaname+".contact contact   " );
        if(searchValues!=null){
           
            // for all search mode, we preform the same condition
            String search = searchValues.get("search");
            String[] sqls = getCondtion(search, searchValues,values,orderCon,offset,pageSize);
            querySql.append(sqls[0]);
            countSql.append(sqls[1]);
        }
        
        querySql.append(joinTables);
        countSql.append(joinTables);
        for(String join:columnJoinTables){
        	if(!join.equals("No Join")){
	        	querySql.append(join);
	        	//countSql.append(join);
        	}
        }
        
        
        querySql.append(conditions);
        countSql.append(conditions);
        if(!"".equals(groupBy.toString())){
	        querySql.append(" group by "+groupBy);
        }
        
        querySql.append(") b ");
    	if(orderCon!=null&&!"".equals(orderCon)){
    		querySql.append(" order by "+orderCon);
    	}
    	if(Pattern.matches("^.*(Company|Skill|Education|location)+.*$", orderCon)){
        	querySql.append(" offset ").append(offset).append(" limit ").append(pageSize);
        }
        if(log.isDebugEnabled()){
            log.debug(querySql.toString());
            log.debug(countSql.toString());
        }
        
        // build the statement
        ss.queryStmt = dbHelper.prepareStatement(con,querySql.toString());
        ss.countStmt = dbHelper.prepareStatement(con,countSql.toString());
        ss.values = values.toArray();
        return ss;
    }
    private String[] getCondtion(String searchValue, Map<String, String> searchValues, List values,String orderCon,
    		Integer offset,Integer pageSize){
    	StringBuilder joinSql = new StringBuilder();
    	StringBuilder countSql = new StringBuilder();

        if(searchValues!=null){
        joinSql = new StringBuilder(renderSearchCondition(searchValues,"search",null,null,values));
        //make subValues add the last
       // values.addAll(subValues);
        countSql = new StringBuilder(joinSql.toString());
        if(!Pattern.matches("^.*(Company|Skill|Education|location)+.*$", orderCon)){
        	if(orderCon.contains("resume")){
    			orderCon = orderCon.replace("resume", "id");
    		}
        	if(orderCon!=null&&!"".equals(orderCon)){
        		joinSql.append(" order by "+orderCon);
        	}
        	joinSql.append(" offset ").append(offset).append(" limit ").append(pageSize);
        }
       
        joinSql.append(") a ");
        countSql.append(") a ");
        }
        
       
        return new String[]{joinSql.toString(),countSql.toString()};
    }
    
    private String getAdvancedJoinTable(String type){
        StringBuilder joinSql = new StringBuilder();
        String schemaname = orgHolder.getSchemaName();
        
        if(type.equals("company")){
            joinSql.append( " left join  "+schemaname+".ts2__employment_history__c c on a.\"sfid\" = c.\"ts2__contact__c\" and c.\"ts2__name__c\"!='' " );
        }else if(type.equals("education")){
            joinSql.append( " left join  "+schemaname+".ts2__education_history__c d on a.\"sfid\" = d.\"ts2__contact__c\" and d.\"ts2__name__c\"!='' " );
        }else if(type.equals("skill")){
            joinSql.append( " left join  "+schemaname+".ts2__skill__c b on a.\"sfid\" = b.\"ts2__contact__c\" and b.\"ts2__skill_name__c\"!='' " );
        }else if(type.equals("location")){
            joinSql.append(" left join jss_sys.zipcode_us z on a.\"mailingpostalcode\" = z.\"zip\" ");
        }
        return joinSql.toString();
    }
    
    
    private String getSearchValueJoinTable(String searchValue, List values,String alias){
        StringBuilder joinSql = new StringBuilder();
      joinSql.append(" right join (");
      joinSql.append(booleanSearchHandler(searchValue, null, values));
      joinSql.append(")  a_ext on a_ext.id = "+alias+".id ");
      return joinSql.toString();
    }
    
    
    public  String booleanSearchHandler(String searchValue,String type, List values){
    	 String schemaname = orgHolder.getSchemaName();
    	StringBuilder sb = new StringBuilder();
    	searchValue = searchValue.replaceAll("[\\(\\)%\\^\\@#~\\*]", "").trim();

    	if(searchValue.equals("")){
    		return  " select id from contact where 1!=1 ";
    	}
    	String temp = "";
    	if(type==null||"OR".equals(type)){
	    	String[] orConditions = searchValue.trim().split("\\s+OR\\s+");
	    	for(int i=0;i<orConditions.length;i++){
	    		String orCondition = orConditions[i];
	    		sb.append("select a_extr"+i+".id from (");
	    		sb.append(booleanSearchHandler(orCondition, "AND",values));
	    		sb.append(" a_extr"+i+" union ");
	    	}
	    	sb.append( "(select 1 as id from  "+schemaname+".contact where 1!=1)" );
    	}else if("AND".equals(type)){
    		String[] andConditions = searchValue.trim().split("\\s+AND\\s+");
	    	for(int i=0;i<andConditions.length;i++){
	    		String andCondition = andConditions[i];
	    		//if(!andCondition.trim().equals("")){
	    			if(i==0){
	    				sb.append(" select n_ext0.id as id from ");
	    			}
		    		sb.append(booleanSearchHandler(andCondition, "NOT",values)+(i));
		    		if(i>0){
		    			sb.append(" on n_ext"+i+".id=n_ext"+(i-1)+".id");
		    		}
		    		sb.append(" join ");
	    	}
	    	sb.append( " (select 1 from   "+schemaname+".contact limit 1) last on 1=1) " );
    	}else if("NOT".equals(type)){
    		String[] notConditions = searchValue.trim().split("\\s+NOT\\s+");
    		if(notConditions.length==1){
    			sb.append("(");
    		}else{
    			sb.append(" (select n_ext.id from (");
    		}
    		
    		temp = notConditions[0].trim();

			sb.append( " select a_copy.id as id from   "+schemaname+".contact a_copy right join (select ex.id from   "+schemaname+".contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)) b on a_copy.id = b.id " + " union "
                + " select a_copy1.id as id from   "+schemaname+".contact a_copy1 "
                + " where a_copy1.\"title\" ilike ? or  a_copy1.\"name\" ilike ? "  );
    		
			if(notConditions.length==1){
    			sb.append(") n_ext");
    		}else{
    			sb.append(") n_ext");
    		}
    		
			
    		values.add(temp);
    		if(!temp.contains("%")){
    			temp = "%"+temp+"%";	
    		}
    		values.add(temp);
    		values.add(temp);
    		boolean hasNot = false;
	    	for(int i=1;i<notConditions.length;i++){
	    		hasNot = true;
	    		temp = notConditions[i].trim();
	    		sb.append(" except ");
                        
	    		sb.append("  (select ex.id from  "+schemaname+".contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)" + " union "
                        + " (select a_copy1.id as id from  "+schemaname+".contact a_copy1 "
                        + " where a_copy1.\"title\"  ilike ? or  a_copy1.\"name\"  ilike ? ) ) ");
	    		values.add(temp);
	    		if(!temp.contains("%")){
	    			values.add("%"+temp+"%");
		    		values.add("%"+temp+"%");
	    		}else{
	    			values.add(temp);
		    		values.add(temp);
	    		}
	    		
	    		//values.add("!"+temp);
	    	}
	    	if(hasNot){
	    		sb.append(")n_ext");
	    	}
    	}
    	
    	return sb.toString();
    }
    
  /*  
    *//** 
     * @param raidus unit meter
     * return minLat,minLng,maxLat,maxLng 
     *//*  
    private  double[] getAround(double lat,double lon,Double raidus){  
          
        Double latitude = lat;  
        Double longitude = lon;  
          
        Double degree = 6378168*2*Math.PI/360.0;//  40065709
        double raidusMile = raidus;  
          
        Double dpmLat = 1/degree;  
        Double radiusLat = dpmLat*raidusMile;  
        Double minLat = latitude - radiusLat;  
        Double maxLat = latitude + radiusLat;  
          
        Double mpdLng = degree*Math.cos(latitude * (Math.PI/180));  
        Double dpmLng = 1 / mpdLng;  
        Double radiusLng = dpmLng*raidusMile;  
        Double minLng = longitude - radiusLng;  
        Double maxLng = longitude + radiusLng;  
        return new double[]{minLat,minLng,maxLat,maxLng};  
    }  
    
    
    private Double[] getLatLong(String condition){
    	Double[] latLong = new Double[2];
    	Connection con = dbHelper.getConnection(orgHolder.getOrgName());
        PreparedStatement s =dbHelper.prepareStatement(con,"select avg(longitude) as longitude,avg(latitude) as latitude from jss_sys.zipcode_us  where 1=1 "+condition);
        List<Map> zip = dbHelper.preparedStatementExecuteQuery(s);
        if(zip.size()>0){
          latLong[0] = (Double) zip.get(0).get("latitude");
          latLong[1] = (Double) zip.get(0).get("longitude");
        }
        try{
	        s.close();
	        con.close();
        }catch(Exception e){
        	 throw Throwables.propagate(e);
        }
        return latLong;
    }
    
    private List<Map> getZipCode(String condition){
    	Connection con = dbHelper.getConnection(orgHolder.getOrgName());
        PreparedStatement s =dbHelper.prepareStatement(con,"select *  from jss_sys.zipcode_us  where 1=1 "+condition);
        List<Map> zip = dbHelper.preparedStatementExecuteQuery(s);
        try{
	        s.close();
	        con.close();
        }catch(Exception e){
        	 throw Throwables.propagate(e);
        }
        return zip;
    }
*/
    public List getTopAdvancedType(Integer offset,Integer size,String type,String keyword,String min) throws SQLException {
        if(size == null||size<8){
            size = 7;
        }
        offset = offset < 0 ? 0 : offset;
        Connection con = dbHelper.getConnection(orgHolder.getOrgName());
        String name = getNameExpr(type);
        String table = getTable(type);
        StringBuilder querySql =new StringBuilder();
        if("location".equals(type)){
        	querySql.append("select city as name from jss_sys.zipcode_us  ");
        	if(keyword!=null&&!"".equals(keyword)){
	        	querySql.append(" where city ilike '%"+keyword+ (keyword.length()>2?"%":"")+"' ");
	        }
		    querySql.append(" group by city order by city offset ").append( offset)
		            .append( " limit ") 
		            .append( size); 
        }else{
	        querySql.append(" select a.name, count(a.contact) from ( ").
	                                append( " select e."+name+" as name, e.\"ts2__contact__c\" as contact ").
	                                append( " from "+table+" e  ").
	                                append( " where e."+name+" !='' ");
	        if(min!=null&&!"".equals(min)){
	        	if("company".equals(type)){
	        	    querySql.append(" AND EXTRACT(year from age(e.\"ts2__employment_end_date__c\",e.\"ts2__employment_start_date__c\"))>="+min);
	        	}else if("education".equals("type")){
	        		querySql.append(" AND EXTRACT(year from age(now(),e.\"ts2__graduationdate__c\"))>="+min);
	        	}else if("skill".equals("type")){
	        		querySql.append(" AND e.\"ts2__rating__c\" >=  "+min);
	        	}
	        }
	        if(keyword!=null&&!"".equals(keyword)){
	        	querySql.append(" AND e."+name+" ilike '%"+keyword+(keyword.length()>2?"%":"")+ "' ");
	        }
	        querySql.append(" group by e.\"ts2__contact__c\", e."+name+") a  ").
					 append(" group by a.name order by a.name offset " ).
	                 append(offset).
	                 append(" limit ").
	                 append(size);
        }
        PreparedStatement prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        List<Map> result = dbHelper.preparedStatementExecuteQuery(prepareStatement);
        prepareStatement.close();
        con.close();
        return result;
    }
    
    private String getNameExpr(String type){
        StringBuilder sql = new StringBuilder();
        
        if(type.equals("company")){
            sql.append("\"ts2__name__c\"");
        }else if(type.equals("education")){
            sql.append("\"ts2__name__c\"");
        }else if(type.equals("skill")){
            sql.append("\"ts2__skill_name__c\"");
        }else if(type.equals("location")){
            sql.append("\"zip\"");
        }
        return sql.toString();
    }
    
    private String getTable(String type){
        String table = null;
        String schemaname = orgHolder.getSchemaName();
        if(type.equals("company")){
            table =  schemaname+".ts2__employment_history__c";
        }else if(type.equals("education")){
            table = schemaname+".ts2__education_history__c";
        }else if(type.equals("skill")){
            table = "ts2__skill__c";
        }else if(type.equals("location")){
        	table = "jss.zipcode_us";
        }
        if (table.equals("zipcode_us")) {
            table = "jss_sys." + table;
        }

        return table;
    }
    private String getConditionForThirdNames(JSONArray values, String type){
        StringBuilder conditions = new StringBuilder();
        String instance = getTableInstance(type);
        String nameExpr = getNameExpr(type);
        conditions.append(" AND (1!=1 ");
        for(int i=0,j=values.size();i<j;i++){
			   JSONObject educationValue = JSONObject.fromObject(values.get(i));
			   conditions.append(" OR ( "+instance+"."+nameExpr+" = ")
			   		   .append("'"+educationValue.get("name")+"' ");
			   if(educationValue.containsKey("minYears")){
				   Integer minYears = educationValue.getInt("minYears");
				   if(!minYears.equals(0)){
					   if(type.equals("education")){
						   conditions.append(" AND EXTRACT(year from age(now(),"+instance+".\"ts2__graduationdate__c\"))>="+minYears);
					   }else if(type.equals("company")){
						   conditions.append(" AND EXTRACT(year from age("+instance+".\"ts2__employment_end_date__c\","+instance+".\"ts2__employment_start_date__c\"))>="+minYears);
					   }else if(type.equals("skill")){
						   conditions.append(" AND "+instance+".\"ts2__rating__c\">="+minYears);
					   }
				   }
			   }
			   conditions.append(" ) ");		   
		   }
        conditions.append(" ) "); 
        return conditions.toString();
    }
    private String getTableInstance(String type){
        String instance = null;
        if(type.equals("company")){
            instance = "c";
        }else if(type.equals("education")){
            instance = "d";
        }else if(type.equals("skill")){
            instance = "b";
        }else if(type.equals("location")){
            instance = "z";
        }
        return instance;
    }
}

class SearchStatements {

    PreparedStatement queryStmt;
    PreparedStatement countStmt;
    Object[]          values;

}
