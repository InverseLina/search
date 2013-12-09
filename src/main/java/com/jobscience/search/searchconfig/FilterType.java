package com.jobscience.search.searchconfig;

public enum FilterType {
    contacts(1),
    skills(2),
    companies(3),
    educations(4),
    locations(5),
    CONTACTS(1),
    SKILLS(2),
    COMPANIES(3),
    EDUCATIONS(4),
    LOCATIONS(5);
    
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
}
