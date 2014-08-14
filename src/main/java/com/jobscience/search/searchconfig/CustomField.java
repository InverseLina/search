package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;

public class CustomField {

    private String type;


    private String columnName;

    private String name;

    @XmlAttribute
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlAttribute
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString(String alias){
        StringBuffer sb = new StringBuffer();
        sb.append(" ").append(alias).append(".\"")
                .append(getColumnName()).append("\" ");
        return sb.toString();
    }

}
