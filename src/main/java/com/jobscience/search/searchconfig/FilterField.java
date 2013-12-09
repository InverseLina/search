package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;

public class FilterField extends Field {

    private String joinFrom;
    private String joinTo;
    
    @XmlAttribute(name="joinfrom")
    public String getJoinFrom() {
        return joinFrom;
    }
    public void setJoinFrom(String joinFrom) {
        this.joinFrom = joinFrom;
    }
    
    @XmlAttribute(name="jointo")
    public String getJoinTo() {
        return joinTo;
    }
    public void setJoinTo(String joinTo) {
        this.joinTo = joinTo;
    }
    
    
    
}
