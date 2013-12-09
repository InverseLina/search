package com.jobscience.search.searchconfig;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="searchconfig")
public class SearchConfiguration {

    private KeyWord keyword;

    private List<Filter> filters;
    
    @XmlElement
    public KeyWord getKeyword() {
        return keyword;
    }

    public void setKeyword(KeyWord keyword) {
        this.keyword = keyword;
    }

    @XmlElement(name="filter")
    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }
    
    public Filter getFilter(FilterType filterType){
        if(filterType!=null){
            for(Filter filter:filters){
                if(filterType.equals(filter.getFilterType())){
                    return filter;
                }
            }
        }
        return null;
    }
}
