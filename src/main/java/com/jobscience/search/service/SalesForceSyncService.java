package com.jobscience.search.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.SyncDao;
import com.jobscience.search.service.sfsync.BulkManager;
import com.jobscience.search.service.sfsync.MetadataManager;
import com.jobscience.search.service.sfsync.NameResolver;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;

@Singleton
public class SalesForceSyncService {
    
    @Inject
    private SyncDao syncDao;
    @Inject
    private BulkManager bulkManager;
    @Inject
    private MetadataManager metadataManager;
    @Inject
    private NameResolver nameResolver;
    
    //FIXME
    String packageNamespacePrefix = "";

    public List<Map> syncData(String token, String instanceUrl)throws AsyncApiException, ConnectionException, IOException{
        MetadataConnection metadataConnection = metadataManager.getMetadataConnection(token, instanceUrl);
        BulkConnection bulkConnection = bulkManager.getBulkConnection(token, instanceUrl);
//        List objects = getCustomObjectNames(metadataConnection);
//        
//        for(int i = 0; i < objects.size(); i++){
//            System.out.println(objects.get(i));
//        }
//        
//        List tables = syncDao.getTablesByOrg(null);
//        
        List notExistTables = new ArrayList();
//        for(int i = 0; i < tables.size(); i++){
//            boolean isExist = false;
//            for(int j = 0; j < objects.size(); j++){
//                if(tables.get(i).equals(objects.get(j))){
//                    isExist = true;
//                    break;
//                }
//            }
//            if(!isExist){
//                    notExistObjects.add(tables.get(i));
//            }
//        }
//        this.pushTables(metadataConnection, notExistObjects);
        
        // ----------- hardcode ----------//
        String tableName = "ts2__education_history__c";
        Map table = new HashMap();
        table.put("name", tableName);
        notExistTables.add(table);
        this.pushTables(metadataConnection, notExistTables);
        // ----------- /hardcode ----------//
        
        List<Map> result = null;
        for(int i = 0; i < notExistTables.size(); i++){
            Map tableDef = (Map) notExistTables.get(i);
            String tablename = (String) tableDef.get("name");
            this.uploadData(bulkConnection, tablename);
            System.out.println("=========upload success");
            result = this.downloadData(bulkConnection,tablename);
            System.out.println(result);
        }
        
        return result;
    }
    
    public void uploadData(BulkConnection bulkConnection,String tablename) throws AsyncApiException, IOException, ConnectionException{
            String object = nameResolver.escapeName(tablename);
            List headers = syncDao.getFields(tablename);
            List data = syncDao.getData(tablename, 2000, 3000);
            String content = toCSVData(headers, data);
            bulkManager.uploadData(packageNamespacePrefix+object + "__c", content, bulkConnection);
    }
    
