package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Filter {

    private String name;
    private FilterType filterType;
    private FilterField filterField;
    private String title;
    private boolean delete;
    private String show;
    
    @XmlAttribute
    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    @XmlAttribute(name="show")
    public String getShow() {
        return show;
    }

    public void setShow(String show) {
        this.show = show;
    }
    
    public boolean isNeedShow() {
        if(show == null){
            return true;
        }
        return "true".equals(show);
    }
   
    @XmlAttribute
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @XmlElement(name="field")
    public FilterField getFilterField() {
        return filterField;
    }

    public void setFilterField(FilterField filterField) {
        this.filterField = filterField;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name="filtertype")
    public FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }
    
}
