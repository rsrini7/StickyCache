package com.rsrini.stickycache.domain;

public class StickyNoteFilter {
	
	public enum StickySearchType{
		SEARCH_BY_KEY,
		SEARCH_BY_VALUE,
		SEARCH_BY_KEY_AND_VALUE
	}
	
	private boolean loadFromDB;
	private String searchValue;
	private StickySearchType searchType = StickySearchType.SEARCH_BY_KEY_AND_VALUE;

	public StickyNoteFilter() {
	}
	
	public StickyNoteFilter(String searchValue) {
		super();
		this.searchValue = searchValue;
		this.searchType = StickySearchType.SEARCH_BY_KEY_AND_VALUE;
	}
	
	public boolean isLoadFromDB() {
		return loadFromDB;
	}
	public void setLoadFromDB(boolean loadFromDB) {
		this.loadFromDB = loadFromDB;
	}
	public String getSearchValue() {
		return searchValue;
	}
	public void setSearchValue(String searchValue) {
		this.searchValue = searchValue;
	}
	public StickySearchType getSearchType() {
		return searchType;
	}
	public void setSearchType(StickySearchType searchType) {
		this.searchType = searchType;
	}

}
