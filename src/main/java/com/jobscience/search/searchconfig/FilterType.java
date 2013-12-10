package com.jobscience.search.searchconfig;

public enum FilterType {
    contact(1),
    skill(2),
    company(3),
    education(4),
    location(5),
    CONTACT(1),
    SKILL(2),
    COMPANY(3),
    EDUCATION(4),
    LOCATION(5);
    
    private int val;
    
    FilterType(int val){
        this.val = val;
    }
    
    private int getVal(){
        return val;
    }
    
    public boolean equals(FilterType filterType){
        return this.val==filterType.getVal();
    }
    
    public String value(){
        return this.toString().toLowerCase();
    }
}
