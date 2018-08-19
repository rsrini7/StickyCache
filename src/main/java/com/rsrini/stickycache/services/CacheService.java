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
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.query.QueryManager;
import net.sf.ehcache.search.query.QueryManagerBuilder;

@Service(value="cacheService")
public class CacheService {
	
	private static final String CACHE_NAME = "stickyCache";
	
	@Value("${sticky.cache.runMode}")
	private String runCacheMode;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	private Cache stickyCache;
	
	private CacheService(){
		//CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/disk/ehcache.xml"));
		//CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/go/ehcache.xml"));
		//CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/arc/ehcache.xml"));
		CacheManager cm = CacheManager.newInstance(CacheService.class.getResource("/max/ehcache.xml"));
//		cm.addCache(CACHE_NAME);
		stickyCache = cm.getCache(CACHE_NAME);
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

	public Collection<Element> searchSticky(StickyNoteFilter stickyFilter) {
		
		QueryManager queryManager = QueryManagerBuilder
		        .newQueryManagerBuilder() 
		        .addCache(stickyCache)
		        .build(); 
		
		String searchValue = "*" + stickyFilter.getSearchValue() + "*";
		Results filteredStickies = null;
		StickySearchType searchType = stickyFilter.getSearchType();
		System.out.println("search value -"+searchValue);
		
		switch (searchType){
			case SEARCH_BY_KEY:{
				//filteredStickies = stickyCache.createQuery().addCriteria(Query.KEY.ilike(searchValue)).includeKeys().includeValues().execute();

				Query stickyKeyQuery = queryManager.createQuery("select * from stickyCache where key like '%"+stickyFilter.getSearchValue()+"%'").includeKeys().includeValues();
				filteredStickies = stickyKeyQuery.end().execute();

				break;
			}
			case SEARCH_BY_VALUE:{
				filteredStickies = stickyCache.createQuery().addCriteria(Query.VALUE.ilike(searchValue)).includeKeys().includeValues().execute();
				break;
			}
			case SEARCH_BY_KEY_AND_VALUE: {
				filteredStickies = stickyCache.createQuery().addCriteria(Query.KEY.ilike(searchValue).or(Query.VALUE.ilike(searchValue))).includeKeys().includeValues().execute();
				break;
			}
			default: {
				System.out.println("Invalid search Type ");
				break;
			}
		}
		System.out.println("sticky search result size ---"+filteredStickies.all().size());
		List<Element> elements = new ArrayList<Element>();
		for (Result filterSticky: filteredStickies.all()){
			Element element = new Element(filterSticky.getKey(), filterSticky.getValue());
			System.out.println("sticky key -"+filterSticky.getKey());
			System.out.println("sticky value -"+filterSticky.getValue());
			elements.add(element);
		}
		return elements;
	}
	
	public boolean removeCacheElement(String key){
		return  stickyCache.remove(key);
	}
}
