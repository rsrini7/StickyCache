package com.rsrini.stickycache.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Logger;

import com.rsrini.stickycache.domain.StickyNote;
import com.rsrini.stickycache.domain.StickyNoteFilter;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.loader.CacheLoader;

public class StickyNoteCacheLoader  implements CacheLoader {
	private static final Logger LOGGER = Logger.getLogger(StickyNoteCacheLoader.class);

	@Override
	public Object load(Object key, Object argument) throws CacheException {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map loadAll(Collection keys) {
		return null;
	}

	@Override
	public Object load(Object key) throws CacheException {
		LOGGER.info("***StickyNoteCacheLoader Loader : " + key.toString() + "***");
		
		StickyNote retStickyNote = null;
		Collection<StickyNote> retieveDataFromDB = Collections.emptyList();
			
		retieveDataFromDB = StickyCacheDataUtil.retieveDataFromDB(new StickyNoteFilter(key.toString()));
			
		System.out.println("retrieved from db: "+retieveDataFromDB);
			
		if(retieveDataFromDB != null && !retieveDataFromDB.isEmpty()) {
				for(StickyNote stickyNote : retieveDataFromDB) {
					retStickyNote = stickyNote;
					break;
				}
		}
			
		return retStickyNote;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map loadAll(Collection keys, Object argument) {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public CacheLoader clone(Ehcache cache) throws CloneNotSupportedException {
		return null;
	}

	@Override
	public void init() {

	}

	@Override
	public void dispose() throws CacheException {

	}

	@Override
	public Status getStatus() {
		return null;
	}

}