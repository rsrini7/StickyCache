package com.rsrini.stickycache.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.devskiller.jfairy.Fairy;
import com.devskiller.jfairy.producer.person.Person;
import com.devskiller.jfairy.producer.text.TextProducer;
import com.rsrini.stickycache.domain.StickyNote;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

@Component
public class MultiThreadedCacheLoader {

	private int batchSize = 10; //default batch size
	private int currentSize = 0;
	private ExecutorService executor;
	private List<Element> putsList = new ArrayList<>();
	private Ehcache cache;

	@Autowired
	private UserCacheService cacheService;

	public MultiThreadedCacheLoader() {
		
	}
	
	public void loadData(Ehcache cache, int batchSize) {
		
		if(batchSize > 0)
			this.batchSize = batchSize;
		
		// start the thread pool
		int numberOfProcessors = Runtime.getRuntime().availableProcessors();
		System.out
				.println("Using a threadpool of " + numberOfProcessors
						+ " with batch size of " + batchSize
						+ " to perform batch load");
		this.executor = Executors.newFixedThreadPool(numberOfProcessors);
		this.cache = cache;
		
		Person person = Fairy.create().person();
		
		for(int i =0 ;i<batchSize;i++) {
			
			if(i % 5 == 0 )
				person = Fairy.create().person();
			
			StickyNote stickyNote = generateData(person);
			
			Element element = new Element(stickyNote.getTitle(), stickyNote);
			//Element userElement = cacheService.getUserElement(stickyNote);
			cacheService.addToUserCache(stickyNote,false);
			
			
			put(element);
		}
	}

	public void put(Element element) {
		// if batch size is met, submit job, else add it to the task
		if (currentSize == batchSize - 1) {
			currentSize = 0;
			putsList.add(element);
			executor.execute(new PutBatchWork(cache, putsList));
			putsList = new ArrayList<Element>();
		} else {
			putsList.add(element);
			currentSize++;
		}
	}

	public boolean shutdownAndWaitFor(int timeInSeconds) throws Exception {
		executor.execute(new PutBatchWork(cache, putsList));
		executor.shutdown();
		boolean isComplete = executor.awaitTermination(timeInSeconds,
				TimeUnit.SECONDS);
		return isComplete;
	}

	class PutBatchWork implements Runnable {
		Ehcache cache;
		List<Element> putsCacheList;

		PutBatchWork(Ehcache cache, List<Element> putsCacheList) {
			this.cache = cache;
			this.putsCacheList = putsCacheList;
		}

		public void run() {
			cache.putAll(putsCacheList);
			putsCacheList.clear();
		}
	}
	
	private StickyNote generateData(Person person) {
		TextProducer textProducer = Fairy.create().textProducer();
		StickyNote stickyNote = new StickyNote(person.getFullName(),textProducer.word(),textProducer.sentence());
		
		return stickyNote;
	}

}
