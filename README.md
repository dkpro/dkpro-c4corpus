# DKPro C4CorpusTools

**NOTE: work in progress until 1.0.0 release**

DKPro C4CorpusTools is a collection of tools for processing CommonCrawl corpus, including Creative
Commons license detection, boilerplate removal, language detection, and near-duplicate removal. 

* **DKPro C4CorpusTools** (or C4CorpusTools) refers to the project source codes
* **C4Corpus** refers the preprocessed CommonCrawl data set (**C4** =
 **C**reative **C**ommons from **C**ommon **C**rawl)

Please use the following citation if you use C4Corpus or C4CorpusTools

```
@InProceedings{Habernal.et.al.2016.LREC,
  author    = {Habernal, Ivan and Zayed, Omnia, and Gurevych, Iryna},
  title     = {{C4Corpus: Multilingual Web-size corpus with free license}},
  booktitle = {Proceedings of the 10th International Conference on Language Resources
               and Evaluation (LREC 2016)},
  month     = {May},
  year      = {2016},
  address   = {Portoro\v{z}, Slovenia},
  publisher = {European Language Resources Association (ELRA)},
  pages     = {(to appear)},
  url       = {TBA}
}
```

> **Abstract:** TODO add from paper 


**Contact person:** Ivan Habernal, habernal@ukp.informatik.tu-darmstadt.de

UKP Lab: http://www.ukp.tu-darmstadt.de/ &mdash; TU Darmstadt: http://www.tu-darmstadt.de/



