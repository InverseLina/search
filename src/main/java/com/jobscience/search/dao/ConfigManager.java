package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.db.DBHelper;

@Singleton
public class ConfigManager {

	@Inject
	private DBHelper dbHelper;
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
		PreparedStatement statement = con.prepareStatement("delete from config where name in "+names);
		System.out.println("delete from config where name in "+names);
		statement.executeUpdate();
		statement.close();
		statement = con.prepareStatement("insert into config(name,value) values "+sql);
		System.out.println("insert into config(name,value) values "+sql);
		statement.executeUpdate();
		statement.close();
		con.close();
	}
	
	
	public List<Map> getConfig(String name){
		String sql = "select * from config";
		if(name!=null){
			sql+=" where name='"+name+"'";
		}
		return dbHelper.executeQuery(sql);		
		
	}
}
