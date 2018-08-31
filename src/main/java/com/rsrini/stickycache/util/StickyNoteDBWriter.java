package com.rsrini.stickycache.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.rsrini.stickycache.domain.StickyNote;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

public class StickyNoteDBWriter implements CacheWriter {

	static Logger logger = Logger.getLogger(StickyNoteDBWriter.class);

	public StickyNoteDBWriter(String iurl, String iuser, String ipass) {
		try {
			DBUtil.registerDB(iurl,iurl,ipass);
		} catch (Exception ex) {
			logger.error(ex);
		}
	}

	public CacheWriter clone(final Ehcache ehcache)
			throws CloneNotSupportedException {
		throw new CloneNotSupportedException("StickyNoteDBCacheWriter cannot be cloned!");
	}

	public void init() {
		try {
			Connection conn = DBUtil.getConnection();
			conn.setAutoCommit(true);
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS STICKYCACHE(USER VARCHAR(255), TITLE VARCHAR(255), CONTENT VARCHAR(255))");
			logger.info(" Table STICKYCACHE CREATED.");
		} catch (Exception e) {
			throw new CacheException("Couldn't conenct to DB", e);
		}
	}

	public void dispose() throws CacheException {
		try {
			DBUtil.getConnection().close();
		} catch (Exception e) {
			throw new CacheException("Couldn't close db connection" + e);
		}
	}

	public synchronized void write(final Element element) throws CacheException {

		try {

			StickyNote stickyNote = (StickyNote) element.getObjectValue();
			PreparedStatement insert = null;
			String insertstmt = " insert into STICKYCACHE(USER,TITLE,CONTENT) values (?,?,?)";
			insert = DBUtil.getConnection().prepareStatement(insertstmt);
			insert.setString(1, stickyNote.getUser());
			insert.setString(2, stickyNote.getTitle());
			insert.setString(3, stickyNote.getContent());
			insert.execute();
			insert.close();

		} catch (Exception ex) {
			logger.error(ex);
		}

	}

	public synchronized void writeAll(final Collection<Element> elements)
			throws CacheException {
		for (Element element : elements) {
			write(element);
		}
	}

	public synchronized void delete(final CacheEntry cacheEntry)
			throws CacheException {
		// noop
	}

	public synchronized void deleteAll(final Collection<CacheEntry> cacheEntries)
			throws CacheException {
		// noop
	}

	public void throwAway(Element arg0, SingleOperationType arg1,
			RuntimeException arg2) {
		// TODO Auto-generated method stub

	}
	
}
