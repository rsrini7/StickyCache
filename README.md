<h1 align="center">Welcome to StickyCache 👋</h1>
<p>
 StickyCache is a POC using EhCache OpenSource / Commercial Features
</p>

##### Objective

    Design / develop an application using EhCache - Terracotta BigMemory Framework
        - Capture the understanding of the basic EhCache framework 
        - Evaluate and leverage the advantages / challenges of its features.

##### Features

    EhCache is an open-source caching framework for faster data retrieval.
        - JVM In-Memory Cache 
        - Disk Store Cache
        - Off-Heap Cache (Commercial)
        - Distributed Cache (Commercial)
        - Sql / Criteria query on Single Cache (Commercial)

##### Below features are available as part of POC
    
    Open-Source Version:

    1. Simple Cache : Store and load data using key / value pair. 
    2. Disk store : Store the cache data into disk for later retrieval.

    Commercial Version:

    1. Big Memory Go : Off-Heap cache
    2. Big Memory Max : Off-Heap and distributed cache using Terracotta Cluster Server
    3. Searchable Cache : Store the data and index for faster data retrieval.
    4. Searchable Cache : Criteria / SQL
      - Build java based criteria api on single cache - open source
      - Provide SQL query on single cache - BigMemory Max
    5. Blocking Cache - Block multiple get request, if any and provide data once received.
    6. Self Populating Cache : Special type of blocking cache for loading value for the given key from anywhere (Eg: Database)
    7. Cache Loader : Load data from outside, only if the key is not available in cache
    8. Cache Extension : Extend additional feature (Ex: Evict the expired data / sync data between DB and cache using threads.)
    9. Cache Event Notifications : Get notification on cache events (Eg: put, delete, etc.,)
    10. Cache Writer : Single / Inbuilt multi threaded data sync with external system (Ex : Store the data into DB)
    11. Management console based on REST / UI
    12. Data snapshot and bootstrap the snapshot's data durability
    13. Easy to configure for high availability with terracotta cluster.


##### Challenges / Observations (May change, based on further exploration)

    * Search feature does not support multi cache. Api is not available to obtain relationship between two or more caches
        - Need to retrieve each cache data separately and do joins by our own logic
        - May require to maintain additional caches for the combined one.
    * Inconsistent snapshot and bootstrap data store / retrieval behaviors (may be bug in my code)
    * "And" search behaves like "or" within single cache (using sql syntax provided by BigMemory Max)
    * Off Heap Cache provided to replace Disk based store adn remove JVM GC overhead for faster retrieval / store
        - Not available in open source version and only available in BigMemory Commercial
    * Very limited forums available for BigMemory related issues.

##### Implementation details
    1. Simple POC for add / edit / delete sticky note into cache
    2. Multipel caches for exploring features
        - stickyCache, searchCache & userStickyCache
    3. Generate data and load bulk into cache
    4. Search by key, value and maintain searched data
    5. Download sticky notes, searched data
    6. Load data directly into DB and search only in DB

##### Libraries Used
    1. Spring Boot with MVC, Themeleaf
    2. H2Database
    3. BigMemory Max - Terracotta Cluster Server
    4. Terracotta Management Console
    5. Jfairy - Fake data generation

## Show your support

Give a ⭐️ if this project helped you!

