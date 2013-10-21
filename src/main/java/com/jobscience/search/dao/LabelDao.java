package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;

public class LabelDao {
    
    @Inject
    private DBHelper dbHelper;
    @Inject
    private CurrentOrgHolder orgHolder;
    
    public Long addLabel(Long userId,String name) {
        Long id = dbHelper.executeInsertReturnId(orgHolder.getOrgName(),
                "insert into label(name,user_id) values(?,?)", name, userId);
        return id;
    }
    
    public void deleteLabel(String name){
        dbHelper.executeUpdate(orgHolder.getOrgName(),
                "delete from label where name=?", name);
    }
    
    public void deleteLabel(Long id){
        dbHelper.executeUpdate(orgHolder.getOrgName(),
                "delete from label where id=?", id);
    }
    
    public void updateLabel(Long labelId,String name){
        dbHelper.executeUpdate(orgHolder.getOrgName(),
                "update label set name=? where id=?", name,labelId);
    }
    
    public List<Map> getLabelForUser(Long userId){
        return dbHelper.executeQuery(orgHolder.getOrgName(),
                "select * from  label where user_id=?", userId);
    }
    
    public  List<Map>  getLabelForById(Long labelId){
        return dbHelper.executeQuery(orgHolder.getOrgName(),
                "select * from  label where id=?", labelId);
    }
    
    public  List<Map>  getLabelByName(String name){
        return dbHelper.executeQuery(orgHolder.getOrgName(),
                "select * from  label where name=? limit 1", name);
    }
    
    
    public void assignLabelToContact(Long contactId,Long labelId){
        dbHelper.executeUpdate(orgHolder.getOrgName(),
                "insert into label_contact(label_id,contact_id) values(?,?)", labelId,contactId);
    }
    
    public void unAssignLabelFromContact(Long contactId,Long labelId){
        dbHelper.executeUpdate(orgHolder.getOrgName(),
                "delete from label_contact where label_id=? and contact_id=?", labelId,contactId);
    }
}
