package com.rsrini.stickycache.controller;


import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.util.StringUtils;

import com.rsrini.stickycache.domain.StickyNote;
import com.rsrini.stickycache.domain.StickyNoteFilter;
import com.rsrini.stickycache.services.CacheService;

import net.sf.ehcache.Element;

@Controller
@RequestMapping("/sticky")
public class StickyController {
	
	private static final int BUFFER_SIZE = 4096;
	
	private final CacheService cacheService;
	
	@Autowired
	public StickyController(CacheService cacheService){
		this.cacheService = cacheService;
	}
	
	@RequestMapping("/add")
	public String addToSticky(@RequestParam(value="key", required=false, defaultValue = "default") String key, 
				            @RequestParam(value="value", required=false) String value, Model model){
		System.out.println("entered into add sticky"+key);
		Element element = new Element(key, value);
		Collection<Element> stickyRecords  = cacheService.addToCache(element);
		model.addAttribute("stickyRecords", stickyRecords);
		System.out.println("elements in sticky records - "+stickyRecords);
		return "sticky";
	}
	
	@RequestMapping("/delete/{key}")
	public String deletetStickyNote(@PathVariable("key") String key, Model model){
		System.out.println("Executing the delete method");
		if (!cacheService.removeCacheElement(key)){
			System.out.println("***could not be deleted!!!***");
		}
		Collection<Element> stickyRecords  = cacheService.getCacheElements();
		model.addAttribute("stickyRecords", stickyRecords);
		model.addAttribute("allSearchTypes", StickyNoteFilter.StickySearchType.values());
		model.addAttribute("stickyNote", new StickyNote());
		model.addAttribute("stickyFilter", new StickyNoteFilter());
		return "sticky";
	}
	
	@RequestMapping("/save")
	public String saveToSticky(@ModelAttribute StickyNote stickyNote, Model model){
		System.out.println("sticky element"+model.asMap().get("stickyNote"));
		System.out.println("sticky element value"+stickyNote);
		Element stickyElement = new Element(stickyNote.getTitle(), stickyNote.getContent());
		Collection<Element> stickyRecords  = cacheService.addToCache(stickyElement);
		model.addAttribute("allSearchTypes", StickyNoteFilter.StickySearchType.values());
		model.addAttribute("stickyRecords", stickyRecords);
		model.addAttribute("stickyNote", new StickyNote());
		model.addAttribute("stickyFilter", new StickyNoteFilter());
		return "sticky";
	}
	
	@RequestMapping("/search")
	public String searchSticky(@ModelAttribute StickyNoteFilter stickyFilter, Model model){
		Collection<Element> stickyRecords = cacheService.searchSticky(stickyFilter);
		model.addAttribute("stickyRecords", stickyRecords);
		model.addAttribute("stickyFilter", new StickyNoteFilter());
		model.addAttribute("stickyNote", new StickyNote());
		model.addAttribute("allSearchTypes", StickyNoteFilter.StickySearchType.values());
		return "sticky";
	}
	
	@RequestMapping(value={"/view",""})
	public String viewSticky(Model model){
		System.out.println("sticky view method is invoked");
		Collection<Element> stickyRecords  = cacheService.getCacheElements();
		model.addAttribute("stickyRecords", stickyRecords);
		String searchTypeStrings[] = new String[3];
		int i = 0;
		for (StickyNoteFilter.StickySearchType searchType : StickyNoteFilter.StickySearchType.values()){
			searchTypeStrings[i++] = searchType.name();
		}
		model.addAttribute("allSearchTypes", StickyNoteFilter.StickySearchType.values());
		
		//if sticky note is not present, new object has been created and added..
		if (!model.containsAttribute("stickyNote")){
			model.addAttribute("stickyNote", new StickyNote());
		}
		
		//if sticky filter element is not present, new object has been created and added..
		if (!model.containsAttribute("stickyFilter")){
			model.addAttribute("stickyFilter", new StickyNoteFilter());
		}
		
		return "sticky";
	}
	
