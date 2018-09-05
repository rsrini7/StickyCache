package com.rsrini.stickycache.util;

import java.util.Collection;
import java.util.Collections;

import com.rsrini.stickycache.domain.StickyNote;
import com.rsrini.stickycache.domain.StickyNoteFilter;

import net.sf.ehcache.constructs.blocking.CacheEntryFactory;

public class StickyNoteDBReadThrough  implements CacheEntryFactory{
	
	@Override
	public Object createEntry(Object key) throws Exception {
		
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
	
}
