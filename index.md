---
#
# Use the widgets beneath and the content will be
# inserted automagically in the webpage. To make
# this work, you have to use › layout: frontpage
#
layout: frontpage
title: "DKPro C4Corpus"
#header:
#	title: DKPro C4Corpus
#   image_fullwidth: "header_unsplash_12.jpg"
#header-1:
#    title: A collection of software components for natural language processing (NLP) based on the Apache UIMA framework.
#    text: Many NLP tools are already freely available in the NLP research community. DKPro Core provides Apache UIMA components wrapping these tools (and some original tools) so they can be used interchangeably in UIMA processing pipelines. DKPro Core builds heavily on uimaFIT which allows for rapid and easy development of NLP processing pipelines, for wrapping existing tools and for creating original UIMA components.
---

DKPro C4CorpusTools is a collection of tools for processing CommonCrawl corpus, including Creative Commons license detection, boilerplate removal, language detection, and near-duplicate removal.

* **DKPro C4CorpusTools** (or C4CorpusTools) refers to the project source codes
* **C4Corpus** refers the preprocessed CommonCrawl data set (**C4** = **C**reative **C**ommons from **C**ommon **C**rawl)

Consult the official C4CorpusTools documentation which contains

* C4Corpus Users's Guide
  * How to access C4Corpus at S3
  * Running boilerplate removal outside Hadoop
  * Examples of simple search in C4Corpus
* C4Corpus Developers's Guide
  * How to run the full processing pipeline on CommonCrawl
* Corpus statistics reported in the LREC article

How to cite
-----------

Please cite DKPro C4CorpusTools itself as:

> Habernal, I., Zayed, O. and Gurevych, I. (2016). **C4Corpus: Multilingual Web-size corpus with free license**. In Proceedings of the 10th International Conference on Language Resources and Evaluation (LREC 2016), to be published, Portoroz, Slovenia.
[(pdf)][LREC_2016_pdf] [(bib)][LREC_2016_bib]

License
-------

This project is licensed under the  [Apache Software License (ASL) version 2][ASL] - but its dependencies may not be.

About DKPro C4CorpusTools
-------------------------

* Contact person: Ivan Habernal, habernal@ukp.informatik.tu-darmstadt.de
* UKP Lab: http://www.ukp.tu-darmstadt.de/
* TU Darmstadt: http://www.tu-darmstadt.de/

[LREC_2016_pdf]: https://www.ukp.tu-darmstadt.de/fileadmin/user_upload/Group_UKP/publikationen/2016/lrec2016-c4corpus-camera-ready.pdf
[LREC_2016_bib]: https://www.ukp.tu-darmstadt.de/publications/details/?no_cache=1&tx_bibtex_pi1%5Bpub_id%5D=TUD-CS-2016-0023&type=99&tx_bibtex_pi1%5Bbibtex%5D=yes
[ASL]: http://www.apache.org/licenses/LICENSE-2.0
