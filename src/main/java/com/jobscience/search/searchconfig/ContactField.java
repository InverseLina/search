package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;

public class ContactField extends Field {

    private ContactFieldType type;

    @XmlAttribute(required=true,name="name")
    public ContactFieldType getType() {
        return type;
    }

    public void setType(ContactFieldType type) {
        this.type = type;
    }

    public String toString(String alias){
        StringBuffer sb = new StringBuffer();
        sb.append(" ").append(alias).append(".\"")
          .append(getColumn()).append("\" ");
        return sb.toString();
    }
  
}
