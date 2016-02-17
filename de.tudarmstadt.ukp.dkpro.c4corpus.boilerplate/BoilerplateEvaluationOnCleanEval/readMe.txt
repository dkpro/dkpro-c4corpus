*To get the results of JusText (Pomikálek 2011)
python cleaneval.py -s -a -u  JusText_Python_Defaults_CleanEvalHTMLTestSubset CleanEvalGoldStandard 

*To get the results of our java reimplementation of JusText
python cleaneval.py -s -a -u  JusText_Java_Defaults_CleanEvalHTMLTestSubset CleanEvalGoldStandard 

Notes:
------
The pages in "JusText_Python_Defaults_CleanEvalHTMLTestSubset" are obtained by running the python script of JusText on the 681 files of the cleaneval dataset
The pages in "JusText_Java_Defaults_CleanEvalHTMLTestSubset" are obtained by running our java implementation on the same 681 file of the cleaneval dataset
