package com.jobscience.search.searchconfig;

import java.util.ArrayList;
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

    public Filter getFilter(Type type){
        if(type!=null){
            for(Filter filter:filters){
                if(type.equals(filter.getType())){
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
    
    public List<Filter> getColumnFilters () {
    	List<Filter> columnFilters = new ArrayList<Filter>();
    	if(filters != null){
            for(Filter filter:filters){
                if("column".equals(filter.getDisplay())){
                	columnFilters.add(filter);
                }
            }
        }
    	return columnFilters;
    }

    public List<Filter> getCustomFilters () {
    	List<Filter> customFilters = new ArrayList<Filter>();
    	if(filters != null){
            for(Filter filter:filters){
                if("side".equals(filter.getDisplay())){
                	customFilters.add(filter);
                }
            }
        }
    	return customFilters;
    }
    
    public Field getContactField(ContactFieldType type){
        if(type!=null){
            for(Field field:contact.getContactFields()){
                if(type.name().equalsIgnoreCase(field.getName())){
                    return field;
                }
            }
        }
        return Field.getInstance(type.name());
    }
    
    public Field getContactField(String type){
        if(type!=null){
            for(Field field:contact.getContactFields()){
                if(type.equalsIgnoreCase(field.getName())){
                    return field;
                }
            }
        }
        return Field.getInstance(type);
    }

    public String toContactFieldsString(String alias){
        StringBuffer sb = new StringBuffer();
        for(Field field:contact.getContactFields()){
            if(!ContactFieldType.RESUME.name().equalsIgnoreCase(field.getName())){
                sb.append(", ").append(alias).append(".\"")
                  .append(field.getColumn()).append("\" as ")
                  .append(field.getName()).append(" ");
            }
        }
        if(sb.length()>0){
            return sb.substring(1);
        }
        return sb.toString();
    }
    
    public Field getContactFieldByName(String name){
    	Field contactField = null;
    	for(Field field : contact.getContactFields()){
            if(name.equalsIgnoreCase(field.getName())){
            	contactField = field;
            	break;
            }
        }
    	return contactField;
    }
    
}
