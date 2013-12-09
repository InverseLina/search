package com.jobscience.search.searchconfig;


public enum ContactFieldType {

    ID(1),
    NAME(2),
    EMAIL(3),
    TITLE(4),
    SFID(5),
    CREATEDDATE(6),
    RESUME(7),
    MAILINGPOSTALCODE(8),
    id(1),
    name(2),
    email(3),
    title(4),
    sfid(5),
    createddate(6),
    resume(7),
    mailingpostalcode(8);
    
    private int val;
    
    ContactFieldType(int val){
        this.val = val;
    }
    
    public boolean equals(ContactFieldType type){
        if(type==null){
            return false;
        }
        return this.val == type.val;
    }
    
}
