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
    
    public List getTopAdvancedType(Integer offset,Integer size,String type) throws SQLException {
        if(size == null||size<6){
            size = 5;
        }
        offset = offset < 0 ? 0 : offset;
        Connection con = dbHelper.getConnection();
        String name = getNameExpr(type);
        String table = getTable(type);
        StringBuilder querySql = new StringBuilder(" select a.name, count(a.contact) from ( ").
                                append( " select e."+name+" as name, e.\"ts2__Contact__c\" as contact ").
                                append( " from "+table+" e  ").
                                append( " where e."+name+" !='' group by e.\"ts2__Contact__c\", e."+name+") a  ").
                                append( " group by a.name order by a.count desc offset " ).
                                append( offset).
                                append( " limit ").
                                append( size);
        
        PreparedStatement prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        List<Map> result = dbHelper.preparedStatementExecuteQuery(prepareStatement);
        prepareStatement.close();
        con.close();
        return result;
    }
    
    public List<Map> getGroupValuesForAdvanced(Map<String, String> searchValues, String type) throws SQLException {
        //the select query  that will query data
        StringBuilder querySql = new StringBuilder();
        StringBuilder groupBy = new StringBuilder();
        StringBuilder conditions = new StringBuilder();
        StringBuilder searchConditions = new StringBuilder();
        String column = null;
        String baseTableIns = null;
        querySql.append("select result.name, count(*) as count from ( select ");
        boolean hasCondition = false;
        String baseTable = new String();
        List values = new ArrayList();
        StringBuilder joinTables = new StringBuilder();
        
        if(type.equals("company")){
            baseTable = " ts2__employment_history__c ";
            baseTableIns = "c";
            column = " c.\"ts2__Contact__c\" as id, c.\"ts2__Name__c\" as name";
            groupBy.append(" group by c.\"ts2__Contact__c\", c.\"ts2__Name__c\" ");
        }else if(type.equals("education")){
            baseTableIns = "d";
            baseTable = " ts2__education_history__c ";
            groupBy.append(" group by d.\"ts2__Contact__c\", d.\"ts2__Name__c\" ");
            column = " d.\"ts2__Contact__c\" as id, d.\"ts2__Name__c\" as name";
        }else if(type.equals("skill")){
            baseTableIns = "b";
            baseTable = " ts2__skill__c ";
            groupBy.append(" group by b.\"ts2__Contact__c\", b.\"ts2__Skill_Name__c\" ");
            column = " b.\"ts2__Contact__c\" as id, b.\"ts2__Skill_Name__c\" as name";
        }
        
        if (searchValues != null) {
           
            // for all search mode, we preform the same condition
            String search = searchValues.get("search");
            if (!Strings.isNullOrEmpty(search)) {
                joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
                baseTable = " contact ";
                baseTableIns = "a";
                searchConditions.append(getSearchValueJoinTable(search, values,"a"));
            }
        	
          	
              
              
            // add the 'educationNames' filter, and join Education table
            if (searchValues.get("educationNames") != null && !"".equals(searchValues.get("educationNames"))) {
                hasCondition = true;
                String value = searchValues.get("educationNames");
                if (!"Any Education".equals(value)) {
                    if(baseTable.indexOf("ts2__education_history__c") == -1 && joinTables.indexOf("ts2__education_history__c") == -1){
                        joinTables.append(" inner join ts2__education_history__c d on ");
                        if(baseTable.indexOf(" contact ") >= 0){
                            joinTables.append("a.\"sfId\" = d.\"ts2__Contact__c\" ");
                        }else{
                            joinTables.append(baseTableIns+".\"ts2__Contact__c\" = d.\"ts2__Contact__c\" ");
                        }
                    }
                    conditions.append(getConditionForThirdNames(value, values, "education"));
                }
            }

            // add the 'companyNames' filter, and join Education table
            if (searchValues.get("companyNames") != null && !"".equals(searchValues.get("companyNames"))) {
                hasCondition = true;
                String value = searchValues.get("companyNames");
                if (!"Any Company".equals(value)) {
                    if(baseTable.indexOf("ts2__employment_history__c") == -1 && joinTables.indexOf("ts2__employment_history__c") == -1){
                        joinTables.append(" inner join ts2__employment_history__c c on ");
                        if(baseTable.indexOf(" contact ") >= 0){
                            joinTables.append("a.\"sfId\" = c.\"ts2__Contact__c\" ");
                        }else{
                            joinTables.append(baseTableIns+".\"ts2__Contact__c\" = c.\"ts2__Contact__c\" ");
                        }
                    }
                   // conditions.append(getConditionForThirdNames(value, values, "company"));
                    

                	//joinEmploymentForCompanyName+=("('"+Joiner.on("','").join(Splitter.on(",").omitEmptyStrings().split(value))+"')");
                	Iterator<String> companyNames = Splitter.on(",").omitEmptyStrings().split(value).iterator();
                	boolean hasCompany = false;
                	if(companyNames.hasNext()){
                		conditions.append(" and ( 1!=1 ");
                		hasCompany = true;
                	}
                	while(companyNames.hasNext()){
                		conditions.append(" or (");
                		String n = companyNames.next();
                		String[] companyParams = n.split("\\|");
                	    conditions.append("  c.\"ts2__Name__c\" = ? ");
                		values.add(companyParams[0]);
                		if(companyParams.length>1){
    	        			if(companyParams[1]!=null&&!"".equals(companyParams[1])){
    	            		  conditions.append(" and EXTRACT(year from age(c.\"ts2__Employment_End_Date__c\",c.\"ts2__Employment_Start_Date__c\"))>=? ");
    	            		  values.add(Double.parseDouble(companyParams[1]));
    	        			}
    	        		    if(companyParams.length>2){
    	        		      if(companyParams[2]!=null&&!"".equals(companyParams[2])){
    	            		    conditions.append(" and EXTRACT(year from age(c.\"ts2__Employment_End_Date__c\",c.\"ts2__Employment_Start_Date__c\"))<=? ");
    	            		    values.add(Double.parseDouble(companyParams[2]));
    	        		      }
    	            	    }
                	   }
                		conditions.append(")");
                	}
                	if(hasCompany){
                		conditions.append(" ) ");
                	}
                
                }
            }

            // add the 'skillNames' filter, and join Education table
            if (searchValues.get("skillNames") != null && !"".equals(searchValues.get("skillNames"))) {
                hasCondition = true;
                String value = searchValues.get("skillNames");
                if (!"Any Skill".equals(value)) {
                    if(baseTable.indexOf("ts2__skill__c") == -1 && joinTables.indexOf("ts2__skill__c") == -1){
                        joinTables.append(" inner join ts2__skill__c b on ");
                        if(baseTable.indexOf(" contact ") >= 0){
                            joinTables.append("a.\"sfId\" = b.\"ts2__Contact__c\" ");
                        }else{
                            joinTables.append(baseTableIns+".\"ts2__Contact__c\" = b.\"ts2__Contact__c\" ");
                        }
                    }
                    
                    Iterator<String> skillNames = Splitter.on(",").omitEmptyStrings().split(value).iterator();
                    int i = 0;
                    conditions.append(" and (");
                    while(skillNames.hasNext()){
                        if(i!=0){
                            conditions.append(" or ");
                        }
                        conditions.append(" (");
                        String n = skillNames.next();
                        String[] companyParams = n.split("\\|");
                        conditions.append("b.\"ts2__Skill_Name__c\" = ? ");
                        values.add(companyParams[0]);
                        if(companyParams.length>1){
                            if(companyParams[1]!=null&&!"".equals(companyParams[1])){
                              conditions.append(" and b.\"ts2__Rating__c\" >= ?");
                              values.add(Double.parseDouble(companyParams[1]));
                            }
                            if(companyParams.length>2){
                              if(companyParams[2]!=null&&!"".equals(companyParams[2])){
                                conditions.append(" and b.\"ts2__Rating__c\" <= ?");
                                values.add(Double.parseDouble(companyParams[2]));
                              }
                            }
                       }
                        conditions.append(") ");
                        i++;
                    }
                    conditions.append(") ");
                }
            }

          //add the 'Title' filter
            if (searchValues.get("Title") != null && !"".equals(searchValues.get("Title"))) {
                String value = searchValues.get("Title");
                if(searchValues.get("curTitle")!=null){
                	if(baseTable.indexOf("contact") ==-1){
         	            joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
         	            baseTable = " contact ";
         	            baseTableIns = "a";
                     }
                    conditions.append(" and a.\"Title\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                }else{
                     if(baseTable.indexOf("contact") ==-1){
         	            joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
         	            baseTable = " contact ";
         	            baseTableIns = "a";
                     }
                     if(baseTable.indexOf("ts2__employment_history__c") == -1 && joinTables.indexOf("ts2__employment_history__c") == -1){
                    	 joinTables.append(" inner join ts2__employment_history__c c1 on a.\"sfId\" =c.\"ts2__Contact__c\" ");
                     }
                     
                    conditions.append(" and ( c.\"ts2__Job_Title__c\" ilike ?  or a.\"Title\" ilike ? )");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    values.add(value);
                    values.add(value);
                }
            }
            
            //add the 'FirstName' filter
            if (searchValues.get("FirstName") != null && !"".equals(searchValues.get("FirstName"))) {
                String value = searchValues.get("FirstName");
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
            }
            
            //add the 'LastName' filter
            if (searchValues.get("LastName") != null && !"".equals(searchValues.get("LastName"))) {
                String value = searchValues.get("LastName");
                if(baseTable.indexOf("contact") ==-1){
     	            joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
     	            baseTable = " contact ";
     	            baseTableIns = "a";
                 }
                
                conditions.append(" and a.\"LastName\" ilike ? ");
                if(!value.contains("%")){
                    value = "%" + value + "%";
                }
                values.add(value);
            }
            
            //add the 'Email' filter
            if (searchValues.get("Email") != null && !"".equals(searchValues.get("Email"))) {
                String value = searchValues.get("Email");
                if(baseTable.indexOf("contact") ==-1){
                    joinTables.append(" inner join "+baseTable+ " "+ baseTableIns + " on "+ baseTableIns+".\"ts2__Contact__c\" = a.\"sfId\" ");
                    baseTable = " contact ";
                    baseTableIns = "a";
                }
                
                
                conditions.append(" and a.\"Email\" ilike ? ");
                if(!value.contains("%")){
                    value = "%" + value + "%";
                }
                values.add(value);
            }
            
            
            
        }
        
        querySql.append(column);
        querySql.append(" from ");
        querySql.append(baseTable);
        querySql.append(baseTableIns);
        querySql.append(searchConditions);
        querySql.append(joinTables);
        
        if(hasCondition){
            String whereStr = " where 1=1 ";
            querySql.append(whereStr);
        }
        
        querySql.append(conditions);
        if(!"".equals(groupBy.toString())){
            querySql.append(groupBy);
        }
        
        querySql.append(") result where result.name != '' group by result.name order by count desc");
        if(log.isDebugEnabled()){
            log.debug(querySql);
        }
        Connection con = dbHelper.getConnection();
        PreparedStatement prepareStatement =   dbHelper.prepareStatement(con,querySql.toString());
        List<Map> result = dbHelper.preparedStatementExecuteQuery(prepareStatement, values.toArray());
        prepareStatement.close();
        con.close();
        return result;
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
    		return " a.\"id\" as resume";
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
    	for(String column:searchColumns.split(",")){
    	if(column.toLowerCase().equals("name")){
    		sb.append("name,");
    	}else if(column.toLowerCase().equals("id")){
    		sb.append("id,");
    	}else if(column.toLowerCase().equals("title")){
    		sb.append("title,lower(title) as \"lTitle\",");
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
        querySql.append(" from ( select  contact.id,contact.\"sfId\",contact.\"Name\",contact.\"LastName\",contact.\"FirstName\",contact.\"Title\",contact.\"CreatedDate\"  ");
        if(orderCon.contains("Title")){
        	
        	
        	
        	querySql.append(",case   when contact.\"Title\" is null then '' " +
        			        " else lower(contact.\"Title\") END \"lTitle\" ");
        }else if(orderCon.contains("Name")){
        	querySql.append(",lower(contact.\"Name\") as \"lName\" ");
        }
        
        querySql.append(" from contact contact  ");
        countSql.append(" from ( select  contact.id,contact.\"sfId\",contact.\"Name\",contact.\"LastName\",contact.\"FirstName\",contact.\"Title\",contact.\"CreatedDate\" from contact contact   ");
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
    	StringBuilder conditions = new StringBuilder();
    	List subValues = new ArrayList();
        if(!Strings.isNullOrEmpty(searchValue)){
	        joinSql.append(getSearchValueJoinTable(searchValue, values,"contact"));
        }
        

        if(searchValues!=null){
        	
        	 //add the 'FirstName' filter
            if (searchValues.get("FirstName") != null && !"".equals(searchValues.get("FirstName"))) {
                String value = searchValues.get("FirstName");
                conditions.append(" and contact.\"FirstName\" ilike ? ");
                if(!value.contains("%")){
                    value = "%" + value + "%";
                }
                subValues.add(value);
            }

            //add the 'LastName' filter
            if (searchValues.get("LastName") != null && !"".equals(searchValues.get("LastName"))) {
                String value = searchValues.get("LastName");
                conditions.append(" and contact.\"LastName\" ilike ? ");
                if(!value.contains("%")){
                    value = "%" + value + "%";
                }
                subValues.add(value);
            }
            
            //add the 'Email' filter
            if (searchValues.get("Email") != null && !"".equals(searchValues.get("Email"))) {
                String value = searchValues.get("Email");
                conditions.append(" and contact.\"Email\" ilike ? ");
                if(!value.contains("%")){
                    value = "%" + value + "%";
                }
                subValues.add(value);
            }
       
            
            String joinEmploymentForTitle = "";
            String joinEmploymentForCompanyName = "";
            //add the 'Title' filter
            if (searchValues.get("Title") != null && !"".equals(searchValues.get("Title"))) {
                String value = searchValues.get("Title");
                if(searchValues.get("curTitle")!=null){

                    conditions.append(" and contact.\"Title\" ilike ? ");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    subValues.add(value);
                }else{
                    
                	joinEmploymentForTitle = " left join (select em.\"ts2__Contact__c\",em.\"ts2__Job_Title__c\" from ts2__employment_history__c em ";
                    conditions.append(" and ( c1.\"ts2__Job_Title__c\" ilike ?  or contact.\"Title\" ilike ? )");
                    if(!value.contains("%")){
                        value = "%" + value + "%";
                    }
                    subValues.add(value);
                    subValues.add(value);
                }
            }
            
            
        	
        //add the 'companyNames' filter, and join Employment table
        if (searchValues.get("companyNames") != null && !"".equals(searchValues.get("companyNames"))) {
            String value = searchValues.get("companyNames");
            if(!"Any Company".equals(value)){
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
        }
        
      //add the 'Company' filter, and join Employment table
        if (searchValues.get("searchCompany") != null && !"".equals(searchValues.get("searchCompany"))) {
             String value = searchValues.get("searchCompany");
             String columnName = " em.\"ts2__Name__c\" ilike ";
             value = "("+columnName+" '%"+Joiner.on("%' OR "+columnName+" '%").join(Splitter.on(",").omitEmptyStrings().split(value.replaceAll("[)(_]", "")))+"%')";
             if(!joinEmploymentForCompanyName.equals("")){
            	 joinEmploymentForCompanyName+=" and   "+value;
             }else{
            	 joinEmploymentForCompanyName=(" join (select em.\"ts2__Contact__c\",em.\"ts2__Job_Title__c\" from ts2__employment_history__c em where 1=1 ");
            	 joinEmploymentForCompanyName+=" and   "+value;
             }

            if(searchValues.get("curCompany") != null){
            	joinEmploymentForCompanyName+=(" and em.\"ts2__Employment_End_Date__c\" is  null ");
            }

            
        }
        
        if(joinEmploymentForCompanyName.equals("")&&!joinEmploymentForTitle.equals("")){
        	 joinSql.append(joinEmploymentForTitle);
        }else if(!joinEmploymentForCompanyName.equals("")){
        	joinSql.append(joinEmploymentForCompanyName);
        }
        
        if(!joinEmploymentForCompanyName.equals("")||!joinEmploymentForTitle.equals("")){
        	joinSql.append(" )  c1 on contact.\"sfId\" = c1.\"ts2__Contact__c\"   ");
        }
        	
        
        
	   //add the 'educationNames' filter, and join Education table
        if (searchValues.get("educationNames") != null && !"".equals(searchValues.get("educationNames"))) {
            String value = searchValues.get("educationNames");
            if(!"Any Education".equals(value)){
                joinSql.append(" join (select ed.\"ts2__Contact__c\" from ts2__education_history__c ed where \"ts2__Name__c\" in ");
                joinSql.append("('"+Joiner.on("','").join(Splitter.on(",").omitEmptyStrings().split(value))+"')");
                joinSql.append(" ) ed1 on contact.\"sfId\" = ed1.\"ts2__Contact__c\" ");
            }
        }
       
        
        //add the 'skillNames' filter, and join Education table
        if (searchValues.get("skillNames") != null && !"".equals(searchValues.get("skillNames"))) {
            String value = searchValues.get("skillNames");
            if(!"Any Skill".equals(value)){
                joinSql.append("join (select sk.\"ts2__Contact__c\" from ts2__skill__c sk where 1=1 ");
                Iterator<String> skillNames = Splitter.on(",").omitEmptyStrings().split(value).iterator();
                int i = 0;
                joinSql.append(" and (");
                while(skillNames.hasNext()){
                    if(i != 0){
                        joinSql.append(" or ");
                    }
                    joinSql.append(" (");
                    String n = skillNames.next();
                    String[] companyParams = n.split("\\|");
                    joinSql.append("sk.\"ts2__Skill_Name__c\" = ? ");
                    values.add(companyParams[0]);
                    if(companyParams.length>1){
                        if(companyParams[1]!=null&&!"".equals(companyParams[1])){
                          joinSql.append(" and sk.\"ts2__Rating__c\" >= ?");
                          values.add(Double.parseDouble(companyParams[1]));
                        }
                        if(companyParams.length>2){
                          if(companyParams[2]!=null&&!"".equals(companyParams[2])){
                            joinSql.append(" and sk.\"ts2__Rating__c\" <= ?");
                            values.add(Double.parseDouble(companyParams[2]));
                          }
                        }
                   }
                    joinSql.append(" )");
                    i++;
                }
                joinSql.append(" )");
                joinSql.append(" ) sk1 on contact.\"sfId\" = sk1.\"ts2__Contact__c\" ");
	        }
	    }
        
        boolean hasLocationCondition = false;
        //add the 'radius' filter
        if (searchValues.get("radiusFlag")!=null&&
        	searchValues.get("radius") != null && !"".equals(searchValues.get("radius"))) {
        	StringBuilder condition = new StringBuilder();
        	//add the 'Zip' filter
            if (searchValues.get("Zip") != null && !"".equals(searchValues.get("Zip"))) {
            	condition.append(" and zipcode_us.zip= '"+searchValues.get("Zip")+"'" );
            	hasLocationCondition= true;
            }
            
          //add the 'City' filter
            if (searchValues.get("City") != null && !"".equals(searchValues.get("City"))) {
            	String city = searchValues.get("City");
                condition.append(" and zipcode_us.City= '"+city+"'" );
                hasLocationCondition = true;
            }
            
            //add the 'State' filter
            if (searchValues.get("State") != null && !"".equals(searchValues.get("State"))) {
            	String state = searchValues.get("State");
            	condition.append(" and zipcode_us.State= '"+state+"'" );
            	 hasLocationCondition = true;
            }
            
            Double[] latLong = getLatLong(condition.toString());
            if(latLong[0]==null||latLong[1]==null|| !hasLocationCondition){
            	conditions.append(" and 1!=1 ");
            }else{
	            double[] latLongAround = getAround(latLong[0], latLong[1], Double.parseDouble(searchValues.get("radius")));
	            conditions.append(" and contact.\"ts2__Latitude__c\" >"+latLongAround[0]);
	            conditions.append(" and contact.\"ts2__Latitude__c\" <"+latLongAround[2]);
	            conditions.append(" and contact.\"ts2__Longitude__c\" >"+latLongAround[1]);
	            conditions.append(" and contact.\"ts2__Longitude__c\" <"+latLongAround[3]);
            }
           // joinSql.append(" join (select avg(longitude) as longitude,avg(latitude) as latitude from zipcode_us  where 1=1 "+condition+" ) zip on ");
            //joinSql.append(" 6378168*acos(sin(zip.latitude*pi()/180)*sin(contact.\"ts2__Latitude__c\"*pi()/180) + cos(zip.latitude*pi()/180)*cos(contact.\"ts2__Latitude__c\"*pi()/180)*cos((zip.longitude-contact.\"ts2__Longitude__c\")*pi()/180)) ");
            //joinSql.append(" <? ");
            //values.add(Double.parseDouble(searchValues.get("radius")));
            /*if(!searchZip){
            	joinSql.append(" and 1!=1 ");
            }*/
        }
        
        joinSql.append(" where 1=1 "+conditions);
        //make subValues add the last
        values.addAll(subValues);
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
    
//    private String getAdvancedValueJoinTable(String type){
//        StringBuilder joinSql = new StringBuilder();
//        
//        if(type.equals("company")){
//            joinSql.append(" left join ts2__employment_history__c c on  contact.\"sfId\" = c.\"ts2__Contact__c\" ");
//        }else if(type.equals("education")){
//            joinSql.append(" left join ts2__education_history__c d on  contact.\"sfId\" = d.\"ts2__Contact__c\" ");
//        }else if(type.equals("skill")){
//            joinSql.append(" left join ts2__skill__c b on  contact.\"sfId\" = b.\"ts2__Contact__c\" ");
//        }
//        return joinSql.toString();
//    }
    
    private String getSearchValueJoinTable(String searchValue, List values,String alias){
        StringBuilder joinSql = new StringBuilder();
      joinSql.append(" right join (");
      joinSql.append(booleanSearchHandler(searchValue, null, values));
      joinSql.append(")  a_ext on a_ext.id = "+alias+".id ");
      return joinSql.toString();
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
    
    private String getTableInstance(String type){
        String instance = null;
        if(type.equals("company")){
            instance = "c";
        }else if(type.equals("education")){
            instance = "d";
        }else if(type.equals("skill")){
            instance = "b";
        }
        return instance;
    }
    
    private String getConditionForThirdNames(String namesStr, List values, String type){
        StringBuilder conditions = new StringBuilder();
        String instance = getTableInstance(type);
        String nameExpr = getNameExpr(type);
        conditions.append("  and ( "+instance+"."+nameExpr+" in ");
        conditions.append("('"+ Joiner.on("','").join(Splitter.on(",").omitEmptyStrings().split(namesStr))+"')");
        conditions.append(")");
        return conditions.toString();
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
		    		
	    		//}
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
//	    		sb.append("  (select ex.id from contact_ex ex " + " join "
//                        + " (select a_copy1.id as id from contact a_copy1 "
//                        + " where (a_copy1.\"Title\" not ilike ? or a_copy1.\"Title\" is null ) and  (a_copy1.\"Name\" not ilike ? or a_copy1.\"Name\" is null)) a_ext on ex.id=a_ext.id  where ex.resume_tsv @@ to_tsquery(?)) n_extd")
//                        .append(i+" on n_extd"+i+".id=n_ext.id ");
                        
	    		sb.append("  (select ex.id from contact_ex ex where ex.resume_tsv @@ plainto_tsquery(?)" + " union "
                        + " (select a_copy1.id as id from contact a_copy1 "
                        + " where a_copy1.\"Title\"  ilike ? or  a_copy1.\"Name\"  ilike ? ) ) ");
                       // .append(i+" on n_extd"+i+".id=n_ext.id ");
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
    public static double[] getAround(double lat,double lon,Double raidus){  
          
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
    
    
    public Double[] getLatLong(String condition){
    	Double[] latLong = new Double[2];
    	 System.out.println("select avg(longitude) as longitude,avg(latitude) as latitude from zipcode_us  where 1=1 "+condition);
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
    
    public static void main(String[] args) {
	//	System.out.println(getAround(30,30,100000)[1]);
	}
}




class SearchStatements {

    PreparedStatement queryStmt;
    PreparedStatement countStmt;
    Object[]          values;

}
