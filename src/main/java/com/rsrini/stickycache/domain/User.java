package com.rsrini.stickycache.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class User {

	private String name;
	
	private List<StickyNote> listSticky = new ArrayList<>();

	public User(String name, String title, String content ) {
		super();
		this.name = name;
		listSticky.add(new StickyNote(title, content));
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
		return "User [name=" + name + ", listSticky=" + listSticky + "]";
	}
	
}
