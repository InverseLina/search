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
	// sin(LatA)*sin(LatB) + cos(LatA)*cos(LatB)*cos(MLonA-MLonB)
	public List<Map> findContactsNearWithoutModule(Double latitude,Double longitude,Double distance) throws SQLException{
		distance *=1609.344;
		StringBuilder querySql = new StringBuilder();
	 	querySql.append("select * from contact a ")
	 	 		.append(" where 6378168*acos(sin(\"ts2__Latitude__c\"*pi()/180)*sin(?*pi()/180) + cos(\"ts2__Latitude__c\"*pi()/180)*cos(?*pi()/180)*cos((\"ts2__Longitude__c\"-?)*pi()/180))<? ");
	 	Connection connection = dbHelper.getConnection();
	 	PreparedStatement paPreparedStatement = connection.prepareStatement(querySql.toString());
	 	List<Map> contacts = dbHelper.preparedStatementExecuteQuery(paPreparedStatement,latitude,latitude,longitude,distance);
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
	 	querySql.append(" select a.* from contact a,(select * from zipcode_us where zip=?) zip  ")
	 			.append(" where 6378168*acos(sin(zip.latitude*pi()/180)*sin(a.\"ts2__Latitude__c\"*pi()/180) + cos(zip.latitude*pi()/180)*cos(a.\"ts2__Latitude__c\"*pi()/180)*cos((zip.longitude-a.\"ts2__Longitude__c\")*pi()/180))")
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