* For bug reporting please use the GitHub bug tracker
* Don't hesitate to send me an e-mail if you have general questions about C4Corpus and C4CorpusTools
* For questions regarding CommonCrawl or Amazon Web Services, use the appropriate channels
    * [CommonCrawl forum](https://groups.google.com/forum/#!forum/common-crawl)
    * [AWS Documentation](http://docs.aws.amazon.com)


The rest of this README contains
* C4Corpus Users's Guide
    * How to access C4Corpus at S3
    * Running boilerplate removal outside Hadoop
* C4Corpus Developers's Guide
* Corpus statistics reported in the LREC article


## C4Corpus User's Guide

Follow these instructions if you don't plan to run the whole processing by yourself but only want to access the final C4Corpus.
Permission to access the C4Corpus is limited only to Amazon Web Services (AWS) accounts.
If you don't have an AWS account, you can create one using Free Tier (free for a year),
see http://aws.amazon.com/ .
This should be sufficient for accessing the C4Corpus as well as for simple processing.
AWS Free Tier does not apply for AWS Elastic Map Reduce.
We strongly recommend to first consult the [official AWS Documentation](http://docs.aws.amazon.com/) if any of the steps below are not clear.


### How to access C4Corpus at S3

The following steps were tested on a clean AWS Free Tier account.

#### 1. Launch your instance at Amazon EC2

* Pick _"US East (N. Virginia)"_ region (``us-east-1``), CommonCrawl and C4Corpus are located here
* You can run Free Tier server, the following was tested with Ubuntu 14.04 LTS as FreeTier (``t2.micro``)
    * Read Amazon EC2 Documentation for details about instance launching, access, etc.
    * Remember that you just launched a publicly accessible server, so make sure it's up-to-date and secure;
    again, refer to AWS documentation
* Connect to your machine using SSH (you have to use your private ```*.pem``` key which you can only download when launching the instance)

#### 2. Install required software for command-line access to AWS (including S3)

```
ubuntu@ip-172-31-50-XX:~$ sudo apt-get install awscli
```

#### 3. Configure AWS access

* You need two keys: _AWS Access Key ID_ and _AWS Secret Access Key_:
    * You'll find it under Security Credential settings for your AWS Account at https://console.aws.amazon.com/iam/home?region=us-east-1#security_credential
    under _"Access Keys (Access Key ID and Secret Access Key)"_. Details are provided in the
    [documentation](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSGettingStartedGuide/AWSCredentials.html)
* Create a new access key and **keep it safe**, because you cannot retrieve the Secret Key later again

```
ubuntu@ip-172-31-50-XX:~$ aws configure
AWS Access Key ID [None]: AKIAIHV............
AWS Secret Access Key [None]: 84uPPc...................................
Default region name [None]: us-east-1
Default output format [None]:
```

#### 4. Test accessing the CommonCrawl

```
ubuntu@ip-172-31-50-XX:~$ aws s3 ls s3://aws-publicdatasets/common-crawl/crawl-data/CC-MAIN-2015-18/
                           PRE segments/
2015-05-27 01:37:59       1366 segment.paths.gz
2015-05-27 01:37:59     121379 warc.paths.gz
2015-05-27 01:38:00     120377 wat.paths.gz
2015-05-27 01:38:00     120377 wet.paths.gz
```

#### 5. Test accessing C4Corpus

This is the complete folder with all steps performed by preprocessing

```
ubuntu@ip-172-31-50-XX:~$ aws s3 ls s3://ukp-research-data/c4corpus/
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
ubuntu@ip-172-31-50-XX:~$ aws s3 ls s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/ | head
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
ubuntu@ip-172-31-50-XX:~$ aws s3 cp s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/Lic_by-nc-nd_Lang_cs_NoBoilerplate_true-r-00130.seg-00000.warc.gz .
download: s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/Lic_by-nc-nd_Lang_cs_NoBoilerplate_true-r-00130.seg-00000.warc.gz to ./Lic_by-nc-nd_Lang_cs_NoBoilerplate_true-r-00130.seg-00000.warc.gz
ubuntu@ip-172-31-50-XX:~$ ls -htr | tail -1
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
ubuntu@ip-172-31-50-XX:~$ aws s3 ls s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/Lic_by-nc_*.warc.gz
ubuntu@ip-172-31-50-XX:~$ 
```

* You have to grep the output from ```aws s3 ls`` to get a list of files with a certain language or license, for example

```
ubuntu@ip-172-31-50-XX:~$ aws s3 ls s3://ukp-research-data/c4corpus/cc-phase1out-2015-11/ \
| grep "Lic_by-nc-nd_Lang_en"
2016-02-02 13:10:41 1000039131 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00000.warc.gz
2016-02-02 13:10:52 1000026370 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00001.warc.gz
2016-02-02 13:11:11 1000035397 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00002.warc.gz
2016-02-02 13:11:32 1000040643 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00003.warc.gz
2016-02-02 13:11:53 1000019635 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00004.warc.gz
2016-02-02 13:12:12  435304263 Lic_by-nc-nd_Lang_en_NoBoilerplate_true-r-00284.seg-00005.warc.gz
```

##### Downloading the free part of C4Corpus

This will print all file names with CC, public domain or cc-unspecified licenses

```
ubuntu@ip-172-31-50-XX:~$ for i in `aws s3 ls s3://ukp-research-data/c4corpus/cc-final-2015-11/ | \
 awk '{print $4}' | grep -E 'Lic_by*|Lic_public*|Lic_cc*' ` ; do echo $i; done
```

Now copy all these files to the local dir

```
ubuntu@ip-172-31-50-XX:~$ for i in `aws s3 ls s3://ukp-research-data/c4corpus/cc-final-2015-11/ | awk '{print $4}' | grep -E 'Lic_by*|Lic_public*|Lic_cc*' ` ; do aws s3 cp s3://ukp-research-data/c4corpus/cc-final-2015-11/${i} c4corpus-2015-11/ ; done
download: s3://ukp-research-data/c4corpus/cc-final-2015-11/Lic_by-nc-nd_Lang_af_NoBoilerplate_true-r-00023.seg-00000.warc.gz to c4corpus-2015-11/Lic_by-nc-nd_Lang_af_NoBoilerplate_true-r-00023.seg-00000.warc.gz
download: s3://ukp-research-data/c4corpus/cc-final-2015-11/Lic_by-nc-nd_Lang_ar_NoBoilerplate_true-r-00035.seg-00000.warc.gz to c4corpus-2015-11/Lic_by-nc-nd_Lang_ar_NoBoilerplate_true-r-00035.seg-00000.warc.gz
download: s3://ukp-research-data/c4corpus/cc-final-2015-11/Lic_by-nc-nd_Lang_bg_NoBoilerplate_true-r-00055.seg-00000.warc.gz to c4corpus-2015-11/Lic_by-nc-nd_Lang_bg_NoBoilerplate_true-r-00055.seg-00000.warc.gz
[...]
```

Be aware of transfer costs in AWS!


### Running boilerplate removal outside Hadoop

You can remove boilerplate from HTML pages locally.

* Package the module ```dkpro-c4corpus-boilerplate```

    ```
    $ cd dkpro-c4corpus-boilerplate/
    $ mvn package
    ```
* Test some example page from BBC
    ```
    $ wget http://www.bbc.com/news/election-us-2016-35694116 -O /tmp/input.html -o /dev/null
    $ head /tmp/input.html 
    <!DOCTYPE html>
    <html lang="en" id="responsive-news" prefix="og: http://ogp.me/ns#">
    <head >
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
        <title>US election 2016: Super Tuesday to test candidates - BBC News</title>
        <meta name="description" content="Candidates bidding for their party's ticket in the November US presidential election face their biggest test yet in the so-called Super Tuesday primaries.">
    
        <link rel="dns-prefetch" href="https://ssl.bbc.co.uk/">
        <link rel="dns-prefetch" href="http://sa.bbc.co.uk/">
    ```
* There are two options for boilerplate removal
    * Keep only plain text  
    
    ```
    $ java -jar target/dkpro-c4corpus-boilerplate-1.0.0.jar /tmp/input.html /tmp/output-plain.html false
    $ head /tmp/output-plain.html 
    Senator Ted Cruz cannot afford to lose to Mr Trump in Texas, Mr Cruz's home state, while a reverse for Mr Trump in Massachusetts, with its moderate voters, could break the property tycoon's nationwide momentum.
    Mrs Clinton is hoping to build on her weekend victory in South Carolina, where she polled heavily among African-Americans, to restore her political fortunes after a bruising defeat in New Hampshire to Bernie Sanders, her self-styled democratic socialist rival.
    On 8 November, America is due to elect a successor to Barack Obama, a Democratic president standing down after two terms in office which have seen the Republicans take control of both houses of Congress.
    Opinion polls give Mr Trump a lead in almost all of the 11 states holding Republican contests on Tuesday: Alabama, Arkansas, Georgia, Massachusetts, Oklahoma, Tennessee, Texas, Vermont, Virginia, Alaska and Minnesota.
    The colourful campaign of the billionaire, who won three of the four early voting states, has divided Republicans.
    He said he was "frustrated and saddened" and would look for a third option if Mr Trump won the Republican nomination.
    Marco Rubio, the third-placed Republican contender after Mr Trump and Mr Cruz, is hoping to stay competitive, gambling on a win in his home state of Florida on 15 March.
    Image copyright Reuters
    Image caption Donald Trump autographs the back of a supporter's hand in Valdosta, Georgia, on Monday
    Image copyright AP
    ```
    * Keep also a minimal HTML tags for paragraphs, spans, headers, etc.
    
    ```
    $ java -jar target/dkpro-c4corpus-boilerplate-1.0.0.jar /tmp/input.html /tmp/output-minimal.html true
    $ head /tmp/output-minimal.html 
    <p>Senator Ted Cruz cannot afford to lose to Mr Trump in Texas, Mr Cruz's home state, while a reverse for Mr Trump in Massachusetts, with its moderate voters, could break the property tycoon's nationwide momentum.</p>
    <p>Mrs Clinton is hoping to build on her weekend victory in South Carolina, where she polled heavily among African-Americans, to restore her political fortunes after a bruising defeat in New Hampshire to Bernie Sanders, her self-styled democratic socialist rival.</p>
    <p>On 8 November, America is due to elect a successor to Barack Obama, a Democratic president standing down after two terms in office which have seen the Republicans take control of both houses of Congress.</p>
    <p>Opinion polls give Mr Trump a lead in almost all of the 11 states holding Republican contests on Tuesday: Alabama, Arkansas, Georgia, Massachusetts, Oklahoma, Tennessee, Texas, Vermont, Virginia, Alaska and Minnesota.</p>
    <p>The colourful campaign of the billionaire, who won three of the four early voting states, has divided Republicans.</p>
    <p>He said he was "frustrated and saddened" and would look for a third option if Mr Trump won the Republican nomination.</p>
    <p>Marco Rubio, the third-placed Republican contender after Mr Trump and Mr Cruz, is hoping to stay competitive, gambling on a win in his home state of Florida on 15 March.</p>
    <p>Image copyright Reuters</p>
    <span>Image caption Donald Trump autographs the back of a supporter's hand in Valdosta, Georgia, on Monday</span>
    <p>Image copyright AP</p>
    ```

### List of URLs from CommonCrawl

* All URLs can be extracted using ``de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.uriextractor.URIExtractor``

* Extracted URLs from 2016-07 crawl are available at our public S3 bucket, size 25.55 GB:

```
$ aws s3 ls s3://ukp-research-data/c4corpus/common-crawl-full-url-list-2016-07/
[...]
$ aws s3 ls s3://ukp-research-data/c4corpus/common-crawl-full-url-list-2016-07/ | \ 
awk '{ sum += $3 }; END { print sum } '
```

## C4CorpusTools Developer's Guide

* Java 1.6 is required for compiling and running the project
* For running the Hadoop pipeline, Hadoop 2.6 is recommended
    * Running the pipeline on CommonCrawl located at S3 requires and active Amazon Web Services (AWS) account 

### Project structure

* ``dkpro-c4corpus-boilerplate`` contains a Java implementation of a state-of-the-art boilerplate removal (JusText, Pomikalek, 2011) 
* ``dkpro-c4corpus-deduplication`` implements near-duplicate content detection based on SimHash
* ``dkpro-c4corpus-hadoop`` contains several Hadoop Map/Reduce jobs for running the pipeline on the CommonCrawl corpus
* ``dkpro-c4corpus-language`` provides language and encoding detection functionality
* ``dkpro-c4corpus-license`` implements Creative Commons license detection in html pages
* ``dkpro-c4corpus-warc-io`` contains I/O tools for reading WARC file format 

Except ``dkpro-c4corpus-hadoop`` the modules are independent of each other and can run locally without Hadoop.



### Run the whole pipeline on CommonCrawl corpus 

```
$ mvn package
```

It will produce a fat jar ``dkpro-c4corpus-hadoop-1.0.0.jar`` in ``dkpro-c4corpus-hadoop/target/``.

Upload the ``dkpro-c4corpus-hadoop-1.0.0.jar`` file into your S3 bucket.

#### Phase 1: License detection, language detection, and boilerplate removal

If you are confident with AWS Elastic Map Reduce command line, you can use the following script
(slightly modified version of what we used)

```
aws emr create-cluster \
    --applications Name=Hadoop \
    --ec2-attributes \
        '{"KeyName":"your-keypair-name", \                                         <---- change this
        "InstanceProfile":"EMR_EC2_DefaultRole", \
        "SubnetId":"subnet-xxxxx", \                                               <---- change this
        "EmrManagedSlaveSecurityGroup":"sg-xxxxxx", \                              <---- change this
        "EmrManagedMasterSecurityGroup":"sg-xxxxxx"}' \                            <---- change this
    --service-role EMR_DefaultRole \
    --enable-debugging \
    --release-label emr-4.2.0 \
    --log-uri 's3n://your-logs/elasticmapreduce/' \                                <---- change this
    --steps '[\
        {"Args":["de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase1FullJob", \
        "-D","mapreduce.task.timeout=7200000", \
        "-D", "mapreduce.map.failures.maxpercent=5", \
        "-D", "mapreduce.map.maxattempts=2", \
        "-D", "c4corpus.keepminimalhtml=true", \                      <---- change this (optionally)
        "s3://aws-publicdatasets/common-crawl/crawl-data/CC-MAIN-2015-27/segments/*/warc/*.warc.gz",\
        "s3://ukp-research-data/c4corpus/cc-phase1out-2015-11"], \                 <---- change this
        "Type":"CUSTOM_JAR", \
        "ActionOnFailure":"CANCEL_AND_WAIT", \
        "Jar":"s3://path-to-your/dkpro-c4corpus-hadoop-1.0.0.jar", \               <---- change this
        "Properties":"", \
        "Name":"Custom JAR"}]' \
    --name 'Full cluster phase 1' \
    --instance-groups '[\
        {"InstanceCount":32, \                                        <---- change this (optionally)
            "bid-price":"your-bid-value", \                                        <---- change this
            "InstanceGroupType":"TASK",\
            "InstanceType":"c3.8xlarge", \
            "Name":"c3.8xlarge = 32 CPU"}, \
        {"InstanceCount":2, \
            "InstanceGroupType":"CORE",\
            "InstanceType":"m3.xlarge", \
            "Name":"Core instance group - 2"}, \
        {"InstanceCount":1, \
            "InstanceGroupType":"MASTER", \
            "InstanceType":"m3.xlarge", \
            "Name":"Master instance group - 1"}]' \
    --auto-terminate \
    --region us-east-1
