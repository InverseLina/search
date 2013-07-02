package com.jobscience.search.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Throwables;
import com.jobscience.search.db.DBHelper;

@Singleton
public class LocationService {

	@Inject
	private DBHelper      dbHelper;
	
	public List<Map> findContactsNear(Double latitude,Double longitude,Double distance) throws SQLException{
		distance *=1609.344;
	 	StringBuilder querySql = new StringBuilder();
	 	querySql.append("select * from contact a ")
	 	 		.append("where earth_distance(ll_to_earth(?,?), ll_to_earth(a.\"ts2__Latitude__c\",a.\"ts2__Longitude__c\"))")
	 	 		.append(" <=?");
	 	Connection connection = dbHelper.getConnection();
	 	PreparedStatement paPreparedStatement = connection.prepareStatement(querySql.toString());
	 	List<Map> contacts = dbHelper.preparedStatementExecuteQuery(paPreparedStatement,latitude,longitude,distance);
	 	try {
	 	  paPreparedStatement.close();
	 	  connection.close();
	    }catch (SQLException e) {
	      throw Throwables.propagate(e);
	    }
		return contacts;
	}
	
	public List<Map> findContactsNearInPointWay(Double latitude,Double longitude,Double distance) throws SQLException{
	 	StringBuilder querySql = new StringBuilder();
	 	querySql.append("select * from contact a ")
	 	 		.append(" where sec_to_gc(point(?,?) <@> point(\"ts2__Longitude__c\", \"ts2__Latitude__c\"))<? ");
	 	Connection connection = dbHelper.getConnection();
	 	PreparedStatement paPreparedStatement = connection.prepareStatement(querySql.toString());
	 	List<Map> contacts = dbHelper.preparedStatementExecuteQuery(paPreparedStatement,longitude,latitude,distance);
	 	try {
	 	  paPreparedStatement.close();
	 	  connection.close();
	    }catch (SQLException e) {
	      throw Throwables.propagate(e);
	    }
		return contacts;
	}
	
	public List<Map> findContactsNear(String zip,Double distance) throws SQLException{
		distance *=1609.344;
		StringBuilder querySql = new StringBuilder();
	 	querySql.append(" select a.* from contact a,(select * from zcta where zip=?) zip  ")
	 	 		.append(" where earth_distance(ll_to_earth(zip.latitude,zip.longitude), ll_to_earth(a.\"ts2__Latitude__c\",a.\"ts2__Longitude__c\"))")
	 	 		.append(" <=?");
	 	Connection connection = dbHelper.getConnection();
	 	PreparedStatement paPreparedStatement = connection.prepareStatement(querySql.toString());
	 	List<Map> contacts = dbHelper.preparedStatementExecuteQuery(paPreparedStatement,zip,distance);
	 	try {
	 	  paPreparedStatement.close();
	 	  connection.close();
	    }catch (SQLException e) {
	      throw Throwables.propagate(e);
	    }
		return contacts;
	}
	
}
