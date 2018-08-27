package com.rsrini.stickycache.util;

import java.util.Iterator;
import java.util.Properties;

import com.rsrini.stickycache.domain.StickyNote;
import com.rsrini.stickycache.domain.User;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeExtractorException;

public class StickyNoteCollectionExtrator implements AttributeExtractor {

	private static final long serialVersionUID = 1L;
	
	public StickyNoteCollectionExtrator(Properties properties){
	}
	
	
	public Object attributeFor(Element element, String attributeName)
			throws AttributeExtractorException {
		//LOG.info("attributeName"+attributeName);
		StringBuffer result = new StringBuffer();

		User user = (User) element.getObjectValue();
		Iterator i = user.getListSticky().iterator();

		while (i.hasNext()) {
			StickyNote stickyNote  = (StickyNote)i.next();
			result.append(stickyNote.getTitle()+"~"+stickyNote.getContent()+",");
		}
		return result.toString();
	}
}