```

* Enter your correct ``EmrManagedSlaveSecurityGroup`` and ``SubnetId``
* Path to logs and packed ``dkpro-c4corpus-hadoop-1.0.0.jar``, output path
* ``bid-price`` if you want to use Spot instances (highly recommended, but might get unstable)
    * If Spot instances died (were over-bidden), the entire job went unstable and failed,
    I recommend to put your bid higher then usual to make sure you won't loose instances
* Parameter ``c4corpus.keepminimalhtml`` is optional. If set to ``true``, the minimal HTML tags
for each paragraph will be kept (see the example for boilerplate removal above)

* Using 32 c3.8xlarge spot instances (each 32 CPUs, thus 1024 CPUs in total), the job finished
in 22 hours (47,656 Normalized instance hours)

You can also configure the EMR cluster in the Web Console; then you only need to provide manually the
job parameters, namely path to your  ``dkpro-c4corpus-hadoop-1.0.0.jar`` with the following parameters

```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase1FullJob \
-D mapreduce.task.timeout=36000000 -D mapreduce.map.failures.maxpercent=5 \
-D mapreduce.map.maxattempts=2 \
s3://aws-publicdatasets/common-crawl/crawl-data/CC-MAIN-2015-27/segments/*/warc/*.warc.gz \
s3://your-bucket/output-path/cc-phase1out-2015-11
```

Consult [AWS EMR Documentation](http://docs.aws.amazon.com/cli/latest/reference/emr/create-cluster.html) for details.


```
Elapsed: 	10hrs, 30mins, 54sec
Diagnostics: 	
Average Map Time 	21mins, 37sec
Average Shuffle Time 	4hrs, 2mins, 32sec
Average Merge Time 	2sec
Average Reduce Time 	6mins, 11sec 

