package com.jobscience.search.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sf.json.JSONObject;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.britesnow.snow.util.JsonUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.SyncDao;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
import com.sforce.async.CSVReader;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

@Singleton
public class SalesForceSyncService {
    private  static final String VERSION = "29.0";
    private  static final String SF_URL = "/services/data/v"+VERSION;
    private static final String SF_SYNC_URL  = "/services/async/"+VERSION;
    private String  metadataServerUrl = null;
    String packageNamespacePrefix = "";
    
    @Inject
    private SyncDao syncDao;

    public void syncData(String token, String instanceUrl)throws AsyncApiException, ConnectionException, IOException{
        MetadataConnection metadataConnection = getMetadataConnection(token, instanceUrl);
        byte[] bytes = getExistObjects(metadataConnection);
        List objects = getCustomObjectNames(bytes);
        
        for(int i = 0; i < objects.size(); i++){
            System.out.println(objects.get(i));
        }
        
        List tables = syncDao.getTablesByOrg(null);
        
        List notExistObjects = new ArrayList();
        for(int i = 0; i < tables.size(); i++){
            boolean isExist = false;
            for(int j = 0; j < objects.size(); j++){
                if(tables.get(i).equals(objects.get(j))){
                    isExist = true;
                    break;
                }
            }
            if(!isExist){
                    notExistObjects.add(tables.get(i));
            }
        }
        this.pushObjects(metadataConnection, notExistObjects);
        
        BulkConnection bulkConnection = getBulkConnection(token, instanceUrl);
        this.pushData(bulkConnection, tables);
        
    }
    
