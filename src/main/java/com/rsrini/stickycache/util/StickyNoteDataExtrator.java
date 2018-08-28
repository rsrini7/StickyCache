package com.rsrini.stickycache.util;

import java.util.Properties;

import com.rsrini.stickycache.domain.StickyNote;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeExtractorException;

public class StickyNoteDataExtrator implements AttributeExtractor {

	private static final long serialVersionUID = 1L;
	
	public StickyNoteDataExtrator(Properties properties){
	}
	
	
	
	public Object attributeFor(Element element, String attributeName)
			throws AttributeExtractorException {
		StringBuffer result = new StringBuffer();

		StickyNote stickyNote = (StickyNote) element.getObjectValue();

		result.append(stickyNote.getTitle()+"~"+stickyNote.getContent()+",");
		
		return result.toString();
	}
}
