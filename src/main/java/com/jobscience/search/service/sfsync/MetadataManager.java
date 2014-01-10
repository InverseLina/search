package com.jobscience.search.service.sfsync;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.britesnow.snow.util.JsonUtil;
import com.google.inject.Singleton;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;


@Singleton
public class MetadataManager {
    
    private String  metadataServerUrl = null;
//    String packageNamespacePrefix = "";
    
    public MetadataConnection getMetadataConnection(String token, String instanceUrl) throws ConnectionException {
        String metadataServerUrl = getMetaDataServerUrl(token, instanceUrl);
        final ConnectorConfig config = new ConnectorConfig();
        config.setServiceEndpoint(metadataServerUrl);
        config.setSessionId(token);
        MetadataConnection metadataConnection = null;
        metadataConnection = new MetadataConnection(config);
        return metadataConnection;
    }
    
    public void deployMetadataObjects(MetadataConnection metadataConnection, byte[] zipBytes) throws Exception{
        DeployOptions deployOptions = new DeployOptions();
        deployOptions.setPerformRetrieve(false);
        deployOptions.setRollbackOnError(true);
        AsyncResult asyncResult = metadataConnection.deploy(zipBytes, deployOptions);
        DeployResult deployResult = waitForDeployCompletion(asyncResult.getId(),metadataConnection);
        if (!deployResult.isSuccess()) {
            System.out.println("The files were not successfully deployed: "+deployResult.getErrorMessage());
        }
    }
    
    public byte[] retriveMetadataObjects(MetadataConnection metadataConnection, com.sforce.soap.metadata.Package p){
        try{
            
            RetrieveRequest retrieveRequest = new RetrieveRequest();
            retrieveRequest.setApiVersion(new Double(SFConfig.VERSION));
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
//            for(int i = 0; i < retrieveResult.getFileProperties().length; i++){
//                if(retrieveResult.getFileProperties()[i].getNamespacePrefix() != null){
//                    packageNamespacePrefix = retrieveResult.getFileProperties()[i].getNamespacePrefix() + "__";
//                    break;
//                }
//            }
            byte[] bs = retrieveResult.getZipFile();
            return bs;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
        
    }
    
    private String getMetaDataServerUrl(String token, String instanceUrl){
        if(metadataServerUrl == null){
            Map map = getloginInfo(token, instanceUrl);
            JSONObject m = (JSONObject) map.get("urls");
            metadataServerUrl = m.getString("metadata");
            metadataServerUrl = metadataServerUrl.replace("{version}", SFConfig.VERSION);
        }
        return metadataServerUrl;
    }
    private Map<String,String> getloginInfo(String token, String instance_url) {
        Map<String,String> result = new HashMap<String,String>();
        //  Get User Info
        OAuthRequest oauth = new OAuthRequest(Verb.GET,instance_url+SFConfig.SF_URL);
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
    
}
