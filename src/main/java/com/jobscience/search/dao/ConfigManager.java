package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;

import static java.lang.String.format;

@Singleton
public class ConfigManager {

	@Inject
	private DBHelper dbHelper;
    @Inject
    private CurrentOrgHolder orgHolder;

	public void saveOrUpdateConfig(Map<String,String> params) throws SQLException{
		StringBuilder names = new StringBuilder("(");
		StringBuilder sql = new StringBuilder();
		for(String key:params.keySet()){
			names.append("'"+key+"'");
			names.append(",");
//			sql.append("('"+key+"','"+params.get(key)+"'),");
			sql.append(format("(%s, '%s', '%s'),", orgHolder.getId(), key, params.get(key)));
		}
		names.append("'-1')");
		sql.deleteCharAt(sql.length()-1);
		dbHelper.executeUpdate(format("delete from jss_sys.config where org_id = %s and  name in %s", orgHolder.getId(), names));
        dbHelper.executeUpdate(format("insert into  jss_sys.config(org_id, name,value) values %s ", sql));

	}
	
	
	public List<Map> getConfig(String name){
		String sql = "select * from jss_sys.config where org_id = ? " ;
		if(name!=null){
			sql+=" and name='"+name+"'";
		}
		return dbHelper.executeQuery(sql, orgHolder.getId());
		
	}
}
