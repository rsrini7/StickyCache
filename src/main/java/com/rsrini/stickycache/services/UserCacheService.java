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

import com.rsrini.stickycache.domain.StickyNote;
import com.rsrini.stickycache.domain.StickyNoteFilter;
import com.rsrini.stickycache.domain.StickyNoteFilter.StickySearchType;
import com.rsrini.stickycache.domain.UserFilter;
import com.rsrini.stickycache.domain.UserStickyNote;
import com.rsrini.stickycache.util.StickyNoteCollectionExtrator;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.query.QueryManager;
import net.sf.ehcache.search.query.QueryManagerBuilder;

@Service(value="userCacheService")
public class UserCacheService {
	
	private static final String CACHE_NAME = "stickyCache";
	private static final String USER_CACHE_NAME = "userStickyCache";
	private static final String SEARCH_CACHE_NAME = "searchCache";
	
	@Value("${sticky.cache.runMode}")
	private String runCacheMode;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	private Ehcache stickyCache;
	private Ehcache searchCache;
	private Ehcache userStickyCache;
	
	private UserCacheService(){
		
		// Create Cache
	    Configuration managerConfig = new Configuration()
	        //.terracotta(new TerracottaClientConfiguration().url("localhost:9510"))
	        .cache(getCacheConfig())
	        .cache(getUserCacheConfig())
	        .cache(new CacheConfiguration().name(SEARCH_CACHE_NAME)
	            .eternal(true)
	            .maxBytesLocalHeap(128, MemoryUnit.MEGABYTES)
	            //.terracotta(new TerracottaConfiguration().consistency(TerracottaConfiguration.Consistency.STRONG))
	            .searchable(new Searchable()
	                .searchAttribute(new SearchAttribute().name("searchedData").expression("key"))
		      ));

	    
	    CacheManager manager = CacheManager.create(managerConfig);
	    searchCache = manager.getEhcache(SEARCH_CACHE_NAME);
	    stickyCache = manager.getEhcache(CACHE_NAME);
	    userStickyCache = manager.getEhcache(USER_CACHE_NAME);
	}

	private CacheConfiguration getCacheConfig() {
		 CacheConfiguration cacheConfig = new CacheConfiguration().name(CACHE_NAME)
		    .eternal(true)
		    .maxBytesLocalHeap(1, MemoryUnit.MEGABYTES)
		    .maxBytesLocalOffHeap(128, MemoryUnit.MEGABYTES)
		    .eternal(true).copyOnRead(true)
		    //.terracotta(new TerracottaConfiguration().consistency(TerracottaConfiguration.Consistency.STRONG))
		    .searchable(new Searchable()
		        .searchAttribute(new SearchAttribute().name("title").expression("value.getTitle()"))
		        .searchAttribute(new SearchAttribute().name("content").expression("value.getContent()"))
		    		);
		 
		 cacheConfig.addCacheWriter(cacheWriterConfig("com.rsrini.stickycache.util.StickyNoteCacheDBWriterFactory"));
		 
		 return cacheConfig;
	}

	private CacheConfiguration getUserCacheConfig() {
		 CacheConfiguration cacheConfig = new CacheConfiguration().name(USER_CACHE_NAME)
		        .eternal(true)
		        .maxBytesLocalHeap(1, MemoryUnit.MEGABYTES)
		        .maxBytesLocalOffHeap(128, MemoryUnit.MEGABYTES)
		        .eternal(true).copyOnRead(true)
		        //.terracotta(new TerracottaConfiguration().consistency(TerracottaConfiguration.Consistency.STRONG))
		        .searchable(new Searchable()
		            .searchAttribute(new SearchAttribute().name("user").expression("value.getName()"))
		            //.searchAttribute(new SearchAttribute().name("titleOrContent").className(StickyNoteDataExtrator.class.getName()).properties("value.getTitleContent()"))
		            .searchAttribute(new SearchAttribute().name("titleOrContent").className(StickyNoteCollectionExtrator.class.getName()).properties("value.getName()")) // dummy property required to boot the application
		    		);
		
		//cacheConfig.addCacheWriter(cacheWriterConfig("com.rsrini.stickycache.util.StickyNoteCacheDBWriterFactory"));
		return cacheConfig;
	}
	
