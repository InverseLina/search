package com.jobscience.search.dao;

import static com.jobscience.search.Utils.demoSfid;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LabelDao {
    
    @Inject
    private DaoHelper daoHelper;
    
    public Object addLabel(String sfid, String name,Map org) {
        Map result = (Map) daoHelper.create((String)org.get("name"),
                "insert into ts2__s_userlist__c(name,ownerid, sfid) values(?,?, ?) returning id", name, sfid, demoSfid());
        return  result.get("id");
    }
    
    public void deleteLabel(String name,Map org){
        daoHelper.executeUpdate((String)org.get("name"),
                "delete from ts2__s_userlist__c where name=?", name);
    }
    
    public void deleteLabel(Long id,Map org){
        daoHelper.executeUpdate((String)org.get("name"),
                "delete from  ts2__s_userlist__c where id=?", id);
    }
    
    public void updateLabel(Long labelId,String name,Map org){
        daoHelper.executeUpdate((String)org.get("name"),
                "update  ts2__s_userlist__c set name=? where id=?", name, labelId);
    }
    
    public List<Map> getLabelForUser(Long userId,Map org){
        return daoHelper.executeQuery((String)org.get("name"),
                "select a.* from  ts2__s_userlist__c a inner join \"jss_user\" b on a.ownerid = b.sfid where b.id=?", userId);
    }
    
    public  List<Map>  getLabelForById(Long labelId,Map org){
        return daoHelper.executeQuery((String)org.get("name"),
                "select * from  ts2__s_userlist__c  where id=?", labelId);
    }
    
    public  List<Map>  getLabelByName(String name, Object userId,Map org){
        return daoHelper.executeQuery((String)org.get("name"),
                "select a.* from  ts2__s_userlist__c a inner join \"jss_user\" b on a.ownerid = b.sfid where a.name=? and b.id = ? limit 1", name, userId);
    }
    
    
    public void assignLabelToContact(Long contactId,Long labelId,Map org){
        daoHelper.executeUpdate((String)org.get("name"),
                "insert into ts2__s_userlistlink__c (ts2__r_contact__c, ts2__r_user_list__c ) " +
                 "select (select sfid from contact where id = ?)," +
                 " (select sfid from ts2__s_userlist__c where id =?)",contactId, labelId);
    }
    
    public void unAssignLabelFromContact(Long contactId,Long labelId,Map org){
        daoHelper.executeUpdate((String)org.get("name"),
                "delete from ts2__s_userlistlink__c where " +
                 "ts2__r_user_list__c in (select sfid from ts2__s_userlist__c where id = ? )" +
                 "and ts2__r_contact__c in (select sfid from contact where id=?)", labelId, contactId);
    }
    
    public List<Map>  getLabelStatus(String contactIds,Long labelId,Map org){
       return  daoHelper.executeQuery((String)org.get("name"),
                "select c.id, case  when l.id is null then false else true end haslabel from" +
                " (select id, sfid from contact where id in ("+contactIds+"))" +
                " c left join ts2__s_userlistlink__c l on c.sfid=l.ts2__r_contact__c and" +
                  "  l.ts2__r_user_list__c in (select sfid from ts2__s_userlist__c where id = ?)", labelId);
    }
}
