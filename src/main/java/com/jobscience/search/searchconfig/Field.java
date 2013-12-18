package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;

public class Field {

   
    private String table;
    
   
    private String column;
    
    private String name;

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute
    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    @XmlAttribute(required=true)
    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }
    
    public String toString(String alias){
        StringBuffer sb = new StringBuffer();
        sb.append(" ").append(alias).append(".\"")
          .append(getColumn()).append("\" ");
        return sb.toString();
    }
    
    public static Field getInstance(String name){
        Field f = new Field();
        f.setColumn(name);
        f.setName(name);
        return f;
    }

}