Job Counters 
		Killed map tasks=1
		Killed reduce tasks=9
		Launched map tasks=34901
		Launched reduce tasks=1038
		Data-local map tasks=34901
		Total time spent by all maps in occupied slots (ms)=2355616565952
		Total time spent by all reduces in occupied slots (ms)=1597882218400
		Total time spent by all map tasks (ms)=45300318576
		Total time spent by all reduce tasks (ms)=15364252100
		Total vcore-seconds taken by all map tasks=45300318576
		Total vcore-seconds taken by all reduce tasks=15364252100
		Total megabyte-seconds taken by all map tasks=75379730110464
		Total megabyte-seconds taken by all reduce tasks=51132230988800
	Map-Reduce Framework
		Map input records=5218270609
		Map output records=1106772814
		Map output bytes=5486829270291
		Map output materialized bytes=2915042688228
		Input split bytes=7953710
		Combine input records=0
		Combine output records=0
		Reduce input groups=1999
		Reduce shuffle bytes=2915042688228
		Reduce input records=1106772814
		Reduce output records=1106772814
		Spilled Records=2219542021
		Shuffled Maps =35912100
		Failed Shuffles=0
		Merged Map outputs=35912100
		GC time elapsed (ms)=609065604
		CPU time spent (ms)=49256484730
		Physical memory (bytes) snapshot=43846440337408
		Virtual memory (bytes) snapshot=77435973947392
		Total committed heap usage (bytes)=43111809548288
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	File Input Format Counters 
		Bytes Read=31414560882228
	File Output Format Counters 
		Bytes Written=1793068433099

