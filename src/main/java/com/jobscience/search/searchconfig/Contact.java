package com.jobscience.search.searchconfig;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Contact {

    private List<Field> contactFields;
    
    private String table;
   
    @XmlAttribute
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private String title;
    
    @XmlAttribute(name="table")
    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    @XmlElement(name="field")
    public List<Field> getContactFields() {
        return contactFields;
    }

    public void setContactFields(List<Field> contactFields) {
        this.contactFields = contactFields;
    }
    
}