	@RequestMapping(value={"/download"})
	public void downloadSticky(Model model, HttpServletResponse response){
		System.out.println("Downloading of sticky notes starts...");
		String NEWLINE_CHARACTER = "\n\r";
		Collection<Element> stickyRecords = cacheService.getCacheElements();
		FileWriter fileWriter = null;
		BufferedWriter bw = null;
		try {
			fileWriter = new FileWriter("stickynotes.txt");
			bw = new BufferedWriter(fileWriter);
			//write buffered output stream..
			for (Element stickyRecord: stickyRecords){
				bw.write(StringUtils.repeat("*", 30));
				bw.write(NEWLINE_CHARACTER);
				
				bw.write(StringUtils.repeat(">", 10));
				bw.write(stickyRecord.getObjectKey().toString());
				bw.write(NEWLINE_CHARACTER);
				bw.write(StringUtils.repeat("*", 30));
				bw.write(NEWLINE_CHARACTER);
				
				bw.write(stickyRecord.getObjectValue().toString());
				bw.write(NEWLINE_CHARACTER);
				
				bw.write(StringUtils.repeat("*", 30));
				bw.write(NEWLINE_CHARACTER);
				bw.write(NEWLINE_CHARACTER);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//writing to HTTP response.
		response.addHeader("Content-Disposition", "attachment; filename=stickynotes.txt");
		FileInputStream fis = null;
		OutputStream out = null;
		byte[] bytes = new byte[BUFFER_SIZE];
		int bytesRead = 0;

		try {
			fis = new FileInputStream("stickynotes.txt");
			out = response.getOutputStream();
			while((bytesRead = fis.read(bytes)) != -1){
				out.write(bytes,0, bytesRead);
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally{
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		model.addAttribute("stickyRecords", stickyRecords);
//		model.addAttribute("stickyFilter", new StickyFilter());
//		model.addAttribute("stickyNote", new StickyNote());
//		model.addAttribute("allSearchTypes", StickyNoteFilter.StickySearchType.values());
//		System.out.println("Sticky download completed.");
//		return "sticky";
	}
	
	@RequestMapping(value={"/searchdownload"})
	public void downloadSearchSticky(Model model, HttpServletResponse response){
		System.out.println("Downloading of sticky notes searches...");
		String NEWLINE_CHARACTER = "\n\r";
		Collection<Element> stickyRecords = cacheService.getSearchedCacheElements();
		FileWriter fileWriter = null;
		BufferedWriter bw = null;
		try {
			fileWriter = new FileWriter("searchsticky.txt");
			bw = new BufferedWriter(fileWriter);
			//write buffered output stream..
			for (Element stickyRecord: stickyRecords){
				bw.write(StringUtils.repeat("*", 20));
				bw.write(NEWLINE_CHARACTER);
				
				bw.write(StringUtils.repeat(">", 5));
				bw.write(stickyRecord.getObjectKey().toString());
				bw.write(NEWLINE_CHARACTER);
				bw.write(StringUtils.repeat("*", 20));
				bw.write(NEWLINE_CHARACTER);
				
				bw.write(stickyRecord.getObjectValue().toString());
				bw.write(NEWLINE_CHARACTER);
				
				bw.write(StringUtils.repeat("*", 20));
				bw.write(NEWLINE_CHARACTER);
				bw.write(NEWLINE_CHARACTER);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//writing to HTTP response.
		response.addHeader("Content-Disposition", "attachment; filename=searchsticky.txt");
		FileInputStream fis = null;
		OutputStream out = null;
		byte[] bytes = new byte[BUFFER_SIZE];
		int bytesRead = 0;

		try {
			fis = new FileInputStream("searchsticky.txt");
			out = response.getOutputStream();
			while((bytesRead = fis.read(bytes)) != -1){
				out.write(bytes,0, bytesRead);
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally{
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
