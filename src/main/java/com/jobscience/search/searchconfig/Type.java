package com.jobscience.search.searchconfig;

public enum Type {
    contact(1),
    skill(2),
    company(3),
    education(4),
    location(5),
    Date(6),
    String(7),
    Boolean(8),
    Number(9),
    CONTACT(1),
    SKILL(2),
    COMPANY(3),
    EDUCATION(4),
    LOCATION(5),
    DATE(6),
    STRING(7),
    BOOLEAN(8),
    NUMBER(9);
    
    private int val;
    
    Type(int val){
        this.val = val;
    }
    
    private int getVal(){
        return val;
    }
    
    public boolean equals(Type type){
        if(type == null){
            return false;
        }
        return this.val == type.getVal();
    }
    
    public String value(){
        return this.toString().toLowerCase();
    }
}
