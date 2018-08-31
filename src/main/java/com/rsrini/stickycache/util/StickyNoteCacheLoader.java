package com.rsrini.stickycache.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.rsrini.stickycache.domain.StickyNote;

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
		LOGGER.info("***StickyNoteCacheLoader Loader" + key.toString() + "***");
		
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
		    
		    System.out.println("loading object " + key);
		   // return new Element(stickyNote.getTitle(),stickyNote);
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if(rs != null)
				try {
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if(st != null)
				try {
					st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//connect.close();
		}
		
		return stickyNote;
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