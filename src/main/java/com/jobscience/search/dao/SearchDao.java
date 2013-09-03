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

import com.jobscience.search.CurrentOrgHolder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.jobscience.search.db.DBHelper;

import static java.lang.String.format;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct ";

    static private String QUERY_COUNT  = "select count (distinct a.id) ";

    private Logger log = Logger.getLogger(SearchDao.class);
    @Inject
    private DBHelper      dbHelper;

    @Inject
    private CurrentOrgHolder orgHolder;

    public SearchResult search(String searchColumns,Map<String, String> searchValues,  Integer pageIdx, Integer pageSize,String orderCon) {
        Connection con = dbHelper.getConnection();
        
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
    
    public List<Map> getGroupValuesForAdvanced(Map<String, String> searchValues, String type,String queryString,Boolean orderByCount) throws SQLException {
        //the select query  that will query data
        StringBuilder querySql = new StringBuilder();
        StringBuilder groupBy = new StringBuilder();
        String schema = orgHolder.getSchema();
        String column = null;
        String baseTableIns = null;
        querySql.append("select result.name, count(*) as count from ( select ");
        String baseTable = new String();
        List values = new ArrayList();
        
        if(type.equals("company")){
            baseTable = format(" %s.ts2__employment_history__c ", schema);
            baseTableIns = "c";
            column = " c.\"ts2__Contact__c\" as id, c.\"ts2__Name__c\" as name";
            groupBy.append(" group by c.\"ts2__Contact__c\", c.\"ts2__Name__c\" ");
        }else if(type.equals("education")){
            baseTableIns = "d";
            baseTable = format(" %s.ts2__education_history__c ", schema);
            groupBy.append(" group by d.\"ts2__Contact__c\", d.\"ts2__Name__c\" ");
            column = " d.\"ts2__Contact__c\" as id, d.\"ts2__Name__c\" as name";
        }else if(type.equals("skill")){
            baseTableIns = "b";
            baseTable = format(" %s.ts2__skill__c ", schema);
            groupBy.append(" group by b.\"ts2__Contact__c\", b.\"ts2__Skill_Name__c\" ");
            column = " b.\"ts2__Contact__c\" as id, b.\"ts2__Skill_Name__c\" as name";
        }else if(type.equals("location")){
            baseTableIns = "z";
            baseTable = " jss_sys.zipcode_us ";
           // groupBy.append(" group by z.\"city\" ");
            column = " z.\"city\" as name ";
        }
        
        querySql.append(column);
        querySql.append(" from ");
            
        querySql.append(renderSearchCondition(searchValues,"advanced",baseTable,baseTableIns,values));
        if(!"".equals(groupBy.toString())){
            querySql.append(groupBy);
        }
        
        if(orderByCount){
        	querySql.append(") result where result.name != ''  group by result.name order by result.count desc offset 0 limit 5");
        }else{
        	querySql.append(") result where result.name != '' and result.name ilike '%"+queryString+(queryString.length()>2?"%":"")+"' group by result.name order by result.name offset 0 limit 10");
        }
        if(log.isDebugEnabled()){
            log.debug(querySql);
        }
        Connection con = dbHelper.getConnection();
        System.out.println(querySql);
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
        String schema = orgHolder.getSchema();
    	List subValues = new ArrayList();
    	boolean advanced = "advanced".equals(type);
    	String tableAliases = advanced?" a ":" contact ";
    	boolean hasCondition = false;
    	
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
	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"zip\" = a.\"MailingPostalCode\" ");
	       				}else{
	       					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
	       			
	       				}
	                   baseTable = format(" %s.contact ", schema);
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
   	        	            joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
   	        	            baseTable = " contact ";
   	        	            baseTableIns = "a";
   	                    }
   	                   conditions.append(" and a.\"FirstName\" ilike ? ");
   	                   if(!value.contains("%")){
   	                       value = "%" + value + "%";
   	                   }
   	                   values.add(value);
                      }else{
	            		  conditions.append("  and contact.\"FirstName\" ilike ? ");
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
   	        	            joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
   	        	            baseTable = format(" %s.contact ", schema);
   	        	            baseTableIns = "a";
   	                    }
   	                   
   	                   conditions.append(" and a.\"LastName\" ilike ? ");
   	                   if(!value.contains("%")){
   	                       value = "%" + value + "%";
   	                   }
   	                   values.add(value);
                      }else{
	            		  conditions.append("  and contact.\"LastName\" ilike ? ");
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
   	                       joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
                           baseTable = format(" %s.contact ", schema);
                           baseTableIns = "a";
   	                   }
   	                   conditions.append(" and a.\"Email\" ilike ? ");
   	                   if(!value.contains("%")){
   	                       value = "%" + value + "%";
   	                   }
   	                   values.add(value);
                      }else{
	            		  conditions.append("  and contact.\"Email\" ilike ? ");
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
      	                       joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
                               baseTable = format(" %s.contact ", schema);
                               baseTableIns = "a";
      	                   }
      	                   conditions.append(" and a.\"Title\" ilike ? ");
      	                   if(!value.contains("%")){
      	                       value = "%" + value + "%";
      	                   }
      	                   values.add(value);
                         }else{
	            		  conditions.append("  and contact.\"Title\" ilike ? ");
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
            	   JSONObject jObject = JSONObject.fromObject(value);
            	   String educationValues = jObject.getString("values");
            	   String minYears  = null;
            	   if(jObject.containsKey("minYears")){
            	      minYears = jObject.getString("minYears");
            	   }
            	   if(educationValues!=null){
            		   educationValues = educationValues.substring(1,educationValues.length()-1).replaceAll("\"", "\'").replaceAll("\\\\\'", "\"");
                	   if(!educationValues.trim().equals("")){
	            		   if(advanced){
		                       if(baseTable.indexOf("ts2__education_history__c") == -1 && joinTables.indexOf("ts2__education_history__c") == -1){
		                    	   joinTables.append(format(" inner join  %s.ts2__education_history__c d on ", schema));
		                           joinTables.append("a.\"sfId\" = d.\"ts2__Contact__c\" ");
		                       }
		                       conditions.append(getConditionForThirdNames(educationValues,minYears, values, "education"));
	                	   }else{
		            		   querySql.append(format(" inner join (select ed.\"ts2__Contact__c\" from %s.ts2__education_history__c ed where \"ts2__Name__c\" in ", schema));
		            		   querySql.append("("+educationValues+")");
		            		   if(minYears!=null){
			                	   querySql.append(" AND EXTRACT(year from age(now(),ed.\"ts2__GraduationDate__c\"))>="+minYears);
			                   }
			                   querySql.append(" ) ed1 on contact.\"sfId\" = ed1.\"ts2__Contact__c\" ");
	                	   }
		                   hasCondition = true;
                	   }
            	   }
               }
            
               // add the 'companies' filter, and join Education table
               if (searchValues.get("companies") != null && !"".equals(searchValues.get("companies"))) {
                   String value = searchValues.get("companies");
                   JSONObject jObject = JSONObject.fromObject(value);
            	   String companyValues = jObject.getString("values");
            	   String minYears  = null;
            	   if(jObject.containsKey("minYears")){
            	      minYears = jObject.getString("minYears");
            	   }
            	   if (companyValues!=null&&!"Any Company".equals(companyValues)) {
            		   companyValues = companyValues.substring(1,companyValues.length()-1).replaceAll("\"", "\'").replaceAll("\\\\\'", "\"");
            		   if(!companyValues.trim().equals("")){
	            		   if(advanced){
		            		   if(baseTable.indexOf("ts2__employment_history__c") == -1 && joinTables.indexOf("ts2__employment_history__c") == -1){
		                           joinTables.append(format(" inner join %s.ts2__employment_history__c c on a.\"sfId\" =c.\"ts2__Contact__c\" ", schema));
		                       }
		            		   conditions.append(getConditionForThirdNames(companyValues,minYears, values, "company"));
		            	   }else{
		                	   querySql.append(format(" join (select em.\"ts2__Contact__c\",em.\"ts2__Job_Title__c\" from %s.ts2__employment_history__c em where em.\"ts2__Name__c\" in ", schema));
		                	   querySql.append(" ("+companyValues+")");
		                       	if(minYears!=null){
		                       		querySql.append(" AND EXTRACT(year from age(em.\"ts2__Employment_End_Date__c\",em.\"ts2__Employment_Start_Date__c\"))>= "+minYears);
		                   		}
		                       	querySql.append(" ) em1 on contact.\"sfId\" = em1.\"ts2__Contact__c\"");
				           }
	            		   hasCondition = true;
            		   }
            	   }
               }
               
               // add the 'skillNames' filter, and join Education table
               if (searchValues.get("skills") != null && !"".equals(searchValues.get("skills"))) {
                   String value = searchValues.get("skills");
                   JSONObject jObject = JSONObject.fromObject(value);
            	   String skillValues = jObject.getString("values");
            	   String minYears  = null;
            	   if(jObject.containsKey("minYears")){
            	      minYears = jObject.getString("minYears");
            	   }
            	   if(skillValues!=null){
                	   skillValues = skillValues.substring(1,skillValues.length()-1).replaceAll("\"", "\'").replaceAll("\\\\\'", "\"");
                	   if(!skillValues.trim().equals("")){
	                	   if(advanced){
		            		   if(baseTable.indexOf("ts2__skill__c") == -1 && joinTables.indexOf("ts2__skill__c") == -1){
		            			   joinTables.append(format(" inner join %s.ts2__skill__c b on ", schema));
	                			   joinTables.append("a.\"sfId\" = b.\"ts2__Contact__c\" ");
		                       }
		            		   conditions.append(getConditionForThirdNames(skillValues,minYears, values, "skill"));
		            	   }else{
	                	   querySql.append(format("join (select sk.\"ts2__Contact__c\" from %s.ts2__skill__c sk where sk.\"ts2__Skill_Name__c\" in ", schema));
	            		   querySql.append(" ("+skillValues+")");
	            		   if(minYears!=null){
	            			   querySql.append(" AND sk.\"ts2__Rating__c\" >=  "+minYears);
	            		   }
	                       querySql.append(" ) sk1 on contact.\"sfId\" = sk1.\"ts2__Contact__c\" ");
		            	   }
	                	   hasCondition = true;
                	   }
            	   }
               }

               
               boolean hasLocationCondition = false;
               //add the 'radius' filter
               if (searchValues.get("locations") != null && !"".equals(searchValues.get("locations"))) {
               	StringBuilder condition = new StringBuilder();
               	String value = searchValues.get("locations");
                JSONObject jObject = JSONObject.fromObject(value);
         	    String locationValues = jObject.getString("values");
         	    String minRadius  = null;
        	    if(jObject.containsKey("minRadius")){
        	    	minRadius = jObject.getString("minRadius");
        	    }
	         	    if(locationValues!=null){
		         	    locationValues = locationValues.substring(1,locationValues.length()-1).replaceAll("\"", "'");
		         	   if(!locationValues.trim().equals("")){
			         	    if(advanced){
		                       if(baseTable.indexOf("zipcode_us") == -1 && joinTables.indexOf("zipcode_us") == -1){
		                    	   joinTables.append("  join jss_sys.zipcode_us z on ");
		                           joinTables.append("a.\"MailingPostalCode\" =z.\"zip\"");
		                       }
		                       
				               condition.append(" AND zipcode_us.City in ("+locationValues+")");
		                	   List<Map> zipcodes = getZipCode(condition.toString());
		                	   if(zipcodes.size()>0){
		                		   conditions.append(" and (1!=1 ");
		                		   for(Map m:zipcodes){
		                			   conditions.append(" or "+tableAliases+".\"MailingPostalCode\" = '")
		                			   		   .append(m.get("zip"))
		                					   .append("' ");
		                		   }
		                		   conditions.append(" )");
		                	   }else{
		                		   conditions.append(" and 1!=1 ");
		                	   }
		                     //  conditions.append(getConditionForThirdNames(locationValues,minRadius, values, "location"));
	                	   }else{
				         	    if(minRadius==null||"0".equals(minRadius)){
				                   	//add the 'Zip' filter
				                	 if(locationValues.length()>0){
					                	 condition.append(" AND zipcode_us.City in ("+locationValues+")");
					                	 hasLocationCondition = true;
				                	 }
				                   if(hasLocationCondition){
				                	   List<Map> zipcodes = getZipCode(condition.toString());
				                	   if(zipcodes.size()>0){
				                		   conditions.append(" and (1!=1 ");
				                		   for(Map m:zipcodes){
				                			   conditions.append(" or "+tableAliases+".\"MailingPostalCode\" = '")
				                			   		   .append(m.get("zip"))
				                					   .append("' ");
				                		   }
				                		   conditions.append(" )");
				                	   }else{
				                		   conditions.append(" and 1!=1 ");
				                	   }
				                   }
				                   hasCondition = hasLocationCondition||hasCondition;
				               
				                }else{
					                if(locationValues.length()>0)
					                	conditions.append(" AND (1!=1 ");
					                for(String location:locationValues.split(",")){
						               if (location != null && !"".equals(location)) {
						                 String city =location.replaceAll("\'", "").replaceAll("\"", "");
						                 condition.append(" and zipcode_us.City= '"+city+"'" );
						                 hasLocationCondition = true;
						               }
						               Double[] latLong = getLatLong(condition.toString());
						               if(latLong[0]==null||latLong[1]==null|| !hasLocationCondition){
						                 conditions.append(" OR ( 1!=1) ");
						               }else{
						     	          double[] latLongAround = getAround(latLong[0], latLong[1], Double.parseDouble(minRadius));
						     	          conditions.append(" OR (  "+tableAliases+".\"ts2__Latitude__c\" >"+latLongAround[0]);
						     	          conditions.append(" and "+tableAliases+".\"ts2__Latitude__c\" <"+latLongAround[2]);
						     	          conditions.append(" and "+tableAliases+".\"ts2__Longitude__c\" >"+latLongAround[1]);
						     	          conditions.append(" and "+tableAliases+".\"ts2__Longitude__c\" <"+latLongAround[3]+")");
						              }
						               condition = new StringBuilder();
					                }
					                if(locationValues.length()>0)
					                	conditions.append(" ) ");
					                   hasCondition = hasLocationCondition;
					            }
	                	   }
		         	   }
        	      }
               } 
           }
    	   if(advanced){
    		   if(joinTables.indexOf("contact")==-1&&!baseTable.contains("contact")){
    			if(joinTables.indexOf(baseTable)==-1){
    				if(baseTableIns.indexOf("z")>-1){
    					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"zip\" = a.\"MailingPostalCode\" ");
    				}else{
    					joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
    			
    				}
    			}
   	            baseTable = format(" %s.contact ", schema);
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
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"Name\"");
    		return "a.\"Name\" as Name,lower(a.\"Name\") as \"lName\"";
    	}else if(orginalName.toLowerCase().equals("id")){
    		return " a.\"id\" as id";
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
     		groupBy.append("a.\"Email\"");
    		return " a.\"Email\" as email,lower(a.\"Email\") as \"lEmail\" ";
    	}else if(orginalName.toLowerCase().equals("title")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"Title\"");
    		return "case   when a.\"Title\" is null then '' " +
        			        " else a.\"Title\" END Title ";
    	}else if(orginalName.toLowerCase().equals("createddate")){
    		if(groupBy.length()>0){
    			groupBy.append(",");
    		}
    		groupBy.append("a.\"CreatedDate\"");
    		return "to_char(a.\"CreatedDate\",'yyyy-mm-dd') as CreatedDate";
    	}else if(orginalName.toLowerCase().equals("company")){
    		columnJoinTables.add(getAdvancedJoinTable("company"));
    		return " case when string_agg(distinct c.\"ts2__Name__c\",',') is null then '' else string_agg(distinct c.\"ts2__Name__c\",',') end  Company";
    	}else if(orginalName.toLowerCase().equals("skill")){
    		columnJoinTables.add(getAdvancedJoinTable("skill"));
    		return "case when string_agg(distinct b.\"ts2__Skill_Name__c\",',') is null then '' else string_agg(distinct b.\"ts2__Skill_Name__c\",',') end  Skill";
    	}else if(orginalName.toLowerCase().equals("education")){
    		columnJoinTables.add(getAdvancedJoinTable("education"));
    		return " case when string_agg(distinct d.\"ts2__Name__c\",',') is null then '' else string_agg(distinct d.\"ts2__Name__c\",',') end  Education";
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
    		sb.append("id,name,lower(name) as \"lName\",Email,lower(\"Email\") as \"lEmail\"lower(title) as \"lTitle\",title ,CreatedDate");
    	}else{
	    	for(String column:searchColumns.split(",")){
		    	if(column.toLowerCase().equals("name")){
		    		sb.append("name,lower(name) as \"lName\",");
		    	}else if(column.toLowerCase().equals("id")){
		    		sb.append("id,");
		    	}else if(column.toLowerCase().equals("title")){
		    		sb.append("title,lower(title) as \"lTitle\",");
		    	}else if(column.toLowerCase().equals("email")){
		    		sb.append( " email ,lower(email) as \"lEmail\",");
		    	}else if(column.toLowerCase().equals("createddate")){
		    		sb.append("createddate as \"CreatedDate\",");
		    	}else if(column.toLowerCase().equals("company")){
		    		sb.append("company,lower(company) as \"lCompany\",");
		    	}else if(column.toLowerCase().equals("skill")){
		    		sb.append("skill,lower(skill) as \"lSkill\",");
		    	}else if(column.toLowerCase().equals("education")){
		    		sb.append("education,lower(education) as \"lEducation\",");
		    	}else if(column.toLowerCase().equals("resume")){
		    		sb.append("resume,");
		    	}else if(column.toLowerCase().equals("location")){
		    		sb.append("location as \"location\",");
		    	}else if(column.toLowerCase().equals("contact")){
		    		sb.append("name,lower(name) as \"lName\",");
		    		sb.append("title,lower(title) as \"lTitle\",");
		    		sb.append( " email ,lower(email) as \"lEmail\",");
		    	}
	    	}
	        sb.deleteCharAt(sb.length()-1);
    	}
        return sb.toString();
    }
    
    private String getSearchColumns(String searchColumns,List columnJoinTables,StringBuilder groupBy){
    	StringBuilder columnsSql = new StringBuilder();
    	 if(searchColumns==null){
             columnsSql.append("a.\"id\" as id,a.\"Name\" as Name,lower(a.\"Name\") as lName,case   when a.\"Title\" is null then ''  else a.\"Title\" END Title ,to_char(a.\"CreatedDate\",'yyyy-mm-dd') as CreatedDate");
             groupBy.append(",a.\"Name\",a.\"Title\",a.\"CreatedDate\"");
    	 }else{
 	        for(String column:searchColumns.split(",")){
 	            columnsSql.append(getQueryColumnName(column,columnJoinTables,groupBy));
 	            columnsSql.append(",");
 	        }
 	        columnsSql.deleteCharAt(columnsSql.length()-1);
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
        String schema =orgHolder.getSchema();
        if(pageIdx < 1){
            pageIdx = 1;
        }
        int offset = (pageIdx -1) * pageSize;

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
        querySql.append(" from ( select  distinct contact.\"MailingPostalCode\",contact.id,contact.\"Email\",contact.\"sfId\",contact.\"Name\",contact.\"LastName\",contact.\"FirstName\",contact.\"Title\",contact.\"CreatedDate\", case  when contact.\"ts2__Text_Resume__c\" is null  or char_length(contact.\"ts2__Text_Resume__c\") = 0 then -1  else contact.id end as resume  ");
        if(orderCon.contains("Title")){
        	querySql.append(",case   when contact.\"Title\" is null then '' " +
        			        " else lower(contact.\"Title\") END \"lTitle\" ");
        }else if(orderCon.contains("Name")){
        	querySql.append(",lower(contact.\"Name\") as \"lName\" ");
        }else if(orderCon.contains("Email")){
        	querySql.append(",lower(contact.\"Email\") as \"lEmail\" ");
        }
        
        querySql.append(format(" from %s.contact contact  ", schema));
        countSql.append(format(" from ( select  contact.\"MailingPostalCode\",contact.id,contact.\"Email\",contact.\"sfId\",contact.\"Name\",contact.\"LastName\",contact.\"FirstName\",contact.\"Title\",contact.\"CreatedDate\" from %s.contact contact   ", schema));
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
            log.debug(querySql);
            log.debug(countSql);
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
        String schema = orgHolder.getSchema();
        
        if(type.equals("company")){
            joinSql.append(format(" left join %s.ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" and c.\"ts2__Name__c\"!='' ", schema));
        }else if(type.equals("education")){
            joinSql.append(format(" left join %s.ts2__education_history__c d on a.\"sfId\" = d.\"ts2__Contact__c\" and d.\"ts2__Name__c\"!='' ", schema));
        }else if(type.equals("skill")){
            joinSql.append(format(" left join %s.ts2__skill__c b on a.\"sfId\" = b.\"ts2__Contact__c\" and b.\"ts2__Skill_Name__c\"!='' ", schema));
        }else if(type.equals("location")){
            joinSql.append(" left join jss_sys.zipcode_us z on a.\"MailingPostalCode\" = z.\"zip\" ");
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
    	StringBuilder sb = new StringBuilder();
    	searchValue = searchValue.replaceAll("[\\(\\)%\\^\\@#~\\*]", "").trim();
        String schema = orgHolder.getSchema();
    	if(searchValue.equals("")){
    		return format(" select id from %s.contact where 1!=1 ", schema);
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
	    	sb.append(format("(select 1 as id from %s.contact where 1!=1)", schema));
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
	    	sb.append(format(" (select 1 from %s.contact limit 1) last on 1=1) ", schema));
    	}else if("NOT".equals(type)){
    		String[] notConditions = searchValue.trim().split("\\s+NOT\\s+");
    		if(notConditions.length==1){
    			sb.append("(");
    		}else{
    			sb.append(" (select n_ext.id from (");
    		}
    		
    		temp = notConditions[0].trim();

			sb.append(format(" select a_copy.id as id from %s.contact a_copy right join (select ex.id from %s.contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)) b on a_copy.id = b.id " + " union "
                + " select a_copy1.id as id from %s.contact a_copy1 "
                + " where a_copy1.\"Title\" ilike ? or  a_copy1.\"Name\" ilike ? ", schema, schema,schema));
    		
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
                        
	    		sb.append(format("  (select ex.id from %s.contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)" + " union "
                        + " (select a_copy1.id as id from %s.contact a_copy1 "
                        + " where a_copy1.\"Title\"  ilike ? or  a_copy1.\"Name\"  ilike ? ) ) ", schema,schema));
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
    
    
    /** 
     * @param raidus unit meter
     * return minLat,minLng,maxLat,maxLng 
     */  
    private static double[] getAround(double lat,double lon,Double raidus){  
          
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
    	Connection con = dbHelper.getConnection();
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
    	Connection con = dbHelper.getConnection();
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

    public List getTopAdvancedType(Integer offset,Integer size,String type,String keyword,String min) throws SQLException {
        if(size == null||size<6){
            size = 5;
        }
        offset = offset < 0 ? 0 : offset;
        Connection con = dbHelper.getConnection();
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
	                                append( " select e."+name+" as name, e.\"ts2__Contact__c\" as contact ").
	                                append( " from "+table+" e  ").
	                                append( " where e."+name+" !='' ");
	        if(min!=null&&!"".equals(min)){
	        	if("company".equals(type)){
	        	    querySql.append(" AND EXTRACT(year from age(e.\"ts2__Employment_End_Date__c\",e.\"ts2__Employment_Start_Date__c\"))>="+min);
	        	}else if("education".equals("type")){
	        		querySql.append(" AND EXTRACT(year from age(now(),e.\"ts2__GraduationDate__c\"))>="+min);
	        	}else if("skill".equals("type")){
	        		querySql.append(" AND e.\"ts2__Rating__c\" >=  "+min);
	        	}
	        }
	        if(keyword!=null&&!"".equals(keyword)){
	        	querySql.append(" AND e."+name+" ilike '%"+keyword+(keyword.length()>2?"%":"")+ "' ");
	        }
	        querySql.append(" group by e.\"ts2__Contact__c\", e."+name+") a  ").
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
            sql.append("\"ts2__Name__c\"");
        }else if(type.equals("education")){
            sql.append("\"ts2__Name__c\"");
        }else if(type.equals("skill")){
            sql.append("\"ts2__Skill_Name__c\"");
        }else if(type.equals("location")){
            sql.append("\"zip\"");
        }
        return sql.toString();
    }
    
    private String getTable(String type){
        String table = null;
        
        if(type.equals("company")){
            table = "ts2__employment_history__c";
        }else if(type.equals("education")){
            table = "ts2__education_history__c";
        }else if(type.equals("skill")){
            table = "ts2__skill__c";
        }else if(type.equals("location")){
        	table = "zipcode_us";
        }
        if (table.equals("zipcode_us")) {
            table = "jss_sys." + table;
        }else{
            table = orgHolder.getSchema() + "." + table;
        }
        return table;
    }
    private String getConditionForThirdNames(String namesStr,String minYear, List values, String type){
        StringBuilder conditions = new StringBuilder();
        String instance = getTableInstance(type);
        String nameExpr = getNameExpr(type);
        conditions.append("  and ( "+instance+"."+nameExpr+" in (");
        conditions.append(namesStr);
        conditions.append(") )");
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
