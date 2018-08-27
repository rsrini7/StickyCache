package com.rsrini.stickycache.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.rsrini.stickycache.domain.StickyNoteFilter;
import com.rsrini.stickycache.domain.StickyNoteFilter.StickySearchType;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.query.QueryManager;
import net.sf.ehcache.search.query.QueryManagerBuilder;

@Service(value="cacheService")
public class CacheService {
	
	private static final String CACHE_NAME = "stickyCache";
	private static final String SEARCH_CACHE_NAME = "searchCache";
	
	@Value("${sticky.cache.runMode}")
	private String runCacheMode;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	private Cache stickyCache;
	
	private Cache searchCache;
	
	private CacheService(){
		//CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/disk/ehcache.xml"));
		CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/go/ehcache.xml"));
		//CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/go/ehcacheuser.xml"));
		//CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/max/ehcache.xml"));
		//CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/arc/ehcache.xml"));
//		cm.addCache(CACHE_NAME);
		stickyCache = cm.getCache(CACHE_NAME);
		searchCache = cm.getCache(SEARCH_CACHE_NAME);
	}
	
	@PostConstruct
	public void initializeCache(){
		System.out.println("Initialize Cache method invoked");
		System.out.println("context property source bean injection" + applicationContext);
		System.out.println("cache mode"+runCacheMode);
		if (runCacheMode.equals("CleanRun")){
			stickyCache.removeAll();
		}
	}

	public Collection<Element> addToCache(Element element){
		stickyCache.put(element);
		System.out.println(" element to be added "+element);
		return getCacheElements();
	}
	
	public Collection<Element> getCacheElements(){
		Map<Object, Element> keysWithElements = stickyCache.getAll(stickyCache.getKeys());
		System.out.println("key set::::: "+keysWithElements.keySet());
		Collection<Element> elements = keysWithElements.values();
		/***to be removed**/
		System.out.println("context property source bean injection" + applicationContext);
		System.out.println("cache mode"+runCacheMode);
		for (Element element1: elements){
			System.out.println("key -" + element1.getObjectKey() + " value - "+ element1.getObjectValue());
		}
		/***to be removed**/
		return elements;
	}
	
	public Collection<Element> getSearchedCacheElements(){
		Map<Object, Element> keysWithElements = searchCache.getAll(searchCache.getKeys());
		Collection<Element> elements = keysWithElements.values();
		for (Element element1: elements){
			System.out.println("key -" + element1.getObjectKey() + " value - "+ element1.getObjectValue());
		}
		return elements;
	}

	public Collection<Element> searchSticky(StickyNoteFilter stickyFilter) {
		
		QueryManager queryManager = QueryManagerBuilder
		        .newQueryManagerBuilder() 
		        .addCache(stickyCache)
		        .addCache(searchCache)
		        .build(); 
		
		//ehcacheuser.xml
		/*Attribute<String> title = stickyCache.getSearchAttribute("title");
		Attribute<String> content = stickyCache.getSearchAttribute("content");*/
		
		String searchActualValue = stickyFilter.getSearchValue();
		
		if(searchActualValue!=null  && searchActualValue.trim().length() != 0) {
			Element searchedElement =  searchCache.get(searchActualValue);
			if(null != searchedElement) {
				searchCache.put(new Element(searchActualValue,(Integer)searchedElement.getObjectValue() + 1));
			}else {
				searchCache.put(new Element(searchActualValue,new Integer(1)));
			}
		}
		
		String searchValue = "*" + searchActualValue + "*";
		Results filteredStickies = null, searchStickies = null;
		StickySearchType searchType = stickyFilter.getSearchType();
		System.out.println("search value -"+searchValue);
		
		switch (searchType){
			case SEARCH_BY_KEY:{
				//filteredStickies = stickyCache.createQuery().addCriteria(Query.KEY.ilike(searchValue)).includeKeys().includeValues().execute();

				Query stickyKeyQuery = queryManager.createQuery("select * from stickyCache where key like '%"+searchActualValue+"%'").includeKeys().includeValues();//ehcache.xml
				//Query stickyKeyQuery = queryManager.createQuery("select * from stickyCache where title like '%"+searchActualValue+"%'").includeKeys().includeValues();//ehcacheuser.xml
				filteredStickies = stickyKeyQuery.end().execute();
				
				searchStickies = searchResults(queryManager,searchActualValue);
				break;
			}
			case SEARCH_BY_VALUE:{
				filteredStickies = stickyCache.createQuery().addCriteria(Query.VALUE.ilike(searchValue)).includeKeys().includeValues().execute();//ehcache.xml
				//filteredStickies = stickyCache.createQuery().addCriteria(content.ilike(searchValue)).includeKeys().includeValues().execute();//ehcacheuser.xml
				searchStickies = searchResults(queryManager,searchActualValue);
				break;
			}
			case SEARCH_BY_KEY_AND_VALUE: {
				filteredStickies = stickyCache.createQuery().addCriteria(Query.KEY.ilike(searchValue).or(Query.VALUE.ilike(searchValue))).includeKeys().includeValues().execute();//ehcache.xml
				//filteredStickies = stickyCache.createQuery().addCriteria(title.ilike(searchValue).or(content.ilike(searchValue))).includeKeys().includeValues().execute();//ehcacheuser.xml
				searchStickies = searchResults(queryManager,searchActualValue);
				break;
			}
			default: {
				System.out.println("Invalid search Type ");
				break;
			}
		}
		System.out.println("sticky search result size ---"+filteredStickies.all().size());
		List<Element> elements = new ArrayList<Element>();
		
		List<Result> searchResult = searchStickies.all();
		String searchCount = (searchResult!= null && !searchResult.isEmpty()) ? searchResult.get(0).getValue()+"" : "";
		
		System.out.println("direct count using key : "+searchCache.get(searchActualValue));
		System.out.println("query based count : "+searchCount);
		
		for (Result filterSticky: filteredStickies.all()){
			Element element = new Element(filterSticky.getKey()+" ( "+searchCount+" )", filterSticky.getValue());
			System.out.println("sticky key -"+filterSticky.getKey());
			System.out.println("sticky value -"+filterSticky.getValue());
			elements.add(element);
		}
		return elements;
	}
	
	private Results searchResults(QueryManager queryManager, String searchedData) {
		String queryStr = "select * from searchCache where (searchedData = '"+searchedData+"')";
		System.out.println("Query: "+ queryStr);
		Query searchStickyKeyQuery = queryManager.createQuery(queryStr).includeKeys().includeValues();
		return searchStickyKeyQuery.end().execute();
	}
	
	public boolean removeCacheElement(String key){
		return  stickyCache.remove(key);
	}
}
