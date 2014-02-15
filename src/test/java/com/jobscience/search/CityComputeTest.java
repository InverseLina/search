package com.jobscience.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipInputStream;

import org.jasql.Runner;
import org.junit.BeforeClass;
import org.junit.Test;

import com.britesnow.snow.testsupport.SnowTestSupport;
import com.jobscience.search.dao.DaoHelper;

public class CityComputeTest extends SnowTestSupport{

    @BeforeClass
    public static void initTestClass() throws Exception {
        SnowTestSupport.initWebApplication("src/main/webapp");
    }
    
    //@Test
    public void computeCity(){
        DaoHelper dh = appInjector.getInstance(DaoHelper.class);
        Runner runner = dh.openNewSysRunner();
        runner.executeUpdate("insert into city(name,longitude,latitude)"
                +" select city,avg(longitude),avg(latitude) from zipcode_us group by city",
                new Object[0]);
        runner.close();
    }
    
    //@Test
    public void importCity() throws Exception{
        try{
            DaoHelper daoHelper = appInjector.getInstance(DaoHelper.class);
            URL url = new URL("https://dl.dropboxusercontent.com/s/2ak2xg3d182t4sj/city.zip?dl=1");
            HttpURLConnection con =  (HttpURLConnection) url.openConnection();
            ZipInputStream in = new ZipInputStream(con.getInputStream());
            in.getNextEntry();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            //To match csv dataType, if columns change should change this
            Runner runner = daoHelper.openNewSysRunner();
            try{
                runner.startTransaction();
                while(line!=null){
                    runner.executeUpdate(line);
                    line = br.readLine();
                }
                runner.commit();
            }catch (Exception e) {
                try {
                    runner.roolback();
                } catch (Exception e1) {
                    e.printStackTrace();
                }
                throw e;
            }finally{
                runner.close();
            }
            in.close();
        }catch (IOException e) {
            throw e;
        }
    
    }
    
    @Test
    public void empty(){
        
    }
}
