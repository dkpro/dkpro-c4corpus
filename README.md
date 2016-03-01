# DKPro C4CorpusTools

NOTE: work in progress until 1.0.0 release

## C4CorpusTools Developer's Guide

* Java 1.6 is required for compiling and running the project
* For running the Hadoop pipeline, Hadoop 2.6 is recommended
    * Running the pipeline on CommonCrawl located at S3 requires and active Amazon Web Services (AWS) account 

### Project structure

* ``dkpro-c4corpus-boilerplate`` contains a Java implementation of a state-of-the-art boilerplate removal (JusText, Pomikalek, 2011) 
* ``dkpro-c4corpus-deduplication`` implements near-duplicate content detection based on SimHash
* ``dkpro-c4corpus-hadoop`` contains several Hadoop Map/Reduce jobs for running the pipeline agains the CommonCrawl corpus
* ``dkpro-c4corpus-language`` provides language and encoding detection functionality
* ``dkpro-c4corpus-license`` implements Creative Commons license detection in html pages

Except ``dkpro-c4corpus-hadoop`` the modules are independent of each other and can run locally without Hadoop.

### Packaging

``$mvn package``

Will produce a _fat_ jar ``dkpro-c4corpus-hadoop-1.0.0.jar`` in ``de.tudarmstadt.ukp.dkpro.c4corpus.hadoop/target/``


### Run the whole pipeline on CommonCrawl corpus 
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

## C4Corpus User's Guide

If you don't plan to run the processing by yourself but only want to access the final C4Corpus.
Permission to access the C4Corpus is limited only to Amazon Web Services (AWS) accounts.
If you don't have an AWS account, you can create one using Free Tier (free for a year),
see http://aws.amazon.com/ .
This should be sufficient for accessing the C4Corpus as well as for simple processing.
AWS Free Tier does not apply for AWS Elastic Map Reduce (Hadoop).
You will find all details for the steps below in the official AWS Documentation (docs.aws.amazon.com/).


### How to access C4Corpus at S3

The following steps were tested on a clean AWS account with Free Tier.

#### 1. Launch your instance at Amazon EC2

