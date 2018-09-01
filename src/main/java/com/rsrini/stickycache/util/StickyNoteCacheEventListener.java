package com.rsrini.stickycache.util;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

public class StickyNoteCacheEventListener implements CacheEventListener,Cloneable {

	@Override
	public void dispose() {
	}

	@Override
	public void notifyElementEvicted(Ehcache cache, Element element) {
		 System.out.println(Thread.currentThread().getName() + ":listener evict " + element.getObjectKey().toString());
	}

	@Override
	public void notifyElementExpired(Ehcache cache, Element element) {
		System.out.println(Thread.currentThread().getName() + ":listener expired " + element.getObjectKey().toString());

	}

	@Override
	public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
		System.out.println(Thread.currentThread().getName() + ":listener element put " + element.getObjectKey().toString());

	}

	@Override
	public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
		System.out.println(Thread.currentThread().getName() + ":listener element removed " + element.getObjectKey().toString());

	}

	@Override
	public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
		System.out.println(Thread.currentThread().getName() + ":listener element updated " + element.getObjectKey().toString());

	}

	@Override
	public void notifyRemoveAll(Ehcache cache) {
		System.out.println(Thread.currentThread().getName() + ":listener remove all " + cache.getName());

	}
	

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	

}
