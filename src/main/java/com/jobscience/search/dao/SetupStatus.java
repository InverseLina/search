package com.jobscience.search.dao;

public enum SetupStatus {
    SYS_SCHEMA_CREATED(1<<0),//1
    ZIPCODE_DATA_IMPORTED(1<<1),//2
    ORG_SCHEMA_CREATED(1<<2),//4
    ORG_EXTRA_CREATED(1<<3),//8
    ORG_INDEX_COLUMNS_CREATED(1<<4),//16
    ORG_INDEX_RESUME_CREATED(1<<5),//32
    ORG_RESUME_RUNNING(1<<6),//64
    PG_TRGM_CREATED(1<<7);//128
    
	private Integer value;
	public Integer getValue() {
		return value;
	}
	SetupStatus(Integer value){
		this.value=value;
	}
}
