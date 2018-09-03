package com.rsrini.stickycache.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.devskiller.jfairy.Fairy;
import com.devskiller.jfairy.producer.person.Person;
import com.devskiller.jfairy.producer.text.TextProducer;
import com.rsrini.stickycache.domain.StickyNote;

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
	
}
