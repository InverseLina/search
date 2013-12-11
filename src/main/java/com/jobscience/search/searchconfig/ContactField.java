package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;

public class ContactField extends Field {

    private String type;

    @XmlAttribute(required=true,name="name")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String toString(String alias){
        StringBuffer sb = new StringBuffer();
        sb.append(" ").append(alias).append(".\"")
          .append(getColumn()).append("\" ");
        return sb.toString();
    }
  
    public static ContactField getInstance(String name){
        ContactField cf = new ContactField();
        cf.setColumn(name);
        cf.setType(name);
        return cf;
    }
}
