package com.jobscience.search.searchconfig;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="searchconfig")
public class SearchConfiguration {

    private KeyWord keyword;
    private List<Filter> filters;
    private Contact contact;
    private CustomFields customFields;

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

    @XmlElement
    public CustomFields getCustomFields() {
        return customFields;
    }

    public void setCustomFields(CustomFields customFields) {
        this.customFields = customFields;
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

    public CustomField getCustomFieldByName(String name){
        if(name!=null){
          for(CustomField f:customFields.getFields()){
              if(name.equals(f.getName())){
                  return f;
              }
          }
        }
        return null;
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
}
