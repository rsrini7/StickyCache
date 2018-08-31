package com.rsrini.stickycache.util;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterFactory;

public class StickyNoteDBWriterFactory extends CacheWriterFactory {

	@Override
	public CacheWriter createCacheWriter(Ehcache ehCache, Properties properties) {
		// TODO Auto-generated method stub
		 return new StickyNoteDBWriter((String)properties.get("url"),(String)properties.get("id"),(String)properties.get("pw"));
	}

}
