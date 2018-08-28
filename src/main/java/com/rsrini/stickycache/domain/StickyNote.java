package com.rsrini.stickycache.domain;

import java.io.Serializable;

import org.springframework.stereotype.Component;

@Component
public class StickyNote implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String user;

	private String title;
	
	private String content;
	
	public StickyNote() {}
	
	public StickyNote(String title, String content) {
		super();
		this.title = title;
		this.content = content;
	}
	
	public StickyNote(String user, String title, String content) {
		super();
		this.user = user;
		this.title = title;
		this.content = content;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	public String getTitleContent() {
		return this.title + "~" + this.content;
	}

	@Override
	public String toString() {
		return "StickyNote [user=" + user + ", title=" + title + ", content=" + content + "]";
	}

}