* Pick _"US East (N. Virginia)"_ region (``us-east-1```), CommonCrawl and C4Corpus are located here
* You can run FreeTier server, the following was tested with Ubuntu 14.04 LTS as FreeTier (```t2.micro```)
    * Read Amazon EC2 Documentation for details about instance launching, access, etc.
    * Remember that you just launched a publicly accessible server, so make sure it's up-to-date and secure; again, refer to AWS Docs
* Connect to your machine using SSH (you have to use your private ```*.pem``` key which you can only download when launching the instance)

#### 2. Install required software for command-line access to AWS (including S3)

    ubuntu@ip-172-31-50-43:~$ sudo apt-get install awscli

#### 3. Configure AWS access

* You need two keys: _AWS Access Key ID_ and _AWS Secret Access Key_:
    * You'll find it under Security Credential settings for your AWS Account at https://console.aws.amazon.com/iam/home?region=us-east-1#security_credential
    under _"Access Keys (Access Key ID and Secret Access Key)"_
* Create a new access key and **keep it safe**, because you cannot retrieve the Secret Key later again

```
ubuntu@ip-172-31-50-43:~$ aws configure
AWS Access Key ID [None]: AKIAIHV............
AWS Secret Access Key [None]: 84uPPc...................................
Default region name [None]: us-east-1
Default output format [None]:
```

#### 4. Test accessing the CommonCrawl

```
ubuntu@ip-172-31-50-43:~$ aws s3 ls s3://aws-publicdatasets/common-crawl/crawl-data/CC-MAIN-2015-18/
                           PRE segments/
2015-05-27 01:37:59       1366 segment.paths.gz
2015-05-27 01:37:59     121379 warc.paths.gz
2015-05-27 01:38:00     120377 wat.paths.gz
2015-05-27 01:38:00     120377 wet.paths.gz
```

#### 5. Test accessing C4Corpus

This is the complete folder with all steps performed by preprocessing

```
ubuntu@ip-172-31-50-43:~$ aws s3 ls s3://ukp-research-data/c4corpus/
                           PRE cc-phase1out-2015-11/
                           PRE cc-phase2out-2015-11/
                           PRE cc-phase3step1out-2015-11/
                           PRE cc-phase3step2out-2015-11/
                           PRE cc-phase3step3out-2015-11/
                           PRE cc-phase3step4out-2015-11/
                           PRE cc-phase4out-2015-11/
```


Part of the final C4Corpus

FIXME update to the final location

```
ubuntu@ip-172-31-50-43:~$ aws s3 ls s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/ | head
2016-02-02 13:09:48          0 
2016-02-02 13:10:39      47852 Lic_by-nc-nd_Lang_af_NoBoilerplate_true-r-00585.seg-00000.warc.gz
2016-02-02 13:10:39    5345561 Lic_by-nc-nd_Lang_ar_NoBoilerplate_true-r-00179.seg-00000.warc.gz
2016-02-02 13:10:39    4707924 Lic_by-nc-nd_Lang_bg_NoBoilerplate_true-r-00143.seg-00000.warc.gz
2016-02-02 13:10:39     420970 Lic_by-nc-nd_Lang_bn_NoBoilerplate_true-r-00387.seg-00000.warc.gz
2016-02-02 13:10:39    6008314 Lic_by-nc-nd_Lang_cs_NoBoilerplate_true-r-00130.seg-00000.warc.gz
2016-02-02 13:10:39    1471382 Lic_by-nc-nd_Lang_da_NoBoilerplate_true-r-00171.seg-00000.warc.gz
2016-02-02 13:10:39  110294744 Lic_by-nc-nd_Lang_de_NoBoilerplate_true-r-00356.seg-00000.warc.gz
2016-02-02 13:11:05    4446993 Lic_by-nc-nd_Lang_el_NoBoilerplate_true-r-00352.seg-00000.warc.gz
2016-02-02 13:10:41 1000039131 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00000.warc.gz
```


#### 6. Download sample data

FIXME update to the final location

```
ubuntu@ip-172-31-50-43:~$ aws s3 cp s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/Lic_by-nc-nd_Lang_cs_NoBoilerplate_true-r-00130.seg-00000.warc.gz .
download: s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/Lic_by-nc-nd_Lang_cs_NoBoilerplate_true-r-00130.seg-00000.warc.gz to ./Lic_by-nc-nd_Lang_cs_NoBoilerplate_true-r-00130.seg-00000.warc.gz
ubuntu@ip-172-31-50-43:~$ ls -htr | tail -1
Lic_by-nc-nd_Lang_cs_NoBoilerplate_true-r-00130.seg-00000.warc.gz
```

* and that's it! :)

#### 7. Accessing the final output of the C4Corpus Tools preprocessing

The final C4Corpus is located at ```s3://ukp-research-data/c4corpus/cc-final-2015-11/``` with the following file naming

```
Lic_LICENSE_Lang_LANGUAGE_NoBoilerplate_BOOLEAN-r-00284.seg-00000.warc.gz
```

For example

```
Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00000.warc.gz
```

* ```aws s3``` command doesn't support wild characters, so the following command returns an empty output

```
ubuntu@ip-172-31-50-43:~$ aws s3 ls s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/Lic_by-nc_*.warc.gz
ubuntu@ip-172-31-50-43:~$ 
```

* You have to grep the output from ```aws s3 ls`` to get a list of files with a certain language or license, for example

```
ubuntu@ip-172-31-50-43:~$ aws s3 ls s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/ \
| grep "Lic_by-nc-nd_Lang_en"
2016-02-02 13:10:41 1000039131 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00000.warc.gz
2016-02-02 13:10:52 1000026370 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00001.warc.gz
2016-02-02 13:11:11 1000035397 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00002.warc.gz
2016-02-02 13:11:32 1000040643 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00003.warc.gz
2016-02-02 13:11:53 1000019635 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00004.warc.gz
2016-02-02 13:12:12  435304263 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00005.warc.gz
```

















## Clean this

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