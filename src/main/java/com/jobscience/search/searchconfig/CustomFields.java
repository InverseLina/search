package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class CustomFields {

    private List<CustomField> fields;

    @XmlElement(name="field")
    public List<CustomField> getFields() {
        return fields;
    }

    public void setFields(List<CustomField> fields) {
        this.fields = fields;
    }
}
