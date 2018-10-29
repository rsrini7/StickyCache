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
import com.rsrini.stickycache.util.StickyCacheDataUtil;
import com.rsrini.stickycache.util.StickyNoteCacheListenerFactory;
import com.rsrini.stickycache.util.StickyNoteCacheLoader;
import com.rsrini.stickycache.util.StickyNoteCollectionExtrator;
import com.rsrini.stickycache.util.StickyNoteDBReadThrough;
import com.rsrini.stickycache.util.StickyNoteDBWriterFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.extension.CacheExtension;
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

	 //"expired" (time of creation + time to live < now)
	private static final int expireThreadSeconds = 60*30; //30 mins (60 * 30)

	
	private final static String CACHE_POLICY = "LRU"; //LFU
	
	@Value("${sticky.cache.runMode}")
	private String runCacheMode;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
	private MultiThreadedCacheLoader multiThreadedCacheLoader;
	
	private Ehcache stickyCache;
	private Ehcache searchCache;
	private Ehcache userStickyCache;
	
	private SelfPopulatingCache replaceWithSelfPopulatingCache;
	
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
	    
	    //replace with dbreadthrough - self populating cache
	    replaceWithSelfPopulatingCache = getSelfPopulatingCache();
	    //manager.replaceCacheWithDecoratedCache(stickyCache, replaceWithSelfPopulatingCache); // only selfpopulating ref with refresh works
	    
	    //loader registration
	    stickyCache.registerCacheLoader(new StickyNoteCacheLoader());
	    
	    userStickyCache = manager.getEhcache(USER_CACHE_NAME);
	    
	    //userStickyCache.setNodeBulkLoadEnabled(true);
	    
	    if(expireThreadSeconds > 0) {
	    	EhCacheExtension ehCacheExtension = new EhCacheExtension(USER_CACHE_NAME,expireThreadSeconds);
	    	ehCacheExtension.init();
	    	userStickyCache.registerCacheExtension(ehCacheExtension);
		}
	    
	    userStickyCache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter() {
	    	@Override
	    	public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
	    		System.out.println(Thread.currentThread().getName() + ":UserCacheService user sticky cache put from adaptor " + element.getObjectKey().toString());
	    	}
	        
	      });
	}

	private SelfPopulatingCache getSelfPopulatingCache() {
		SelfPopulatingCache selfPopulatingCache = new SelfPopulatingCache(stickyCache, new StickyNoteDBReadThrough());
		selfPopulatingCache.setTimeoutMillis(100);
		return selfPopulatingCache;
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
		 
		 cacheConfig.addCacheWriter(cacheWriterConfig(StickyNoteDBWriterFactory.class.getName()));
		 
		 cacheConfig.setMemoryStoreEvictionPolicy(CACHE_POLICY);
		 
		 //cacheConfig.addCacheDecoratorFactory(cacheDecorator(StickyNoteDBReadFactory.class.getName())); //Registering - but not retriving from db. how to refresh ?

		 //cacheConfig.addCacheLoaderFactory(cacheLoaderFactoryConfiguration(StickyNoteCacheLoaderFactory.class.getName()));
		 
		 cacheConfig.addCacheEventListenerFactory(getCacheEventListnerConfig(StickyNoteCacheListenerFactory.class.getName()));
		 
		 return cacheConfig;
	}
	
	private CacheEventListenerFactoryConfiguration getCacheEventListnerConfig(String className) {
		return new CacheEventListenerFactoryConfiguration().className(className).listenFor("all");
	}

/*	private CacheDecoratorFactoryConfiguration cacheDecorator(String className) {
		return new CacheDecoratorFactoryConfiguration().className(className);
	}*/
	
