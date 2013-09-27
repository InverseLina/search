package com.jobscience.search.dao;

public enum SetupStatus {
    SYS_CREATE_SCHEMA(1),
    SYS_IMPORT_ZIPCODE_DATA(2),
    ORG_CREATE_EXTRA(3),
    ORG_CREATE_INDEX_COLUMNS(4),
    ORG_CREATE_INDEX_RESUME(5),
    ORG_CREATE_INDEX_RESUME_RUNNING(6),
    PG_TRGM(7),
    SCHEMA_NOT_EXIST(30);
	private Integer value;
	public Integer getValue() {
		return value;
	}
	SetupStatus(Integer value){
		this.value=value;
	}
}