    public List<Map> downloadData(BulkConnection bulkConnection, String tablename) {
        String object = nameResolver.escapeName(tablename);
        List columns = syncDao.getFields(tablename);
        List fields = new ArrayList();
        for(int j = 0; j < columns.size(); j++){
            Map columnInfo = (Map) columns.get(j);
            fields.add(nameResolver.escapeName((String) columnInfo.get("name")) + "__c");
        }
        String originData = bulkManager.downloadData(object + "__c", fields, bulkConnection);
        List<Map> result = new ArrayList();
        String[] lines = originData.split("\n");
        String[] headerColumns = lines[0].split(",");
        List fieldNames = new ArrayList();
        for(String headerColumn : headerColumns){
            String column = headerColumn.replaceAll("\"", "").trim();
            column = nameResolver.unencapeName(column);
            fieldNames.add(column);
        }
        
        for(int i = 1; i < lines.length; i++){
            String[] records = lines[i].split("\",\"");
            Map map = new HashMap();
            System.out.println(lines[i]);
            for(int j = 0; j < records.length; j++){
                Map columnDef = (Map) columns.get(j);
//                for(int k = 0; k < columns.size(); k++){
//                    String fieldName = (String) ((Map)columns.get(k)).get("name");
//                    if(fieldName.equals(fieldNames.get(j))){
//                        columnDef = (Map) columns.get(k);
//                        break;
//                    }
//                }
                map.put(fieldNames.get(j), getValueByType(columnDef,records[j].replaceAll("\"", "")));
            }
            result.add(map);
        }
        return result;
    }
    
    
    private void pushTables(MetadataConnection metadataConnection,List tables){
        if(tables.size() == 0){
            return;
        }
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ZipOutputStream out = new ZipOutputStream(byteStream);
            
            
            StringBuilder builder = new StringBuilder();
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            builder.append("<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n");
            
            StringBuilder objs = new StringBuilder();
            String objInfo = null;
            for(int i = 0; i < tables.size(); i++){
                Map tableDef = (Map) tables.get(i);
                String name = nameResolver.escapeName((String) tableDef.get("name"));
                if(i != 0){
                    objs.append(",");
                }
                objs.append(name);
                objInfo = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<CustomObject xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n"
                                        + "    <label>"+name+"</label>\n"
                                        + "    <nameField>\n"
                                        + "        <label>"+name+" Name</label>\n"
                                        + "        <type>Text</type>\n"
                                        + "    </nameField>\n"
                                        + "    <pluralLabel>"+name+"s</pluralLabel>\n"
                                        + "    <sharingModel>ReadWrite</sharingModel>\n"
                                        + "    <deploymentStatus>Deployed</deploymentStatus>\n";
                List columns = syncDao.getFields((String) tableDef.get("name"));
                for(int j = 0; j < columns.size(); j++){
                    Map columnDef = (Map) columns.get(j);
                    objInfo += getColumnTagString(columnDef);
                }
                
                objInfo += "</CustomObject>\n";
                
                
                zipFile(out, "unpackaged/objects/"+name+"__c.object", objInfo);

            }
            
            builder.append("<types>\n");
            builder.append("<members>*</members>\n");
            builder.append("<name>CustomObject</name>\n");
            builder.append("</types>\n");
            
            builder.append("<version>29.0</version>\n");
            builder.append("</Package>\n");
            
            
            zipFile(out, "unpackaged/package.xml", builder.toString());
            
            
            out.close();
            
            byte[] zipBytes = byteStream.toByteArray(); 
            metadataManager.deployMetadataObjects(metadataConnection, zipBytes);
            byteStream.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

//    private List<String> getCustomObjectNames(MetadataConnection metadataConnection) {
//        com.sforce.soap.metadata.Package p = new com.sforce.soap.metadata.Package();
//        p.setVersion(VERSION);
//        
//        PackageTypeMembers ptm = new PackageTypeMembers();
//        ptm.setName("CustomObject");
//        ptm.setMembers(new String[]{"*"});
//        
//        
//        p.setTypes(new PackageTypeMembers[]{ptm});
//        byte[] data = metadataManager.retriveMetadataObjects(metadataConnection, p);
//        
//        List<String> names = new ArrayList();
//        try {
//            ByteArrayInputStream bis = new ByteArrayInputStream(data);
//            ZipInputStream zip = new ZipInputStream(bis);
//            ZipEntry entry = null;
//            while ((entry = zip.getNextEntry()) != null) {
//                if(entry.getName().indexOf("unpackaged/objects/") == 0){
//                    String name = entry.getName();
//                    name = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf(".") );
//                    names.add(name);
//                    zip.closeEntry();
//                }
//            }
//            zip.close();
//            bis.close();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return names;
//    }
    
    private String toCSVData(List<Map> headers, List<Map> data) throws AsyncApiException, IOException{
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for(Map header : headers){
                
            if (i != 0) {
                sb.append(",");
            }
            // FIXME
            sb.append(packageNamespacePrefix + nameResolver.escapeName((String) header.get("name")) + "__c");
            i++;
        }
        sb.append("\n");
        for(Map row : data){
            i = 0;
            for(Map header : headers){
                    
                if (i != 0) {
                    sb.append(",");
                }

                Object o = row.get(header.get("name"));
                if (o == null) {
                    sb.append("#N/A");
                } else {
                    if ("character varying".equals(header.get("type")) || "text".equals(header.get("type"))
                                            || "tsvector".equals(header.get("type"))) {
                        String value = (String) o;
                        value = value.replaceAll("\"", "\"\"");
                        sb.append("\"" + value + "\"");
                    } else if ("timestamp with time zone".equals(header.get("type")) || "timestamp without time zone".equals(header.get("type"))
                                            || "date".equals(header.get("type"))) {
                        Date date = (Date) o;
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                        sb.append("\"" + df.format(date) + "\"");
                    } else {
                        sb.append("\"" + o + "\"");
                    }
                }
                i++;
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    private String getColumnTagString(Map propertyInfo){
        StringBuilder sb = new StringBuilder();
        String name = (String) propertyInfo.get("name");
        sb.append("<fields>\n");
        String dataType = (String) propertyInfo.get("type");
        sb.append("<fullName>"+nameResolver.escapeName(name)+"__c</fullName>");
        sb.append("<label>"+nameResolver.escapeName(name)+"</label>");
        if(dataType.equals("date") || dataType.equals("timestamp with time zone") || dataType.equals("timestamp without time zone")){
            sb.append("<type>DateTime</type>");
        }else if(dataType.equals("integer") || dataType.equals("bigint")){
            sb.append("<type>Number</type>");
            sb.append("<unique>false</unique>");
            sb.append("<scale>0</scale>");
            sb.append("<precision>18</precision>");
        }else if(dataType.equals("double precision")){
            sb.append("<type>Number</type>");
            sb.append("<unique>false</unique>");
            sb.append("<scale>6</scale>");
            sb.append("<precision>18</precision>");
        }else if(dataType.equals("character varying")){
            sb.append("<type>Text</type>");
            sb.append("<unique>false</unique>");
            sb.append("<length>255</length>");
        }else if(dataType.equals("text") || dataType.equals("tsvector")){
            sb.append("<type>LongTextArea</type>");
            sb.append("<visibleLines>3</visibleLines>");
            sb.append("<length>32768</length>");
        }else if(dataType.equals("boolean")){
            sb.append("<type>Checkbox</type>");
            sb.append("<defaultValue>false</defaultValue>");
        }
        
        sb.append("<required>false</required>");
        sb.append("<externalId>false</externalId>");
        sb.append("<trackFeedHistory>false</trackFeedHistory>");
        sb.append("<trackTrending>false</trackTrending>");
        sb.append("</fields>\n");
        return sb.toString();
    }
    
    private Object getValueByType(Map columnDef, String v){
        String dataType = (String) columnDef.get("type");
        if(dataType.equals("date") || dataType.equals("timestamp with time zone") || dataType.equals("timestamp without time zone")){
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                Date d = df.parse(v);
                return d;
            } catch (ParseException e) {
            }
        }else if(dataType.equals("bigint")){
            try{
                Long l = Long.parseLong(v);
                return l;
            } catch (Exception e) {
            }
        }else if(dataType.equals("integer")){
            try{
                Integer i = Integer.parseInt(v);
                return i;
            } catch (Exception e) {
            }
        }else if(dataType.equals("double precision")){
            try{
                Double i = Double.parseDouble(v);
                return i;
            } catch (Exception e) {
            }
        }else if(dataType.equals("character varying") || dataType.equals("text") || dataType.equals("tsvector")){
            return v;
        }else if(dataType.equals("boolean")){
            try{
                Boolean i = Boolean.parseBoolean(v);
                return i;
            } catch (Exception e) {
            }
        }
        
        return null;
    }
    
    private void zipFile(ZipOutputStream out, String path, String value){
        try {
            out.putNextEntry(new ZipEntry(path));
            ByteArrayInputStream stream = new ByteArrayInputStream(value.getBytes());
            int i;  
            while ((i = stream.read()) != -1) {  
                out.write(i);
            } 
            stream.close();
            out.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
