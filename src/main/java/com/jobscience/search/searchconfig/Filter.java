package com.jobscience.search.searchconfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Filter {

	private String name;
	private String title;
	private Type type;
	private FilterField filterField;
	private String show;
	private String display;
	private String bg_color;
	private boolean delete;

	@XmlAttribute
	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

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

	@XmlAttribute
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

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute(name = "type")
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
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

	public boolean isFilterColumn (){
		return "column".equals(display);
	}
}
