package com.jobscience.search.dao;

import static com.jobscience.search.Utils.demoSfid;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.CurrentOrgHolder;

@Singleton
public class LabelDao {
    
    @Inject
    private DaoHelper daoHelper;
    @Inject
    private CurrentOrgHolder orgHolder;
    
    public Object addLabel(String sfid, String name) {
        Map result = (Map) daoHelper.insert(orgHolder.getOrgName(),
                "insert into ts2__s_userlist__c(name,ownerid, sfid) values(?,?, ?) returning id", name, sfid, demoSfid());
        return  result.get("id");
    }
    
    public void deleteLabel(String name){
        daoHelper.executeUpdate(orgHolder.getOrgName(),
                "delete from ts2__s_userlist__c where name=?", name);
    }
    
    public void deleteLabel(Long id){
        daoHelper.executeUpdate(orgHolder.getOrgName(),
                "delete from  ts2__s_userlist__c where id=?", id);
    }
    
    public void updateLabel(Long labelId,String name){
        daoHelper.executeUpdate(orgHolder.getOrgName(),
                "update  ts2__s_userlist__c set name=? where id=?", name, labelId);
    }
    
    public List<Map> getLabelForUser(Long userId){
        return daoHelper.executeQuery(orgHolder.getOrgName(),
                "select a.* from  ts2__s_userlist__c a inner join \"user\" b on a.ownerid = b.sfid where b.id=?", userId);
    }
    
    public  List<Map>  getLabelForById(Long labelId){
        return daoHelper.executeQuery(orgHolder.getOrgName(),
                "select * from  ts2__s_userlist__c  where id=?", labelId);
    }
    
    public  List<Map>  getLabelByName(String name, Object userId){
        return daoHelper.executeQuery(orgHolder.getOrgName(),
                "select a.* from  ts2__s_userlist__c a inner join \"user\" b on a.ownerid = b.sfid where a.name=? and b.id = ? limit 1", name, userId);
    }
    
    
    public void assignLabelToContact(Long contactId,Long labelId){
        daoHelper.executeUpdate(orgHolder.getOrgName(),
                "insert into ts2__s_userlistlink__c (ts2__r_contact__c, ts2__r_user_list__c ) " +
                 "select (select sfid from contact where id = ?)," +
                 " (select sfid from ts2__s_userlist__c where id =?)",contactId, labelId);
    }
    
    public void unAssignLabelFromContact(Long contactId,Long labelId){
        daoHelper.executeUpdate(orgHolder.getOrgName(),
                "delete from ts2__s_userlistlink__c where " +
                 "ts2__r_user_list__c in (select sfid from ts2__s_userlist__c where id = ? )" +
                 "and ts2__r_contact__c in (select sfid from contact where id=?)", labelId, contactId);
    }
    
    public List<Map>  getLabelStatus(String contactIds,Long labelId){
       return  daoHelper.executeQuery(orgHolder.getOrgName(),
                "select c.id, case  when l.id is null then false else true end haslabel from" +
                " (select id, sfid from contact where id in ("+contactIds+"))" +
                " c left join ts2__s_userlistlink__c l on c.sfid=l.ts2__r_contact__c and" +
                  "  l.ts2__r_user_list__c in (select sfid from ts2__s_userlist__c where id = ?)", labelId);
    }
}
