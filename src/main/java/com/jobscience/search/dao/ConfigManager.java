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
		Connection con = dbHelper.getConnection();
		StringBuilder names = new StringBuilder("(");
		StringBuilder sql = new StringBuilder();
		for(String key:params.keySet()){
			names.append("'"+key+"'");
			names.append(",");
			sql.append("('"+key+"','"+params.get(key)+"'),");
		}
		names.append("'-1')");
		sql.deleteCharAt(sql.length()-1);
		PreparedStatement statement = con.prepareStatement(format("delete from %s.config where name in %s",
                orgHolder.getSchema(), names));
		statement.executeUpdate();
		statement.close();
		statement = con.prepareStatement(format("insert into  %s.config(name,value) values ", orgHolder.getSchema()) +sql);
		statement.executeUpdate();
		statement.close();
		con.close();
	}
	
	
	public List<Map> getConfig(String name){
		String sql = format("select * from %s.config", orgHolder.getSchema());
		if(name!=null){
			sql+=" where name='"+name+"'";
		}
		return dbHelper.executeQuery(sql);		
		
	}
}
