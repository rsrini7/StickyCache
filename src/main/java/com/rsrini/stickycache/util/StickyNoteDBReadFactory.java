package com.rsrini.stickycache.util;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.CacheDecoratorFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;

public class StickyNoteDBReadFactory  extends CacheDecoratorFactory {
	
	@Override
	public Ehcache createDecoratedEhcache(Ehcache cache, Properties properties) {
		return new SelfPopulatingCache(cache, new StickyNoteDBReadThrough());
	}

	@Override
	public Ehcache createDefaultDecoratedEhcache(Ehcache cache, Properties properties) {
		return createDecoratedEhcache(cache, properties);
	}

}
