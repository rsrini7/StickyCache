package com.rsrini.stickycache.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.devskiller.jfairy.Fairy;
import com.devskiller.jfairy.producer.person.Person;
import com.devskiller.jfairy.producer.text.TextProducer;
import com.rsrini.stickycache.domain.StickyNote;
import com.rsrini.stickycache.domain.StickyNoteFilter;
import com.rsrini.stickycache.domain.StickyNoteFilter.StickySearchType;

import net.sf.ehcache.Element;

public class StickyCacheDataUtil {
	
	public static List<Element> generateElementData(int count){
		Person person = null;
		StickyNote stickyNote = null;
		List<Element>  stickyNoteListElement = new ArrayList<>();
		for(int i=0;i<count;i++) {
			
			if(i%2==0)
				person = Fairy.create().person();
			
			TextProducer textProducer = Fairy.create().textProducer();
			
			stickyNote = new StickyNote(person.getFullName(),textProducer.word(),textProducer.sentence());
			
			stickyNoteListElement.add(new Element(stickyNote.getTitle(),stickyNote));
			
		}
		
		return stickyNoteListElement;
	}
	
	public static List<StickyNote> generateStickyNoteData(int count){
		Person person = null;
		StickyNote stickyNote = null;
		List<StickyNote>  stickyNoteList = new ArrayList<>();
		for(int i=0;i<count;i++) {
			
			if(i%2==0)
				person = Fairy.create().person();
			
			TextProducer textProducer = Fairy.create().textProducer();
			
			stickyNote = new StickyNote(person.getFullName(),textProducer.word(),textProducer.sentence());
			
			stickyNoteList.add(stickyNote);
		}
		
		return stickyNoteList;
	}
	
	public static void saveDataToDB(StickyNote stickyNote ) {
		try {
			PreparedStatement insert = null;
			String insertstmt = " insert into STICKYCACHE(USER,TITLE,CONTENT) values (?,?,?)";
			insert = DBUtil.getConnection().prepareStatement(insertstmt);
			insert.setString(1, stickyNote.getUser());
			insert.setString(2, stickyNote.getTitle());
			insert.setString(3, stickyNote.getContent());
			insert.execute();
			insert.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void saveDataToDB(List<StickyNote> stickyNoteList ) {
		PreparedStatement insertBatch = null;
		
		try {
			String insertstmt = " insert into STICKYCACHE(USER,TITLE,CONTENT) values (?,?,?)";
			
			insertBatch = DBUtil.getConnection().prepareStatement(insertstmt);
			for(StickyNote stickyNote : stickyNoteList) {
				insertBatch.setString(1, stickyNote.getUser());
				insertBatch.setString(2, stickyNote.getTitle());
				insertBatch.setString(3, stickyNote.getContent());
				
				insertBatch.addBatch();
			}
			
			insertBatch.executeBatch();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}finally {
				try {
					if(insertBatch != null)
						insertBatch.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}

	public static Collection<StickyNote> retieveDataFromDB(StickyNoteFilter stickyFilter) {
		List<StickyNote> listStickies = new ArrayList<>();
		PreparedStatement pstmt = null;
		ResultSet rs=null;
		String selectstmt = "";
		try {
			
			StickySearchType searchType = stickyFilter.getSearchType();
			String searchValue = stickyFilter.getSearchValue();
			
			switch (searchType){
				case SEARCH_BY_KEY:{
					selectstmt = "select USER,TITLE,CONTENT from STICKYCACHE where TITLE LIKE ?";
					break;
				}
				case SEARCH_BY_KEY_AND_VALUE:{
					selectstmt = "select USER,TITLE,CONTENT from STICKYCACHE where TITLE LIKE ? or CONTENT LIKE ?";
					break;
				}
				case SEARCH_BY_VALUE:{
					selectstmt = "select USER,TITLE,CONTENT from STICKYCACHE where CONTENT LIKE ?";
					break;
				}
			}
			
			pstmt = DBUtil.getConnection().prepareStatement(selectstmt);
			pstmt.setString(1, "%"+searchValue+"%");
			
			if(searchType == StickySearchType.SEARCH_BY_KEY_AND_VALUE)
				pstmt.setString(2, "%"+searchValue+"%");
			
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				listStickies.add(new StickyNote(rs.getString("USER"),rs.getString("TITLE"),rs.getString("CONTENT")));
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			if(rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if(pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	
		return listStickies;
	}
	
}
