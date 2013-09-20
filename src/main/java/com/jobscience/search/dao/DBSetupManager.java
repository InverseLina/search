package com.jobscience.search.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import javax.swing.RepaintManager;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.db.DBHelper;
import com.jobscience.search.db.DataSourceManager;

@Singleton
public class DBSetupManager {

    @Inject
    private DBHelper dbHelper;
    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    @Named("zipcode.path")
    @Inject
    private String zipcodePath;
    @Inject
    private DataSourceManager dsMng;
    
    private Cache<String, Object> cache= CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
    .maximumSize(100).build(new CacheLoader<String,Object >() {
		@Override
		public Object load(String key) throws Exception {
			
			return key;
	}});
    
    public List checkSetupStatus(){
    	List<SetupStatus> status = new ArrayList<SetupStatus>();
        if(checkSysSchema()){
        	status.add(SetupStatus.SYS_CREATE_SCHEMA);
        }
        if(checkZipcodeImported()){
        	status.add(SetupStatus.SYS_IMPORT_ZIPCODE_DATA);
        }
        return status;
    }
    
    public void createSysSchema(){
        File sysFolder = new File(getRootSqlFolderPath() + "/jss_sys");
        File[] sqlFiles = sysFolder.listFiles();
        List<String> allSqls = new ArrayList();
        for(File file : sqlFiles){
            List subSqlList = loadSQLFile(file);
            allSqls.addAll(subSqlList);
        }
        Connection conn = dbHelper.getConnection();
        try {
            conn.setAutoCommit(false);
            Statement st = conn.createStatement();
            for(String sql : allSqls){
                st.addBatch(sql);
            }
            st.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }
    
    public void updateZipCode(){
    	try{
    		int rowCount = 0;
			URL url = new URL(zipcodePath);
			HttpURLConnection con =  (HttpURLConnection) url.openConnection();
			ZipInputStream in = new ZipInputStream(con.getInputStream());
			System.out.println(in.available());
			in.getNextEntry();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();
			String prefix = "INSERT INTO jss_sys.zipcode_us ("+line+") values ";
			Connection conn = dbHelper.getConnection();
			try{
				Statement st = conn.createStatement();
				conn.setAutoCommit(false);
				line = br.readLine();
				while(line!=null){
					rowCount++;
					if(!line.trim().equals("")){
						st.addBatch(prefix+"("+line.replaceAll("\'", "\'\'").replaceAll("\"", "\'")+");");
					}
					line = br.readLine();
				}
				st.executeBatch();
		        conn.commit();
			}catch (SQLException e) {
				e.printStackTrace();
				try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			in.close();
			cache.put("zipcodeLoadCount", rowCount);
    	}catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private String getRootSqlFolderPath(){
        StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
        path.append("/WEB-INF/sql");
        return path.toString();
    }
    
    private List<String> loadSQLFile(File file){
        List<String> sqlList = new ArrayList<String>();
        StringBuffer temp = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                temp.append(str);
            }
            in.close();
            String sqls[] = temp.toString().split(";");
            for (String sql : sqls) {
                sqlList.add(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sqlList;
    }
    
    private  boolean checkSysSchema(){
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), "select count(*) as count from information_schema.tables" +
        		" where table_schema='jss_sys' and table_type='BASE TABLE' and table_name in ('zipcode_us','org','config')");
    	if(list.size()==1){
    		if("3".equals(list.get(0).get("count").toString())){
    			return true;
    		}
    	}
    	return false;
    }
    
    private boolean checkZipcodeImported(){
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), "select count(*) as count from zipcode_us");
    	if(list.size()==1){
    		if(list.get(0).get("count").toString().equals(cache.getIfPresent("zipcodeLoadCount").toString())){
    			return true;
    		}
    	}
    	return false;
    }
}