    private void pushData(BulkConnection bulkConnection,List objects) throws AsyncApiException, IOException{
        for (int i = 0; i < objects.size(); i++) {
            Map m = (Map) objects.get(i);
            String tablename = (String) m.get("name");
            String object = filterName(tablename);
            JobInfo job = createJob(packageNamespacePrefix+object + "__c", bulkConnection);
            List headers = syncDao.getFields(tablename);
            List data = syncDao.getData(tablename, 2000, 3000);
            String content = getCSVData(headers, data);
            List<BatchInfo> batchInfoList = createBatchesFromCSVFile(bulkConnection, job, content);
            System.out.println(content);
            sendJob(bulkConnection, job.getId());
            awaitCompletion(bulkConnection, job, batchInfoList);
            checkResults(bulkConnection, job, batchInfoList);
        }
    }
    
    
    private String getCSVData(List<Map> headers, List<Map> data) throws AsyncApiException, IOException{
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for(Map header : headers){
                
            if (i != 0) {
                sb.append(",");
            }
            // FIXME
            sb.append(packageNamespacePrefix + filterName((String) header.get("name")) + "__c");
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
    
    
    private void pushObjects(MetadataConnection metadataConnection,List objects){
        if(objects.size() == 0){
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
            for(int i = 0; i < objects.size(); i++){
                Map tableDef = (Map) objects.get(i);
                String name = filterName((String) tableDef.get("name"));
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
            this.deployMetadataObjects(metadataConnection, zipBytes);
            byteStream.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private byte[] getExistObjects(MetadataConnection metadataConnection){
        com.sforce.soap.metadata.Package p = new com.sforce.soap.metadata.Package();
        p.setVersion(VERSION);
        
        PackageTypeMembers ptm = new PackageTypeMembers();
        ptm.setName("CustomObject");
        ptm.setMembers(new String[]{"*"});
        
        
        p.setTypes(new PackageTypeMembers[]{ptm});
        byte[] bytes = retriveMetadataObjects(metadataConnection, p);
        
        return bytes;
    }
    

    private List<String> getCustomObjectNames(byte[] data) {
        List<String> names = new ArrayList();
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ZipInputStream zip = new ZipInputStream(bis);
            ZipEntry entry = null;
            while ((entry = zip.getNextEntry()) != null) {
                if(entry.getName().indexOf("unpackaged/objects/") == 0){
                    String name = entry.getName();
                    name = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf(".") );
                    names.add(name);
                    zip.closeEntry();
                }
            }
            zip.close();
            bis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return names;
    }
    
    private String filterName(String name){
        String result = name;
        result = "jss_"+result.replaceAll("__", "00");
        result = result.replaceAll("__", "_");
        return result;
    }
    
    private String getColumnTagString(Map propertyInfo){
        StringBuilder sb = new StringBuilder();
        String name = (String) propertyInfo.get("name");
        sb.append("<fields>\n");
        String dataType = (String) propertyInfo.get("type");
        sb.append("<fullName>"+filterName(name)+"__c</fullName>");
        sb.append("<label>"+filterName(name)+"</label>");
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
    
    // ---------- metadata api ---------- //
    private MetadataConnection getMetadataConnection(String token, String instanceUrl) throws ConnectionException {
        String metadataServerUrl = getMetaDataServerUrl(token, instanceUrl);
        final ConnectorConfig config = new ConnectorConfig();
        config.setServiceEndpoint(metadataServerUrl);
        config.setSessionId(token);
        MetadataConnection metadataConnection = null;
        metadataConnection = new MetadataConnection(config);
        return metadataConnection;
    }
    private String getMetaDataServerUrl(String token, String instanceUrl){
        if(metadataServerUrl == null){
            Map map = getloginInfo(token, instanceUrl);
            JSONObject m = (JSONObject) map.get("urls");
            metadataServerUrl = m.getString("metadata");
            metadataServerUrl = metadataServerUrl.replace("{version}", VERSION);
        }
        return metadataServerUrl;
    }
    private Map<String,String> getloginInfo(String token, String instance_url) {
        Map<String,String> result = new HashMap<String,String>();
        //  Get User Info
        OAuthRequest oauth = new OAuthRequest(Verb.GET,instance_url+SF_URL);
        oauth.addHeader("Authorization", "Bearer "+token);
        oauth.addHeader("X-PrettyPrint", "1");
        Response res = oauth.send();
        String body = res.getBody();
        Map opts = JsonUtil.toMapAndList(body);
        
        // it contains all api url
        String identityUrl = opts.get("identity").toString();
        oauth = new OAuthRequest(Verb.GET,identityUrl);
        oauth.addHeader("Authorization", "Bearer "+token);
        oauth.addHeader("X-PrettyPrint", "1");
        result = JsonUtil.toMapAndList(oauth.send().getBody());
        return result;
    }
    
    private void deployMetadataObjects(MetadataConnection metadataConnection, byte[] zipBytes) throws Exception{
        DeployOptions deployOptions = new DeployOptions();
        deployOptions.setPerformRetrieve(false);
        deployOptions.setRollbackOnError(true);
        AsyncResult asyncResult = metadataConnection.deploy(zipBytes, deployOptions);
        DeployResult deployResult = waitForDeployCompletion(asyncResult.getId(),metadataConnection);
        if (!deployResult.isSuccess()) {
            System.out.println("The files were not successfully deployed: "+deployResult.getErrorMessage());
        }
    }
    
    private byte[] retriveMetadataObjects(MetadataConnection metadataConnection, com.sforce.soap.metadata.Package p){
        try{
            
            RetrieveRequest retrieveRequest = new RetrieveRequest();
            retrieveRequest.setApiVersion(new Double(VERSION));
            retrieveRequest.setUnpackaged(p);
            
            AsyncResult asyncResult = metadataConnection.retrieve(retrieveRequest);
            asyncResult = waitForRetrieveCompletion(asyncResult,metadataConnection);
            RetrieveResult retrieveResult =  metadataConnection.checkRetrieveStatus(asyncResult.getId());
            
            StringBuilder stringBuilder = new StringBuilder();
            if (retrieveResult.getMessages() != null) {
                for (RetrieveMessage rm : retrieveResult.getMessages()) {
                    stringBuilder.append(rm.getFileName() + " - " + rm.getProblem() + "\n");
                }
            }
            if (stringBuilder.length() > 0) {
                System.out.println("Retrieve warnings:\n" + stringBuilder);
            }
            for(int i = 0; i < retrieveResult.getFileProperties().length; i++){
                if(retrieveResult.getFileProperties()[i].getNamespacePrefix() != null){
                    packageNamespacePrefix = retrieveResult.getFileProperties()[i].getNamespacePrefix() + "__";
                    break;
                }
            }
            byte[] bs = retrieveResult.getZipFile();
            return bs;
        }catch (Exception e){
            e.printStackTrace();
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
    
    private DeployResult waitForDeployCompletion(String asyncResultId, MetadataConnection metadataConnection) throws Exception {
        int poll = 0;
        long waitTimeMilliSecs = 1000;
        DeployResult deployResult;
        boolean fetchDetails;
        do {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            if (poll++ > 50) {
                throw new Exception("Request timed out. If this is a large set of metadata components, " + "ensure that MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            // Fetch in-progress details once for every 3 polls
            fetchDetails = (poll % 3 == 0);
            deployResult = metadataConnection.checkDeployStatus(asyncResultId, fetchDetails);
            System.out.println("Status is: " + deployResult.getStatus());
            if (!deployResult.isDone() && fetchDetails) {
            }
        } while (!deployResult.isDone());
        if (!deployResult.isSuccess() && deployResult.getErrorStatusCode() != null) {
            throw new Exception(deployResult.getErrorStatusCode() + " msg: " + deployResult.getErrorMessage());
        }
        if (!fetchDetails) {
            // Get the final result with details if we didn't do it in the last attempt.
            deployResult = metadataConnection.checkDeployStatus(asyncResultId, true);
        }
        return deployResult;
    }
    
    private AsyncResult waitForRetrieveCompletion(AsyncResult asyncResult,MetadataConnection metadataConnection) throws Exception {
        int poll = 0;
        long waitTimeMilliSecs = 1000;
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            if (poll++ > 50) {
                throw new Exception(
                    "Request timed out. If this is a large set of metadata components, " +
                    "ensure that MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            asyncResult = metadataConnection.checkStatus(
                new String[]{asyncResult.getId()})[0];
            System.out.println("Status is: " + asyncResult.getState());
        }
        if (asyncResult.getState() != AsyncRequestState.Completed) {
            throw new Exception(asyncResult.getStatusCode() + " msg: " +
                asyncResult.getMessage());
        }
        return asyncResult;
    }
    
    // ---------- metadata api ---------- //
    
    // ---------- bulk api ---------- //
    
    /**
     * Create the BulkConnection used to call Bulk API operations.
     */
    private BulkConnection getBulkConnection(String token,String instanceUrl)
          throws ConnectionException, AsyncApiException {
        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(token);
        config.setRestEndpoint(instanceUrl + SF_SYNC_URL);
        // This should only be false when doing debugging.
        config.setCompression(true);
        // Set this to true to see HTTP requests and responses on stdout
        config.setTraceMessage(false);
        BulkConnection connection = new BulkConnection(config);
        return connection;
    }
    
    /**
     * Create a new job using the Bulk API.
     * 
     * @param sobjectType
     *            The object type being loaded, such as "Account"
     * @param connection
     *            BulkConnection used to create the new job.
     * @return The JobInfo for the new job.
     * @throws AsyncApiException
     */
    private JobInfo createJob(String sobjectType, BulkConnection connection)
          throws AsyncApiException {
        JobInfo job = new JobInfo();
        job.setObject(sobjectType);
        job.setOperation(OperationEnum.insert);
        job.setContentType(ContentType.CSV);
        job = connection.createJob(job);
        System.out.println(job);
        return job;
    }
    
    /**
     * Create and upload batches using a CSV file.
     * The file into the appropriate size batch files.
     * 
     * @param connection
     *            Connection to use for creating batches
     * @param jobInfo
     *            Job associated with new batches
     * @param csvFileName
     *            The source file for batch data
     */
    private List<BatchInfo> createBatchesFromCSVFile(BulkConnection connection,
          JobInfo jobInfo, String content)
            throws IOException, AsyncApiException {
        List<BatchInfo> batchInfos = new ArrayList<BatchInfo>();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content.getBytes())));
        // read the CSV header row
        byte[] headerBytes = (rdr.readLine() + "\n").getBytes("UTF-8");
        int headerBytesLength = headerBytes.length;
        File tmpFile = File.createTempFile("bulkAPIInsert", ".csv");

        // Split the CSV file into multiple batches
        try {
            FileOutputStream tmpOut = new FileOutputStream(tmpFile);
            int maxBytesPerBatch = 10000000; // 10 million bytes per batch
            int maxRowsPerBatch = 10000; // 10 thousand rows per batch
            int currentBytes = 0;
            int currentLines = 0;
            String nextLine;
            while ((nextLine = rdr.readLine()) != null) {
                byte[] bytes = (nextLine + "\n").getBytes("UTF-8");
                // Create a new batch when our batch size limit is reached
                if (currentBytes + bytes.length > maxBytesPerBatch
                  || currentLines > maxRowsPerBatch) {
                    createBatch(tmpOut, tmpFile, batchInfos, connection, jobInfo);
                    currentBytes = 0;
                    currentLines = 0;
                }
                if (currentBytes == 0) {
                    tmpOut = new FileOutputStream(tmpFile);
                    tmpOut.write(headerBytes);
                    currentBytes = headerBytesLength;
                    currentLines = 1;
                }
                tmpOut.write(bytes);
                currentBytes += bytes.length;
                currentLines++;
            }
            // Finished processing all rows
            // Create a final batch for any remaining data
            if (currentLines > 1) {
                createBatch(tmpOut, tmpFile, batchInfos, connection, jobInfo);
            }
            
        } finally {
            rdr.close();
            tmpFile.delete();
        }
        return batchInfos;
    }

    /**
     * Create a batch by uploading the contents of the file.
     * This closes the output stream.
     * 
     * @param tmpOut
     *            The output stream used to write the CSV data for a single batch.
     * @param tmpFile
     *            The file associated with the above stream.
     * @param batchInfos
     *            The batch info for the newly created batch is added to this list.
     * @param connection
     *            The BulkConnection used to create the new batch.
     * @param jobInfo
     *            The JobInfo associated with the new batch.
     */
    private void createBatch(FileOutputStream tmpOut, File tmpFile,
      List<BatchInfo> batchInfos, BulkConnection connection, JobInfo jobInfo)
              throws IOException, AsyncApiException {
        tmpOut.flush();
        tmpOut.close();
        FileInputStream tmpInputStream = new FileInputStream(tmpFile);
        try {
            BatchInfo batchInfo =
              connection.createBatchFromStream(jobInfo, tmpInputStream);
            System.out.println(batchInfo);
            batchInfos.add(batchInfo);

        } finally {
            tmpInputStream.close();
        }
    }
    
    private void sendJob(BulkConnection connection, String jobId) throws AsyncApiException {
        JobInfo job = new JobInfo();
        job.setId(jobId);
        job.setState(JobStateEnum.Closed);
        connection.updateJob(job);
    }
    
    /**
     * Wait for a job to complete by polling the Bulk API.
     * 
     * @param connection
     *            BulkConnection used to check results.
     * @param job
     *            The job awaiting completion.
     * @param batchInfoList
     *            List of batches for this job.
     * @throws AsyncApiException
     */
    private void awaitCompletion(BulkConnection connection, JobInfo job,
          List<BatchInfo> batchInfoList)
            throws AsyncApiException {
        long sleepTime = 0L;
        Set<String> incomplete = new HashSet<String>();
        for (BatchInfo bi : batchInfoList) {
            incomplete.add(bi.getId());
        }
        while (!incomplete.isEmpty()) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {}
            System.out.println("Awaiting results..." + incomplete.size());
            sleepTime = 10000L;
            BatchInfo[] statusList =
              connection.getBatchInfoList(job.getId()).getBatchInfo();
            for (BatchInfo b : statusList) {
                if (b.getState() == BatchStateEnum.Completed
                  || b.getState() == BatchStateEnum.Failed) {
                    if (incomplete.remove(b.getId())) {
                        System.out.println("BATCH STATUS:\n" + b);
                    }
                }
            }
        }
    }
    
    /**
     * Gets the results of the operation and checks for errors.
     */
    private void checkResults(BulkConnection connection, JobInfo job,
              List<BatchInfo> batchInfoList)
            throws AsyncApiException, IOException {
        // batchInfoList was populated when batches were created and submitted
        for (BatchInfo b : batchInfoList) {
            CSVReader rdr =
              new CSVReader(connection.getBatchResultStream(job.getId(), b.getId()));
            List<String> resultHeader = rdr.nextRecord();
            int resultCols = resultHeader.size();

            List<String> row;
            while ((row = rdr.nextRecord()) != null) {
                Map<String, String> resultInfo = new HashMap<String, String>();
                for (int i = 0; i < resultCols; i++) {
                    resultInfo.put(resultHeader.get(i), row.get(i));
                }
                boolean success = Boolean.valueOf(resultInfo.get("Success"));
                boolean created = Boolean.valueOf(resultInfo.get("Created"));
                String id = resultInfo.get("Id");
                String error = resultInfo.get("Error");
                if (success && created) {
                    System.out.println("Created row with id " + id);
                } else if (!success) {
                    System.out.println("Failed with error: " + error);
                }
            }
        }
    }
    
   // ---------- /bulk api ---------- //
}
