package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SearchDao {

    static private String QUERY_SELECT = "select distinct ";

    static private String QUERY_COUNT  = "select count (distinct a.id) ";

    private Logger log = Logger.getLogger(SearchDao.class);
    @Inject
    private DBHelper      dbHelper;

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
    
    
    private String renderSearchCondition(Map<String, String> searchValues,String type,String baseTable,String baseTableIns,List values){
        StringBuilder joinTables = new StringBuilder();
        StringBuilder querySql = new StringBuilder();
    	StringBuilder conditions = new StringBuilder();
    	List subValues = new ArrayList();
    	String tableAliases =" contact ";
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
            	   querySql.append(getSearchValueJoinTable(search, values,"contact"));
            	   hasCondition = true;
               }
           	
               JSONArray contacts = JSONArray.fromObject(searchValues.get("contacts"));
               if(contacts.size()>0)
               conditions.append(" AND (1!=1 ");
               for(int i=0,j=contacts.size();i<j;i++){
            	   JSONObject contact = JSONObject.fromObject(contacts.get(i));
            	   conditions.append(" OR (1=1 ");
            	   String value ;
            	  if(contact.containsKey("firstName")){
            		  conditions.append("  and contact.\"FirstName\" = ? ");
            		  value = contact.getString("firstName");
            		  if(!value.contains("%")){
                          value = "%" + value + "%";
                      }
                      subValues.add(value);
                      hasCondition = true;
            	  }
            	  if(contact.containsKey("lastName")){
            		  conditions.append("  and contact.\"LastName\" = ? ");
            		  value = contact.getString("lastName");
            		  if(!value.contains("%")){
                          value = "%" + value + "%";
                      }
                      subValues.add(value);
                      hasCondition = true;
            	  }
            	  if(contact.containsKey("email")){
            		  conditions.append("  and contact.\"Email\" = ? ");
            		  value = contact.getString("email");
            		  if(!value.contains("%")){
                          value = "%" + value + "%";
                      }
                      subValues.add(value);
                      hasCondition = true;
            	  }
            	  if(contact.containsKey("title")){
            		  conditions.append("  and contact.\"Title\" = ? ");
            		  value = contact.getString("title");
            		  if(!value.contains("%")){
                          value = "%" + value + "%";
                      }
                      subValues.add(value);
                      hasCondition = true;
            	  }
            	  conditions.append(" ) ");
               }
               if(contacts.size()>0)
               conditions.append(" ) ");
         
               // add the 'educationNames' filter, and join Education table
               if (searchValues.get("educations") != null && !"".equals(searchValues.get("educations"))) {
            	   String value = searchValues.get("educations");
	                   if (!"Any Education".equals(value)) {
	                	   value = value.substring(1,value.length()-1);
                		   querySql.append(" join (select ed.\"ts2__Contact__c\" from ts2__education_history__c ed where \"ts2__Name__c\" in ");
                		   querySql.append("("+Joiner.on(",").join(Splitter.on(",").omitEmptyStrings().split(value))+")");
                	   }
	                   querySql.append(" ) ed1 on contact.\"sfId\" = ed1.\"ts2__Contact__c\" ");
	                   hasCondition = true;
               }
               
               
               String joinEmploymentForTitle = "";
               String joinEmploymentForCompanyName = "";
               boolean searchForCompany = (joinTables.indexOf("ts2__employment_history__c")!=-1||baseTable.indexOf("ts2__employment_history__c")!=-1);
              
               
               
               // add the 'companyNames' filter, and join Education table
               if (searchValues.get("employers") != null && !"".equals(searchValues.get("employers"))) {
                   String value = searchValues.get("employers");
                   if (!"Any Company".equals(value)) {
                	   	value = value.substring(1,value.length()-1);
                		joinEmploymentForCompanyName=(" join (select em.\"ts2__Contact__c\",em.\"ts2__Job_Title__c\" from ts2__employment_history__c em where ");
                       	//joinEmploymentForCompanyName+=("('"+Joiner.on("','").join(Splitter.on(",").omitEmptyStrings().split(value))+"')");
                       	Iterator<String> companyNames = Splitter.on(",").omitEmptyStrings().split(value).iterator();
                       	boolean hasCompany = false;
                       	if(companyNames.hasNext()){
                       		joinEmploymentForCompanyName+="( 1!=1 ";
                       		hasCompany = true;
                       	}
                       	while(companyNames.hasNext()){
                       		joinEmploymentForCompanyName+=" or (";
                       		String n = companyNames.next();
                       		String[] companyParams = n.split("\\|");
                       	    joinEmploymentForCompanyName+="  em.\"ts2__Name__c\" = ? ";
                       		values.add(companyParams[0]);
                       		if(companyParams.length>1){
           	        			if(companyParams[1]!=null&&!"".equals(companyParams[1])){
           	            		  joinEmploymentForCompanyName+=" and EXTRACT(year from age(em.\"ts2__Employment_End_Date__c\",em.\"ts2__Employment_Start_Date__c\"))>=? ";
           	            		  values.add(Double.parseDouble(companyParams[1]));
           	        			}
           	        		    if(companyParams.length>2){
           	        		      if(companyParams[2]!=null&&!"".equals(companyParams[2])){
           	            		    joinEmploymentForCompanyName+=" and EXTRACT(year from age(em.\"ts2__Employment_End_Date__c\",em.\"ts2__Employment_Start_Date__c\"))<=? ";
           	            		    values.add(Double.parseDouble(companyParams[2]));
           	        		      }
           	            	    }
                       	   }
                       		joinEmploymentForCompanyName+=")";
                       	}
                       	if(hasCompany){
                       		joinEmploymentForCompanyName+=" ) ";
                       	}
	                   }
	                   searchForCompany = true;
	                   hasCondition = true;
               }
               
              if(searchValues.get("curCompany") != null&&searchForCompany){
            	  joinEmploymentForCompanyName+=(" and em.\"ts2__Employment_End_Date__c\" is  null ");
              }
              if(joinEmploymentForCompanyName.equals("")&&!joinEmploymentForTitle.equals("")){
            	  querySql.append(joinEmploymentForTitle);
              }else if(!joinEmploymentForCompanyName.equals("")){
            	  querySql.append(joinEmploymentForCompanyName);
              }
              
              if(!joinEmploymentForCompanyName.equals("")||!joinEmploymentForTitle.equals("")){
            	  querySql.append(" )  c1 on contact.\"sfId\" = c1.\"ts2__Contact__c\"   ");
              }
              
               // add the 'skillNames' filter, and join Education table
               if (searchValues.get("skills") != null && !"".equals(searchValues.get("skills"))) {
                   String value = searchValues.get("skills");
                   if (!"Any Skill".equals(value)) {
                	   value = value.substring(1,value.length()-1);
            		   querySql.append("join (select sk.\"ts2__Contact__c\" from ts2__skill__c sk where 1=1 ");
                       Iterator<String> skillNames = Splitter.on(",").omitEmptyStrings().split(value).iterator();
                       int i = 0;
                       querySql.append(" and (");
                       while(skillNames.hasNext()){
                           if(i != 0){
                        	   querySql.append(" or ");
                           }
                           querySql.append(" (");
                           String n = skillNames.next();
                           String[] companyParams = n.split("\\|");
                           querySql.append("sk.\"ts2__Skill_Name__c\" = ? ");
                           values.add(companyParams[0]);
                           if(companyParams.length>1){
                               if(companyParams[1]!=null&&!"".equals(companyParams[1])){
                            	 querySql.append(" and sk.\"ts2__Rating__c\" >= ?");
                                 values.add(Double.parseDouble(companyParams[1]));
                               }
                               if(companyParams.length>2){
                                 if(companyParams[2]!=null&&!"".equals(companyParams[2])){
                                   querySql.append(" and sk.\"ts2__Rating__c\" <= ?");
                                   values.add(Double.parseDouble(companyParams[2]));
                                 }
                               }
                          }
                           querySql.append(" )");
                           i++;
                       }
                       querySql.append(" )");
                       querySql.append(" ) sk1 on contact.\"sfId\" = sk1.\"ts2__Contact__c\" ");
                   }
                   hasCondition = true;
               }

               
               boolean hasLocationCondition = false;
               //add the 'radius' filter
               if (searchValues.get("radius") != null && !"".equals(searchValues.get("radius"))) {
               	StringBuilder condition = new StringBuilder();
               	JSONArray locations = JSONArray.fromObject(searchValues.get("locations"));
                if("0".equals(searchValues.get("radius"))){
                   	//add the 'Zip' filter
                	if(locations.size()>0)
                		condition.append(" AND (1!=1 ");
                	 for(int i=0,j=locations.size();i<j;i++){
	                   	String city = (String) locations.get(i);
	                    condition.append(" or zipcode_us.City ilike '"+city+"'" );
	                    hasLocationCondition = true;
                	 }
                	 if(locations.size()>0)
                 		condition.append(" ) ");
                	 
                   if(hasLocationCondition){
                	   List<Map> zipcodes = getZipCode(condition.toString());
                	   if(zipcodes.size()>0){
                		   conditions.append(" and (1!=1 ");
                		   for(Map m:zipcodes){
                			   conditions.append(" or "+tableAliases+".\"MailingPostalCode\" ilike '")
                			   		   .append(m.get("zip"))
                					   .append("%' ");
                		   }
                		   conditions.append(" )");
                	   }else{
                		   conditions.append(" and 1!=1 ");
                	   }
                   }
                   hasCondition = hasLocationCondition||hasCondition;
               
                }else{
                if(locations.size()>0)
                	conditions.append(" AND (1!=1 ");
                for(int i=0,j=locations.size();i<j;i++){
             	   String location = (String) locations.get(i);
	               if (location != null && !"".equals(location)) {
	                 String city =location;
	                 condition.append(" and zipcode_us.City= '"+city+"'" );
	                 hasLocationCondition = true;
	               }
	               Double[] latLong = getLatLong(condition.toString());
	               if(latLong[0]==null||latLong[1]==null|| !hasLocationCondition){
	                 conditions.append(" OR ( 1!=1) ");
	               }else{
	     	          double[] latLongAround = getAround(latLong[0], latLong[1], Double.parseDouble(searchValues.get("radius")));
	     	          conditions.append(" OR (  "+tableAliases+".\"ts2__Latitude__c\" >"+latLongAround[0]);
	     	          conditions.append(" and "+tableAliases+".\"ts2__Latitude__c\" <"+latLongAround[2]);
	     	          conditions.append(" and "+tableAliases+".\"ts2__Longitude__c\" >"+latLongAround[1]);
	     	          conditions.append(" and "+tableAliases+".\"ts2__Longitude__c\" <"+latLongAround[3]+")");
	              }
	               condition = new StringBuilder();
                }
                if(locations.size()>0)
                	conditions.append(" ) ");
                   hasCondition = hasLocationCondition;
                }
               } 
           }
		   querySql.append(" where 1=1 "+conditions);
		   values.addAll(subValues);
    	   if(!hasCondition){
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
     * @param searchMode
     * @return SearchStatements
     */
    private SearchStatements buildSearchStatements(Connection con,String searchColumns, Map<String, String> searchValues,
                                                   Integer pageIdx, Integer pageSize,String orderCon) {
        SearchStatements ss = new SearchStatements();
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
        querySql.append(" from ( select  distinct contact.id,contact.\"Email\",contact.\"sfId\",contact.\"Name\",contact.\"LastName\",contact.\"FirstName\",contact.\"Title\",contact.\"CreatedDate\", case  when contact.\"ts2__Text_Resume__c\" is null  or char_length(contact.\"ts2__Text_Resume__c\") = 0 then -1  else contact.id end as resume  ");
        if(orderCon.contains("Title")){
        	querySql.append(",case   when contact.\"Title\" is null then '' " +
        			        " else lower(contact.\"Title\") END \"lTitle\" ");
        }else if(orderCon.contains("Name")){
        	querySql.append(",lower(contact.\"Name\") as \"lName\" ");
        }else if(orderCon.contains("Email")){
        	querySql.append(",lower(contact.\"Email\") as \"lEmail\" ");
        }
        
        querySql.append(" from contact contact  ");
        countSql.append(" from ( select  contact.id,contact.\"Email\",contact.\"sfId\",contact.\"Name\",contact.\"LastName\",contact.\"FirstName\",contact.\"Title\",contact.\"CreatedDate\" from contact contact   ");
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
	        	countSql.append(join);
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
    	if(Pattern.matches("^.*(Company|Skill|Education)+.*$", orderCon)){
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
        if(!Pattern.matches("^.*(Company|Skill|Education)+.*$", orderCon)){
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
        
        if(type.equals("company")){
            joinSql.append(" left join ts2__employment_history__c c on a.\"sfId\" = c.\"ts2__Contact__c\" and c.\"ts2__Name__c\"!='' ");
        }else if(type.equals("education")){
            joinSql.append(" left join ts2__education_history__c d on a.\"sfId\" = d.\"ts2__Contact__c\" and d.\"ts2__Name__c\"!='' ");
        }else if(type.equals("skill")){
            joinSql.append(" left join ts2__skill__c b on a.\"sfId\" = b.\"ts2__Contact__c\" and b.\"ts2__Skill_Name__c\"!='' ");
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
    
    
    public static String booleanSearchHandler(String searchValue,String type, List values){
    	StringBuilder sb = new StringBuilder();
    	searchValue = searchValue.replaceAll("[\\(\\)%\\^\\@#~\\*]", "").trim();
    	if(searchValue.equals("")){
    		return " select id from contact where 1!=1 ";
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
	    	sb.append("(select 1 as id from contact where 1!=1)");
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
	    	sb.append(" (select 1 from contact limit 1) last on 1=1) ");
    	}else if("NOT".equals(type)){
    		String[] notConditions = searchValue.trim().split("\\s+NOT\\s+");
    		if(notConditions.length==1){
    			sb.append("(");
    		}else{
    			sb.append(" (select n_ext.id from (");
    		}
    		
    		temp = notConditions[0].trim();
			sb.append(" select a_copy.id as id from contact a_copy right join (select ex.id from contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)) b on a_copy.id = b.id " + " union "
                + " select a_copy1.id as id from contact a_copy1 "
                + " where a_copy1.\"Title\" ilike ? or  a_copy1.\"Name\" ilike ? ");
    		
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
                        
	    		sb.append("  (select ex.id from contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)" + " union "
                        + " (select a_copy1.id as id from contact a_copy1 "
                        + " where a_copy1.\"Title\"  ilike ? or  a_copy1.\"Name\"  ilike ? ) ) ");
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
        PreparedStatement s =dbHelper.prepareStatement(con,"select avg(longitude) as longitude,avg(latitude) as latitude from zipcode_us  where 1=1 "+condition);
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
        PreparedStatement s =dbHelper.prepareStatement(con,"select *  from zipcode_us  where 1=1 "+condition);
        List<Map> zip = dbHelper.preparedStatementExecuteQuery(s);
        try{
	        s.close();
	        con.close();
        }catch(Exception e){
        	 throw Throwables.propagate(e);
        }
        return zip;
    }

    public List getTopAdvancedType(Integer offset,Integer size,String type,String min) throws SQLException {
        if(size == null||size<6){
            size = 5;
        }
        offset = offset < 0 ? 0 : offset;
        Connection con = dbHelper.getConnection();
        String name = getNameExpr(type);
        String table = getTable(type);
        StringBuilder querySql =new StringBuilder();
        if("location".equals(type)){
        	querySql.append("select city as name from zipcode_us group by city order by city offset ")
		        	.append( offset)
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
	        querySql.append(" group by e.\"ts2__Contact__c\", e."+name+") a  ").
					 append( " group by a.name order by a.count desc,a.name offset " ).
	                 append( offset).
	                 append( " limit ").
	                 append( size);
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
        }
        return table;
    }
}