```



#### Phase 2: Exact match de-duplication

Similarly as in the previous step, but with different parameters

```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase2ExactMatchDeDuplication \
-D mapreduce.task.timeout=36000000 \
s3://your-bucket/output-path/cc-phase1out-2015-11/*.warc.gz \
s3://your-bucket/output-path/cc-phase2out-2015-11/
```

Took 22 minutes with 4 + 16 c3.8xlarge instances. 

```
Job Counters 
		Launched map tasks=2088
		Launched reduce tasks=522
		Data-local map tasks=2088
		Total time spent by all maps in occupied slots (ms)=35480702088
		Total time spent by all reduces in occupied slots (ms)=74178655824
		Total time spent by all map tasks (ms)=682321194
		Total time spent by all reduce tasks (ms)=713256306
		Total vcore-seconds taken by all map tasks=682321194
		Total vcore-seconds taken by all reduce tasks=713256306
		Total megabyte-seconds taken by all map tasks=1135382466816
		Total megabyte-seconds taken by all reduce tasks=2373716986368
	Map-Reduce Framework
		Map input records=1105484605
		Map output records=1105484605
		Map output bytes=5508868950574
		Map output materialized bytes=2338339670842
		Input split bytes=311112
		Combine input records=0
		Combine output records=0
		Reduce input groups=427404794
		Reduce shuffle bytes=2338339670842
		Reduce input records=1105484605
		Reduce output records=427404794
		Spilled Records=3316291949
		Shuffled Maps =1089936
		Failed Shuffles=2990
		Merged Map outputs=1089936
		GC time elapsed (ms)=4112696
		CPU time spent (ms)=484494420
		Physical memory (bytes) snapshot=2619546624000
		Virtual memory (bytes) snapshot=6272763899904
		Total committed heap usage (bytes)=2961300258816
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=653
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	File Input Format Counters 
		Bytes Read=1793068399499
	File Output Format Counters 
		Bytes Written=907193887387
