package com.jobscience.search.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.inject.Singleton;

@Singleton
public class QueryLogger {
   private Logger log = Logger.getLogger(getClass());
   
   public void debug(LoggerType type,Object message){
       if(log.isDebugEnabled()){
           log.debug(getPrefix(type)+message+getUnit(type));
       }
   }
    
   public void error(LoggerType type,Object message){
       if(log.isEnabledFor(Level.ERROR)){
           log.error(getPrefix(type)+message+getUnit(type));
       }
   }
   
   private String getPrefix(LoggerType type){
       String prefix = null ;
       switch(type){
           case SEARCH_SQL: 
               prefix = "search select query : ";
               break;
           case SEARCH_COUNT_SQL:
               prefix = "search count query : ";
               break;
           case AUTO_SQL:
               prefix = "auto complete query : ";
               break;
           case PARAMS:
               prefix = "params : ";
               break;
           case AUTO_PERF:
               prefix = "auto complete performance : ";
               break;
           case SEARCH_PERF:
               prefix = "search performance : ";
               break;
           case SEARCH_COUNT_PERF:
               prefix = "search count performance : ";
               break;
       }
       return prefix;
   }
   
   private String getUnit(LoggerType type){
       switch(type){
           case AUTO_PERF:
           case SEARCH_COUNT_PERF:
           case SEARCH_PERF: return "ms";
           default : return "";
       }
   }
}
