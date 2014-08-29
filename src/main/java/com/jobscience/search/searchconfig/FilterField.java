package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.Strings;

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
    
    public String toJoinToString(String alias){
        StringBuffer sb = new StringBuffer();
        sb.append(" ").append(alias).append(".\"")
          .append(getJoinTo()).append("\" ");
        return sb.toString();
    }
    
    public String toJoinFromString(String alias){
        StringBuffer sb = new StringBuffer();
        sb.append(" ").append(alias).append(".\"")
          .append(getJoinFrom()).append("\" ");
        return sb.toString();
    }
    
    public String toString(String alias){
        StringBuffer sb = new StringBuffer();
        if(alias!=null&&!Strings.isNullOrEmpty(alias)){
            sb.append(" ").append(alias).append(".");
        }
        sb.append("\"").append(getColumn()).append("\"");
        return sb.toString();
    }
    
}