```


#### Phase 3: Detecting near duplicates

##### Step 1: Extract near duplicates info
```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase3Step1ExtractNearDupInfo \
s3://your-bucket/output-path/cc-phase2out-2015-11/*.warc.gz \
s3://your-bucket/output-path/cc-phase3step1out-2015-11/
```

56 minutes, 1152 normalized instance hours


##### Step 2: Distinct data

```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase3Step2DistinctDataJob \
s3://your-bucket/output-path/cc-phase3step1out-2015-11/*.txt \
s3://your-bucket/output-path/cc-phase3step2out-2015-11/
```

45 minutes, 384 normalized instance hours

##### Step 3: Tuples creation

```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase3Step3NearDupTuplesCreation \
-D mapreduce.task.timeout=0 \
s3://your-bucket/output-path/cc-phase3step2out-2015-11/* \
s3://your-bucket/output-path/cc-phase3step3out-2015-11/
```

* The timeout should be disabled as while calculating the Hamming distance,
the mapper neither reads an input, writes an output, nor updates its status string 
so it will fail after the default 3 hours.


1 day, 12 hours, 7104 normalized instance hours

##### Step 4: Greedy clustering


```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase3Step4GreedyClustering \
-D mapreduce.task.timeout=0 \
s3://your-bucket/output-path/cc-phase3step3out-2015-11/* \
s3://your-bucket/output-path/cc-phase3step4out-2015-11/
```

#### Phase 4: Removing near duplicates

```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase4RemoveDuplicatesUsingReduceSideJoins \
s3://your-bucket/output-path/cc-phase3step4out-2015-11/ \
s3://your-bucket/output-path/cc-phase2out-2015-11/*.warc.gz \
s3://your-bucket/output-path/cc-phase4out-2015-11/
```

#### Phase 5: Sorting final corpus by language and license

```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase5MergeByLangLicJob \
s3://your-bucket/output-path/cc-phase4out-2015-11/*.warc.gz \
s3://your-bucket/output-path/cc-final-2015-11/
```

### Including C4CorpusTools in your Java projects

C4CorpusTools is hosted on Maven Central, you can add the following dependencies into your ``pom.xml``
(see descriptions above)

```
<dependency>
  <groupId>org.dkpro.c4corpus</groupId>
  <artifactId>dkpro-c4corpus-boilerplate</artifactId>
  <version>1.0.0</version>
</dependency>
```

and analogically

* ``<artifactId>dkpro-c4corpus-license</artifactId>``
* ``<artifactId>dkpro-c4corpus-deduplication</artifactId>``
* ``<artifactId>dkpro-c4corpus-language</artifactId>``
* ``<artifactId>dkpro-c4corpus-hadoop</artifactId>``

## Working with C4Corpus - Word count example

Although you can download the C4Corpus to your computer and process it locally, it is probably
worth running it on an AWS EMR cluster (good scalability).

See ``de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.examples.WordCountExample`` under ``dkpro-c4corpus-hadoop`` 
which is an adaptation of the famous word counting example present in every Hadoop tutorial.

You should run it on the processed C4Corpus; here we want to count words in all German data.

* Spin an EMR cluster. It doesn't have to be big, I tested this example with 2 nodes
    * Tested with ``emr-4.2.0`` distribution but it should work with newer ones as well
    * Also add ``Pig 0.14.0`` if you want to analyze the output
* Run this step, change your output bucket accordingly

```
hadoop jar s3://your-bucket/dkpro-c4corpus-hadoop-1.0.0.jar \
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.examples.WordCounterExample \
s3://ukp-research-data/c4corpus/cc-final-2015-11/*Lang_de*.warc.gz \
s3://ukp-research-data/c4corpus/statistics/examples-word-count-de-2015-11/
```

This will produce several plain text files with words and its counts. The output is pretty big (320 MB)
because of many "words" with a single occurrence.
 
Let's explore that deeper using Pig. Login to your headnode, i.e.,

``
ssh -i your-keypair.pem hadoop@ec2-54-85-129-184.compute-1.amazonaws.com
``

and run Pig

```
[hadoop@ip-172-31-9-118 ~]$ pig
[...]
grunt> words = load 's3://ukp-research-data/c4corpus/statistics/examples-word-count-de-2015-11/' 
 as (word:chararray, counts:int);
