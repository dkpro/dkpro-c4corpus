# DKPro C4CorpusTools

NOTE: work in progress until 1.0.0 release

## Requirements

* Java 1.6 is required for compiling and running the project
* For running the Hadoop pipeline, Hadoop 2.6 is recommended
    * Running the pipeline on CommonCrawl located at S3 requires and active Amazon Web Services (AWS) account 

## Project structure

* ``dkpro-c4corpus-boilerplate`` contains a Java implementation of a state-of-the-art boilerplate removal (JusText, Pomikalek, 2011) 
* ``dkpro-c4corpus-deduplication`` implements near-duplicate content detection based on SimHash
* ``dkpro-c4corpus-hadoop`` contains several Hadoop Map/Reduce jobs for running the pipeline agains the CommonCrawl corpus
* ``dkpro-c4corpus-language`` provides language and encoding detection functionality
* ``dkpro-c4corpus-license`` implements Creative Commons license detection in html pages

Except ``dkpro-c4corpus-hadoop`` the modules are independent of each other and can run locally without Hadoop.

## Packaging

``$mvn package``

Will produce a _fat_ jar ``dkpro-c4corpus-hadoop-1.0.0.jar`` in ``de.tudarmstadt.ukp.dkpro.c4corpus.hadoop/target/``


## Run the whole pipeline on CommonCrawl corpus 
(dont copy paste the commands directly to the console, remove the line breaks & indentation first)

### Phase 1: all components 

This phase includes license detection, language detection, and boilerplate removal.

```
hadoop jar c4corpus-tools-1.0-SNAPSHOT.jar de.tudarmstadt.aiphes.c4corpus.hadoop.full.Phase1FullJob \
-Dmapreduce.job.queuename=longrunning \
/user/habernal/commoncrawl/*.warc.gz /user/username/commoncrawl-subset-phase1
```

TODO command line approach to launch complete cluster?
--maybe: http://docs.aws.amazon.com/cli/latest/reference/emr/create-cluster.html

