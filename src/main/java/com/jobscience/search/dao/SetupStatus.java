package com.jobscience.search.dao;

public enum SetupStatus {
    SYS_CREATE_SCHEMA,
    SYS_IMPORT_ZIPCODE_DATA,
    ORG_IMPORT_BASE_DATA,
    ORG_CREATE_EXTRA,
    ORG_CREATE_INDEX_COLUMNS,
    ORG_CREATE_INDEX_RESUME;
}