/*	private CacheLoaderFactoryConfiguration cacheLoaderFactoryConfiguration(String className) {
		return new CacheLoaderFactoryConfiguration().className(className).properties("type=int;startCounter=10")
				.propertySeparator(";");
	}*/

	private CacheConfiguration getUserCacheConfig() {
		 CacheConfiguration cacheConfig = new CacheConfiguration().name(USER_CACHE_NAME)
		        .eternal(true)
		        .maxBytesLocalHeap(1, MemoryUnit.MEGABYTES)
		        .maxBytesLocalOffHeap(128, MemoryUnit.MEGABYTES)
		        .copyOnRead(false)
		        //.terracotta(new TerracottaConfiguration().consistency(TerracottaConfiguration.Consistency.STRONG))
		        .searchable(new Searchable()
		            .searchAttribute(new SearchAttribute().name("user").expression("value.getName()"))
		            //.searchAttribute(new SearchAttribute().name("titleOrContent").className(StickyNoteDataExtrator.class.getName()).properties("value.getTitleContent()"))
		            .searchAttribute(new SearchAttribute().name("titleOrContent").className(StickyNoteCollectionExtrator.class.getName()).properties("value.getName()")) // dummy property required to boot the application
		    		);
		
/*		 cacheConfig.terracotta(new TerracottaConfiguration()
					.consistency(TerracottaConfiguration.Consistency.STRONG)
					.nonstop(new NonstopConfiguration().enabled(true).timeoutMillis(4000)
						.timeoutBehavior(new TimeoutBehaviorConfiguration()
						.type(TimeoutBehaviorConfiguration.TimeoutBehaviorType.LOCAL_READS.getTypeName())))
						);*/
		 
		return cacheConfig;
	}
	
	private CacheWriterConfiguration cacheWriterConfig(String className) {
		
		return new CacheWriterConfiguration().writeMode(CacheWriterConfiguration.WriteMode.WRITE_BEHIND)
				.maxWriteDelay(3).rateLimitPerSecond(10).notifyListenersOnException(true).writeCoalescing(true)
				.writeBatching(true).writeBatchSize(2).retryAttempts(5).retryAttemptDelaySeconds(5).writeBehindConcurrency(2)
				.cacheWriterFactory(
						new CacheWriterConfiguration.CacheWriterFactoryConfiguration().className(className)
						.properties("url=jdbc:h2:mem:stickycache;id=sa;pw=sa").propertySeparator(";")
						);

		//jdbc:h2:tcp://localhost:8000/~/stickycache
		//jdbc:h2:tcp://localhost:8000/mem:stickycache_db;DB_CLOSE_DELAY=-1;
		//jdbc:h2:tcp://localhost:9092/~/stickycache_db;DB_CLOSE_DELAY=-1
		//DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE
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
		//System.out.println(" element to be added to usercache "+element);
	}
	
	public Collection<Element> addToCache(Element element, boolean writeBehindDbFlag){
		if(writeBehindDbFlag) {
			stickyCache.putWithWriter(element);
		}else {
			stickyCache.put(element);
		}
	//	System.out.println(" element to be added "+element);
		return getCacheElements();
	}
	
	public Collection<Element> getUserCacheElements(){
		Map<Object, Element> keysWithElements = userStickyCache.getAll(userStickyCache.getKeys());
		System.out.println("userlevel key set::::: "+keysWithElements.keySet());
		Collection<Element> elements = keysWithElements.values();
		/***to be removed**/
		/*for (Element element1: elements){
			System.out.println("user level key -" + element1.getObjectKey() + " value - "+ element1.getObjectValue());
		}*/
		/***to be removed**/
		return elements;
	}
	
	public Element getUserStickyNote(String key) {
		return userStickyCache.get(key);
	}
	
	public Collection<Element> getCacheElements(){
		Map<Object, Element> keysWithElements = stickyCache.getAll(stickyCache.getKeys());
		//System.out.println("key set::::: "+keysWithElements.keySet());
		Collection<Element> elements = keysWithElements.values();
		/***to be removed**/
		//System.out.println("context property source bean injection" + applicationContext);
		//System.out.println("cache mode"+runCacheMode);
		/*for (Element element1: elements){
			System.out.println("key -" + element1.getObjectKey() + " value - "+ element1.getObjectValue());
		}*/
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
			//System.out.println(filterSticky);
			UserStickyNote userNote  = (UserStickyNote) filterSticky.getValue();
			for(StickyNote note: userNote.getListSticky()) {
				Element element = new Element(note.getTitle(),note);
				elements.add(element);
			}
		}
		return elements;
		
	}

	public Collection<Element> searchSticky(StickyNoteFilter stickyFilter) {
		
		String searchActualValue = stickyFilter.getSearchValue();
		
		if(stickyFilter.isLoadFromDB()) {
			Element element = stickyCache.getWithLoader(searchActualValue,new StickyNoteCacheLoader(),null); //called only when key not in memory
			stickyCache.putIfAbsent(element);
			System.out.println("data from db and loaded to cache : "+element.getObjectValue());
		}
		
		QueryManager queryManager = QueryManagerBuilder
		        .newQueryManagerBuilder() 
		        .addCache(stickyCache)
		        .addCache(searchCache)
		        .build(); 
		
		Attribute<String> title = stickyCache.getSearchAttribute("title");
		Attribute<String> content = stickyCache.getSearchAttribute("content");
		
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
				
/*				Element element = replaceWithSelfPopulatingCache.get(searchActualValue); //not returning from db
				System.out.println("self populating cache : "+element);
				if(element != null && element.getObjectValue() != null) {*/
					replaceWithSelfPopulatingCache.refresh(searchActualValue);
				//}
				
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

	public void generateAndLoadStickyNotesIntoCache(int count) {

		List<StickyNote> stickyNoteDataList = StickyCacheDataUtil.generateStickyNoteData(count);
		List<Element> stickyCacheElementList = new ArrayList<>();
		
		for(StickyNote note : stickyNoteDataList) {
			stickyCacheElementList.add(new Element(note.getTitle(),note));
			addToUserCache(note, false);
		}
		stickyCache.putAll(stickyCacheElementList);
		
	}
	
	public void addToUserCache(StickyNote stickyNote, boolean writeToDB) {
		Element userStickyElement = getUserElement(stickyNote);
		addToUserCache(userStickyElement, writeToDB);
	}

	public Element getUserElement(StickyNote stickyNote) {
		Element existingUserStickyElement = getUserStickyNote(stickyNote.getUser());
		Element userStickyElement = null;
		if(existingUserStickyElement == null) {
			userStickyElement = new Element(stickyNote.getUser(), new UserStickyNote(stickyNote.getUser(), stickyNote.getTitle(), stickyNote.getContent()));
		}else {
			 UserStickyNote objectValue = (UserStickyNote) existingUserStickyElement.getObjectValue();
			 objectValue.getListSticky().add(new StickyNote(stickyNote.getUser(), stickyNote.getTitle(), stickyNote.getContent()));
			 userStickyElement = existingUserStickyElement;
		}
		return userStickyElement;
	}

	public void generateAndBulkLoadStickyNotesIntoCache(int count) {
		
		multiThreadedCacheLoader.loadData(stickyCache, count);
		
		try {
			multiThreadedCacheLoader.shutdownAndWaitFor(10);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Collection<Element> getTopSearchCacheElements(int count) {

		List<Element> elements = new ArrayList<Element>();
		
		QueryManager queryManager = QueryManagerBuilder
		        .newQueryManagerBuilder() 
		        .addCache(stickyCache)
		        .addCache(searchCache)
		        .build(); 
		
		String queryStr = "select * from searchCache order by value desc limit "+count;
		System.out.println("Search Cache Query: "+ queryStr);
		Query searchStickyKeyQuery = queryManager.createQuery(queryStr).includeKeys().includeValues();
		Results searchStickies = searchStickyKeyQuery.end().execute();

		for (Result filterSearchSticky: searchStickies.all()){
			queryStr = "select * from stickyCache where (title  like '%"+filterSearchSticky.getKey()+"%' or content  like '%"+filterSearchSticky.getKey()+"%')";
			System.out.println("Sticky Cache Query: "+ queryStr);
			Query stickyKeyQuery = queryManager.createQuery(queryStr).includeKeys().includeValues();
			Results stickies = stickyKeyQuery.end().execute();
			
			for (Result filterSticky: stickies.all()){
				Element element = new Element(filterSticky.getKey(), filterSticky.getValue());
				elements.add(element);
			}
		}

		return elements;
		
	}

	public void clearAllCache() {
		stickyCache.removeAll();
		searchCache.removeAll();
		userStickyCache.removeAll();
	}

	public void generateAndLoadStickyNotesIntoDBOnly(int count) {
		List<StickyNote> stickyNotes = StickyCacheDataUtil.generateStickyNoteData(count);
		System.out.println("Generated data for DB before inserting to DB : "+stickyNotes);
		StickyCacheDataUtil.saveDataToDB(stickyNotes);
	}

	public Collection<StickyNote> searchStickyInDB(StickyNoteFilter stickyFilter) {
		return StickyCacheDataUtil.retieveDataFromDB(stickyFilter);
	}
	

	public static class EhCacheExtension implements CacheExtension {
		private String cacheName;
		private EvictionThread evictThread;
		private Thread thread;
		private int seconds;

		public EhCacheExtension(final String cacheName,final int seconds) {
			this.seconds = seconds;
			this.cacheName = cacheName;
		}

		public CacheExtension clone(final net.sf.ehcache.Ehcache arg0) throws CloneNotSupportedException {
			return new EhCacheExtension(arg0.getName(),seconds);
		}

		public void dispose() throws CacheException {
			evictThread.kill();
			thread.interrupt();
			evictThread.kill();
		}

		public Status getStatus() {
			return Status.STATUS_ALIVE;
		}

		public void init() {
			evictThread = new EvictionThread(cacheName,seconds);
			thread = new Thread(evictThread);
			thread.setName(cacheName+"-expire");
			thread.start();
		}

	}

	public static class EvictionThread implements Runnable {
		private String cacheName;
		private int seconds;
		private boolean run = true;

		public EvictionThread(final String cacheName,final int seconds) {
			this.cacheName = cacheName;
			this.seconds = seconds;
		}

		public void kill() {
			run = false;
		}

		public void run() {
			while(run) {
				Ehcache cache = CacheManager.getInstance().getCache(cacheName);
				if(cache != null) {
					cache.evictExpiredElements();
				}
				try {
					Thread.sleep(1000*seconds);
				} catch(Exception e) {
				}
			}
		}
	}
	
}
