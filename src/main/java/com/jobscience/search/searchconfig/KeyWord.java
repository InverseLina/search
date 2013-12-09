package com.jobscience.search.searchconfig;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class KeyWord {

    private List<Field> fields;

    @XmlElement(name="field")
    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
    
}
