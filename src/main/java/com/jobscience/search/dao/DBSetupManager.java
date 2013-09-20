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
import java.util.zip.ZipInputStream;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.db.DBHelper;

@Singleton
public class DBSetupManager {

    @Inject
    private DBHelper dbHelper;
    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    @Named("zipcode.path")
    @Inject
    private String zipcodePath;
    public List checkSetupStatus(){
        //TODO: get where we are now.
        return null;
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
					st.addBatch(prefix+"("+line.replaceAll("\"", "\'")+");");
					line = br.readLine();
				}
				st.executeBatch();
		        conn.commit();
			}catch (SQLException e) {
				try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			in.close();
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
}