	private CacheWriterConfiguration cacheWriterConfig(String className) {
		return new CacheWriterConfiguration().writeMode(CacheWriterConfiguration.WriteMode.WRITE_BEHIND)
				.maxWriteDelay(3).rateLimitPerSecond(10).notifyListenersOnException(true).writeCoalescing(true)
				.writeBatching(true).writeBatchSize(2).retryAttempts(5).retryAttemptDelaySeconds(5).cacheWriterFactory(
						new CacheWriterConfiguration.CacheWriterFactoryConfiguration().className(className)
						.properties("url=jdbc:h2:mem:stickycache_db;DB_CLOSE_ON_EXIT=FALSE;id=sa;pw=").propertySeparator(";"));
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

	public void addToUserCache(Element element, boolean writeBehindDbFlag){
		if(writeBehindDbFlag) {
			userStickyCache.putWithWriter(element);
		}else {
			userStickyCache.put(element);
		}
		System.out.println(" element to be added to usercache "+element);
	}
	
	public Collection<Element> addToCache(Element element, boolean writeBehindDbFlag){
		if(writeBehindDbFlag) {
			stickyCache.putWithWriter(element);
		}else {
			stickyCache.put(element);
		}
		System.out.println(" element to be added "+element);
		return getCacheElements();
	}
	
	public Collection<Element> getUserCacheElements(){
		Map<Object, Element> keysWithElements = userStickyCache.getAll(userStickyCache.getKeys());
		System.out.println("userlevel key set::::: "+keysWithElements.keySet());
		Collection<Element> elements = keysWithElements.values();
		/***to be removed**/
		for (Element element1: elements){
			System.out.println("user level key -" + element1.getObjectKey() + " value - "+ element1.getObjectValue());
		}
		/***to be removed**/
		return elements;
	}
	
	public Element getUserStickyNote(String key) {
		return userStickyCache.get(key);
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
	
	public Collection<Element> searchUserSticky(UserFilter userFilter) {
		
		QueryManager queryManager = QueryManagerBuilder
		        .newQueryManagerBuilder() 
		        .addCache(userStickyCache)
		        .build(); 
		
		Results filteredUserStickies = null;
		String searchUser = userFilter.getSearchUser();
		String searchActualValue = userFilter.getSearchValue();
		
		//getUserCacheElements();
		
		Query stickyKeyQuery = queryManager.createQuery("select * from userStickyCache where (user = '"+searchUser+"' and titleOrContent like '%"+searchActualValue+"%')").includeKeys().includeValues();
		
//		Query stickyKeyQuery = queryManager.createQuery("select * from userStickyCache where (user = '"+searchUser+"')").includeKeys().includeValues();
		
		filteredUserStickies = stickyKeyQuery.end().execute();
		
		System.out.println("user list stickies: "+filteredUserStickies.all());
		
		List<Element> elements = new ArrayList<Element>();
		for (Result filterSticky: filteredUserStickies.all()){
			System.out.println(filterSticky);
			UserStickyNote userNote  = (UserStickyNote) filterSticky.getValue();
			for(StickyNote note: userNote.getListSticky()) {
				Element element = new Element(note.getTitle(),note);
				elements.add(element);
			}
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
		Attribute<String> title = stickyCache.getSearchAttribute("title");
		Attribute<String> content = stickyCache.getSearchAttribute("content");
		
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
				Query stickyKeyQuery = queryManager.createQuery("select * from stickyCache where title like '%"+searchActualValue+"%'").includeKeys().includeValues();
				filteredStickies = stickyKeyQuery.end().execute();
				
				searchStickies = searchResults(queryManager,searchActualValue);
				break;
			}
			case SEARCH_BY_VALUE:{
				filteredStickies = stickyCache.createQuery().addCriteria(content.ilike(searchValue)).includeKeys().includeValues().execute();
				searchStickies = searchResults(queryManager,searchActualValue);
				break;
			}
			case SEARCH_BY_KEY_AND_VALUE: {
				filteredStickies = stickyCache.createQuery().addCriteria(title.ilike(searchValue).or(content.ilike(searchValue))).includeKeys().includeValues().execute();
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
			//Element element = new Element(filterSticky.getKey()+" ( "+searchCount+" )", filterSticky.getValue());
			Element element = new Element(filterSticky.getKey(), filterSticky.getValue());
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
