package com.rsrini.stickycache.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.rsrini.stickycache.domain.StickyNote;

import net.sf.ehcache.constructs.blocking.CacheEntryFactory;

public class StickyNoteDBReadThrough  implements CacheEntryFactory{
	
	@Override
	public Object createEntry(Object key) throws Exception {
		Connection connect = DBUtil.getConnection();
	    Statement st = null;
	    ResultSet rs = null;
	    StickyNote stickyNote = new StickyNote();
	    
		try {
			
			st = connect.createStatement();
		    rs = st.executeQuery("select * from STICKYCACHE where title='" + key + "'");
		    
		    System.out.println("Retrieving stickynote for the title " + key + " from DB ... ");
		    
		    while (rs.next()) {
		    	
		    	String user = rs.getString("USER");
		    	String title = rs.getString("TITLE");
		    	String content = rs.getString("CONTENT");
		     
		    	stickyNote.setUser(user);
		    	stickyNote.setTitle(title);
		    	stickyNote.setContent(content);
		    }
		    
		    System.out.println("Writing object " + key + " to the cache ... ");
		   // return new Element(stickyNote.getTitle(),stickyNote);
		
		} finally {
			if(rs != null) 	rs.close();
			if(st != null) st.close();
			//connect.close();
		}

		return stickyNote;
	}
	
}