2- Phase 2: ExactMatch De-duplication
    hadoop jar c4corpus-tools-1.0-SNAPSHOT.jar 
    de.tudarmstadt.aiphes.c4corpus.hadoop.full.Phase2ExactMatchDeduplication 
    -Dmapreduce.job.queuename=longrunning
    /user/username/commoncrawl-subset-phase1/*.warc.gz /user/username/commoncrawl-subset-phase2

3- Phase 3 Step 1: Extract Near Duplicates Info
    hadoop jar c4corpus-tools-1.0-SNAPSHOT.jar de.tudarmstadt.aiphes.c4corpus.hadoop.full.Phase3Step1ExtractNearDupInfo 
    -Dmapreduce.job.queuename=longrunning 
    /user/username/commoncrawl-subset-phase2/*.warc.gz /user/username/commoncrawl-subset-phase3-step1

4- Phase 3 Step 2: Distinct data
    hadoop jar c4corpus-tools-1.0-SNAPSHOT.jar de.tudarmstadt.aiphes.c4corpus.hadoop.full.Phase3Step2DistinctDataJob 
    -Dmapreduce.reduce.memory.mb=5120 
    -Dmapreduce.reduce.java.opts=-Xmx4096m 
    -Dmapreduce.job.queuename=longrunning 
    /user/username/commoncrawl-subset-phase3-step1/*.txt /user/username/commoncrawl-subset-phase3-step2

5- Phase 3 Step 3: Tuples creation
*it is better to delete the _SUCCESS file from the input file before performing this step
*the timeout must be disabled as while calculating the Hamming Distance 
*the map neither reads an input, writes an output, nor updates its status string 
*so it will fail after 3 hours
    hadoop jar c4corpus-tools-1.0-SNAPSHOT.jar de.tudarmstadt.aiphes.c4corpus.hadoop.full.Phase3Step3NearDupTuplesCreation 
    -Dmapreduce.map.memory.mb=5120 
    -Dmapreduce.map.java.opts=-Xmx4096m 
    -Dmapreduce.reduce.memory.mb=5120 
    -Dmapreduce.reduce.java.opts=-Xmx4096m
    -Dmapreduce.task.timeout=0 
    -Dmapreduce.job.queuename=longrunning 
    /user/username/commoncrawl-subset-phase3-step2/* /user/username/commoncrawl-subset-phase3-step3

6- Phase 3 Step 4: local greedy algorithm
The class to be used is: de.tudarmstadt.aiphes.c4corpus.c4corpustools.deduplication.impl.DocumentDeduplication 
the method to be used is: selectIDsToDelete
Arguments: args[0]: input directory name (from step 5), args[1]: output directory name


## Collecting word distribution statistics

1. Collect word statistics

```
hadoop jar c4corpus-tools-1.0-SNAPSHOT.jar de.tudarmstadt.aiphes.c4corpus.hadoop.statistics.vocabulary.WARCWordDistribution -Dmapreduce.job.queuename=shortrunning /user/habernal/lrec2015-ccweb-phase1/Lic_publicdomain_Lang_en*,/user/habernal/lrec2015-ccweb-phase1/Lic_cc-unspecified_Lang_en*,/user/habernal/lrec2015-ccweb-phase1/Lic_by*_Lang_en* /user/habernal/lrec2015-ccweb-phase1-vocabulary


-- common crawl cc
hadoop jar c4corpus-tools-1.0-SNAPSHOT.jar de.tudarmstadt.aiphes.c4corpus.hadoop.statistics.vocabulary.WARCWordDistribution -Dmapreduce.job.queuename=shortrunning /user/habernal/lrec2015-commoncrawl-subset-phase1/Lic_publicdomain_Lang_en*,/user/habernal/lrec2015-commoncrawl-subset-phase1/Lic_cc-unspecified_Lang_en*,/user/habernal/lrec2015-commoncrawl-subset-phase1/Lic_by*_Lang_en* /user/habernal/lrec2015-commoncrawl-subset-phase1-vocabulary

-- common crawl none
hadoop jar c4corpus-tools-1.0-SNAPSHOT.jar de.tudarmstadt.aiphes.c4corpus.hadoop.statistics.vocabulary.WARCWordDistribution -Dmapreduce.job.queuename=shortrunning /user/habernal/lrec2015-commoncrawl-subset-phase1/Lic_none_Lang_en* /user/habernal/lrec2015-commoncrawl-subset-phase1-vocabulary-none

```

2. Sort vocabularies using Pig

`$ pig`

```PigLatin
a = LOAD '/user/habernal/lrec2015-enwiki-vocabulary/*' AS (word:chararray, counts:int);
b = order a by counts desc;
c = filter b by counts > 4;
store c into '/user/habernal/lrec2015-enwiki-vocabulary-sorted/' using PigStorage();
```

TODO: consider creating a pig script (http://sudarmuthu.com/blog/passing-command-line-arguments-to-pig-scripts/)

3. Get data to your local filesystem

```
hadoop fs -getmerge /user/habernal/lrec2015-brown-vocabulary-sorted/* brown-vocabulary-sorted.txt
```

4. Compare two corpora using `de.tudarmstadt.aiphes.c4corpus.hadoop.statistics.vocabulary.TopNWordsCorrelation`

* parameters `brown-vocabulary-sorted.txt another-corpus.txt topNWords`

## Collect vocabulary distribution from Wikipedia


1. Download Wikipedia dump

```
wget http://download.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2
```

(Note: torrents are much faster)

2. Run WikiExtractor.py to extract plain text (http://medialab.di.unipi.it/wiki/Wikipedia_Extractor)

```
./WikiExtractor.py -c -o extracted
```

3. Upload Wikipedia to HDFS


habernal@node-00b:~/wikipedia/extracted-merged$ for file in * ; do echo $file ; filename=$(basename "$file") ; cat $file | hadoop fs -put - "/user/habernal/enwiki/$filename" ; done


```
~/wikipedia/extracted$ for prefix in * ; do for file in $prefix/* ; do echo $prefix ; echo $file; \
 filename=$(basename "$file") ; echo $filename; head -1 $file; \
  cat $file | hadoop fs -put - "/user/habernal/enwiki/$prefix_$filename.txt" ; done; done
```


## NOT RELATED TO THE PROJECT ANYMORE:
##Remove duplicates in candidates for deletion
```PigLatin
//to increase memory
grunt> SET mapreduce.map.memory.mb 5120
grunt> SET mapreduce.reduce.memory.mb 5120
grunt> SET mapreduce.map.java.opts -Xmx4280m;
grunt> SET mapreduce.reduce.java.opts -Xmx4096m;

grunt> data = load '/user/habernal/lrec2015-ccweb-phase2/*' as (s:chararray);
grunt> d = distinct data;
grunt> store d into '/user/habernal/lrec2015-ccweb-phase2-uniq' using PigStorage();
```
fails on heap space [fixed]