grunt> sorted = order words by counts desc;
grunt> top100 = limit sorted 100;
grunt> dump top100;
[...]
(und,43420735)
(der,38801000)
(die,36769583)
(in,24394590)
(r,16453990)
(von,15897453)
(f,15624384)
(den,15533307)
(mit,15096028)
(ist,15001207)
(zu,14588717)
(das,13847411)
(auf,11843696)
(1,11153547)
(nicht,10757865)
(im,10262142)
[...]
```

This will sort the words by their counts (revesed) and prints the top 100 words to the console.
Consult [Pig documentation](http://pig.apache.org/) for further data manipulation.

## Corpus statistics reported in the LREC article

### Token and document counts in the final corpus

Reports in Table 7 were collected using the following M/R job:

```
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.LangLicenseStatistics
```

Run it with the following parameters on EMR cluster:

```
s3://ukp-research-data/c4corpus/cc-final-2015-11/*.warc.gz s3://your-bucket/statistics
```

Then download the results into a local file system and convert it to a CSV table:

```
$ aws s3 cp s3://ukp-research-data/c4corpus/statistics/cc-final-2015-11/ . --recursive
$ gunzip *.gz -c > stats.tsv
$ java -cp dkpro-c4corpus-hadoop-1.0.0.jar \
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.StatisticsTableCreator \
stats.tsv stats-table.csv
```


### Collecting word distribution statistics

* Collect word statistics (CommonCrawl CC)
```
hadoop jar dkpro-c4corpus-hadoop-1.0.0.jar \ 
de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.vocabulary.WARCWordDistribution \
s3://ukp-research-data/c4corpus/statistics/cc-final-2015-11/Lic_publicdomain_Lang_en*,\
s3://ukp-research-data/c4corpus/statistics/cc-final-2015-11/Lic_cc-unspecified_Lang_en*,\
s3://ukp-research-data/c4corpus/statistics/cc-final-2015-11/Lic_by*_Lang_en* \
s3://your-bucket/output-folder1
```

* Sort vocabularies using Pig

`$ pig`

```PigLatin
a = LOAD 's3://your-bucket/output-folder1*' AS (word:chararray, counts:int);
b = order a by counts desc;
c = filter b by counts > 4;
store c into 's3://your-bucket/output-folder2' using PigStorage();
```

* Get data to your local filesystem

```
hadoop fs -getmerge s3://your-bucket/output-folder2/* cc-vocabulary-sorted.txt
```

* Compare two corpora using `de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.vocabulary.TopNWordsCorrelation`
    * parameters `brown-vocabulary-sorted.txt another-corpus.txt topNWords`


## Collect vocabulary distribution from Wikipedia


* Download Wikipedia dump
    * ``wget http://download.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2``
    * (Note: torrents are much faster)
* Run WikiExtractor.py to extract plain text (http://medialab.di.unipi.it/wiki/Wikipedia_Extractor)
    * ``./WikiExtractor.py -c -o extracted``
* Upload Wikipedia to HDFS
```
~/wikipedia/extracted$ for prefix in * ; do for file in $prefix/* ; do echo $prefix ; echo $file; \
filename=$(basename "$file") ; echo $filename; head -1 $file; \
cat $file | hadoop fs -put - "/user/your-folder/enwiki/$prefix_$filename.txt" ; done; done
```
* We ran this step on a local Hadoop cluster; you have to adjust it to work on AWS EMR
