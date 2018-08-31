package com.rsrini.stickycache.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class UserStickyNote  implements Serializable{

	private static final long serialVersionUID = 1L;

	private String name;
	
	private List<StickyNote> listSticky = new ArrayList<>();

	public UserStickyNote(String name, String title, String content ) {
		super();
		this.name = name;
		listSticky.add(new StickyNote(name, title, content));
	}
	
	public List<StickyNote> getListSticky() {
		return listSticky;
	}

	public void setListSticky(List<StickyNote> listSticky) {
		this.listSticky = listSticky;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "UserStickyNote [name=" + name + ", listSticky=" + listSticky + "]";
	}
	
}
