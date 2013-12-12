package com.jobscience.search.searchconfig;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="searchconfig")
public class SearchConfiguration {

    private KeyWord keyword;

    private List<Filter> filters;
    
    private Contact contact;
    
    @XmlElement
    public KeyWord getKeyword() {
        return keyword;
    }

    public void setKeyword(KeyWord keyword) {
        this.keyword = keyword;
    }

    @XmlElement
    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
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
    
    public Filter getFilterByName(String name){
        if(name!=null){
            for(Filter filter:filters){
                if(name.equals(filter.getName())){
                    return filter;
                }
            }
        }
        return null;
    }
    
    public ContactField getContactField(ContactFieldType type){
        if(type!=null){
            for(ContactField field:contact.getContactFields()){
                if(type.name().equalsIgnoreCase(field.getType())){
                    return field;
                }
            }
        }
        return ContactField.getInstance(type.name());
    }
    
    public ContactField getContactField(String type){
        if(type!=null){
            for(ContactField field:contact.getContactFields()){
                if(type.equalsIgnoreCase(field.getType())){
                    return field;
                }
            }
        }
        return ContactField.getInstance(type);
    }

    public String toContactFieldsString(String alias){
        StringBuffer sb = new StringBuffer();
        for(ContactField field:contact.getContactFields()){
            if(!ContactFieldType.RESUME.name().equalsIgnoreCase(field.getType())){
                sb.append(", ").append(alias).append(".\"")
                  .append(field.getColumn()).append("\" as ")
                  .append(field.getType()).append(" ");
            }
        }
        if(sb.length()>0){
            return sb.substring(1);
        }
        return sb.toString();
    }
}
