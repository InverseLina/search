package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;

public class Field {

   
    private String table;
    
   
    private String column;

    @XmlAttribute
    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    @XmlAttribute
    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

}
