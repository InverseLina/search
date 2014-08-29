package com.jobscience.search.service.sfsync;

import com.google.inject.Singleton;

@Singleton
public class NameResolver {
    

    public String escapeName(String name){
        String result = name;
        result = "jss_"+result.replaceAll("__", "00");
        result = result.replaceAll("__", "_");
        return result;
    }
    
    public String unencapeName(String name){
        String result = name;
        result = result.substring(3, result.length() - 3);
        
        //FIXME for now hardcode, cause two table _trigger_last_id, _trigger_log
        if(result.indexOf("_trigger") == -1 && result.indexOf("_c5") == -1){
            result = result.substring(1, result.length());
        }
        result = result.replaceAll("00", "__");
        return result;
    }
}
