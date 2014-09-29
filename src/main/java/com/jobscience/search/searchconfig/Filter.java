package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Filter {

	private String name;
	private String title;
	private Type filterType;
	private String type;
	private FilterField filterField;
	private String show;
	private String display;
	private String bg_color;
	private boolean all_any;
	private boolean orderable;
	private boolean delete;

	@XmlAttribute(name = "show")
	public String getShow() {
		return show;
	}

	public void setShow(String show) {
		this.show = show;
	}

	public boolean isNeedShow() {
		if (show == null) {
			return true;
		}
		return "true".equals(show);
	}

	@XmlAttribute(name = "title")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@XmlElement(name = "field")
	public FilterField getFilterField() {
		return filterField;
	}

	public void setFilterField(FilterField filterField) {
		this.filterField = filterField;
	}

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute(name = "type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlAttribute(name = "display")
	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	@XmlAttribute(name = "bg-color")
	public String getBg_color() {
		return bg_color;
	}

	public void setBg_color(String bg_color) {
		this.bg_color = bg_color;
	}
	
	@XmlAttribute(name = "delete")
	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}
	
	@XmlAttribute(name = "all-any")
	public boolean isAll_any() {
		return all_any;
	}

	public void setAll_any(boolean all_any) {
		this.all_any = all_any;
	}

	@XmlAttribute(name = "orderable")
	public boolean isOrderable() {
		return orderable;
	}

	public void setOrderable(boolean orderable) {
		this.orderable = orderable;
	}

	public Type getFilterType() {
		if(filterType == null && type != null){
			for (Type type : Type.values()){
				if(type.value().toLowerCase().equals(this.type.toLowerCase())){
					this.filterType = type;
				}
			}
		}
		return filterType;
	}

	public void setFilterType(Type filterType) {
		this.filterType = filterType;
	}

	public boolean isFilterColumn (){
		return "column".equals(display);
	}
}
