#!/usr/bin/env python2.5
# encoding: utf-8
"""
cleaneval.py v1.0

Simple and fast evaluation of CleanEval-1 tasks (precision, recall, F-score).
"""

import sys
import os
import string
import re
from math import *
from difflib import SequenceMatcher
from getopt import getopt, GetoptError

help_message = '''
This is cleaneval.py version 1.0 -- Copyright (C) 2008 by Stefan Evert

Usage:  cleaneval.py [options] <texts_dir> <gold_dir> [<align_dir>]

Options:
  -t    print total precision/recall for all files (micro-averaged)
        (does not print results for individual files)
  -n    omit table header (e.g. to combine multiple tables)
  -s    calculate summary statistics (mean / s.d.) and print on STDERR
  -a    remove non-ASCII characters before comparison
  -u    calculate unlabelled segmentation accuracy

Evaluates automatically cleaned files in directory <texts_dir> against
gold standard files in directory <gold_dir>.  Correspoding files in the 
two directories must have identical names and there must be no other files
in these directories.

The script prints a TAB-delimited evaluation table on standard output, which
can be redirected to a file and read into R, Excel or a similar application.
Precision, recall and F-score are calculated as percentages of whitespace-
delimited words.  Accuracy of segment identification is measured by precision,
recall and F-score for labelled or unlabelled segment marker tags (if the
option -u is specified, no distinction is made between types <p>, <h> and <l>).

If the third argument is given, full alignments will be written to separate
files in directory <align_dir>.
'''

def slurp_file(filename):
	fh = file(filename)
	body = fh.read()
	fh.close()
	return body

re_URL = re.compile("^\s*URL.*$", re.MULTILINE)
re_TAG = re.compile("(<[phl]>)", re.IGNORECASE)
re_WS = re.compile("\s+")
re_CTRL = re.compile("[\x00-\x1F]+")
re_HI = re.compile("[\x80-\xFF]+")

def normalize(text, ascii=False, unlabelled=False):
	text = re_URL.sub("", text)           # remove URL line at start of gold standard files
	text = re_CTRL.sub(" ", text)         # replace any control characters by spaces (includes newlines)

	if unlabelled:
		text = re_TAG.sub("\n<p> ", text) # start each segment on new line, normalise tags
	else:
		text = re_TAG.sub("\n\g<1> ", text)  # only break lines before segment markers

	text = re_WS.sub(" ", text)           # normalise whitespace (including line breaks) to single spaces
	if ascii:
		text = re_HI.sub("", text)        # delete non-ASCII characters (to avoid charset problems)

	return text

## return diff as list of tuples ("equal"/"insert"/"delete", [text words], [gold words])
def make_diff(alignment, text_w, gold_w):
	diff = []
	for tag, i1, i2, j1, j2 in alignment.get_opcodes():
		text_region = text_w[i1:i2]
		gold_region = gold_w[j1:j2]
		if tag == "replace":
			diff.append( ("delete", text_region, []) )
			diff.append( ("insert", [], gold_region) )
		else:
			diff.append( (tag, text_region, gold_region) )
	return diff

## return evaluation measures for given diff:
##   (f-score, precision, recall, [labelled] segmentation f-score, precision, recall)
def evaluate(diff):
	tp = fp = fn = 0
	tag_tp = tag_fp = tag_fn = 0
	for tag, text, gold in diff:
		text_tags = len( filter(re_TAG.match, text) )
		gold_tags = len( filter(re_TAG.match, gold) )
		text_l = len(text)
		gold_l = len(gold)
		if tag == "delete":
			fp += text_l
			tag_fp += text_tags
		elif tag == "insert":
			fn += gold_l
			tag_fn += gold_tags
		else:
			tp += text_l
			tag_tp += text_tags
			assert text_l == gold_l
			assert text_tags == gold_tags

	n_text = tp + fp if tp + fp > 0 else 1
	n_gold = tp + fn if tp + fn > 0 else 1
	precision = float(tp) / n_text
	recall = float(tp) / n_gold
	precision_plus_recall = precision + recall if precision + recall > 0 else 1
	f_score = 2 * precision * recall / precision_plus_recall

	tags_text = tag_tp + tag_fp if tag_tp + tag_fp > 0 else 1
	tags_gold = tag_tp + tag_fn if tag_tp + tag_fn > 0 else 1
	tag_precision = float(tag_tp) / tags_text
	tag_recall = float(tag_tp) / tags_gold
	precision_plus_recall = tag_precision + tag_recall if tag_precision + tag_recall > 0 else 1
	tag_f_score = 2 * tag_precision * tag_recall / precision_plus_recall

	return (100 * f_score, 100 * precision, 100 * recall, 100 * tag_f_score, 100 * tag_precision, 100 * tag_recall, tp, fp, fn, tag_tp, tag_fp, tag_fn)

def write_alignment(diff, filename):
	fh = file(filename, "w")
	for tag, text_seg, gold_seg in diff:
		if tag == "delete":
			print >> fh, "<" * 40, "(false positive)"
			print >> fh, " ".join(text_seg)
		if tag == "insert":
			print >> fh, ">" * 40, "(false negative)"
			print >> fh, " ".join(gold_seg)
		if tag == "equal":
			print >> fh, "=" * 40
			print >> fh, " ".join(gold_seg)
	fh.close()

try:
	options, args = getopt(sys.argv[1:], "tnsau")
except GetoptError:
	print >> sys.stderr, help_message
	sys.exit(2)

if len(args) not in (2,3):
	print >> sys.stderr, help_message
	sys.exit(2)

text_dir = args[0]
gold_dir = args[1]
align_dir = args[2] if len(args) > 2 else None

opt_total = ('-t', '') in options
opt_noheader = ('-n', '') in options
opt_summary = ('-s', '') in options
opt_unlabelled = ('-u', '') in options
opt_ascii = ('-a', '') in options


files = os.listdir(text_dir)  # filenames should be the same in text_dir/ and gold_dir/
n_files = len(files)

sum = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
ss = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

if not opt_noheader:
	print "file\tF\tP\tR\tF.tag\tP.tag\tR.tag\tTP\tFP\tFN\tTP.tag\tFP.tag\tFN.tag"

n_processed = 0
for filename in files:
	text_file = text_dir + "/" + filename
	gold_file = gold_dir + "/" + filename

	if not os.access(gold_file, os.R_OK):
		continue  # original and cleaned files don't always match in CleanEval gold standard

	text = normalize(slurp_file(text_file), opt_ascii, opt_unlabelled)
	gold = normalize(slurp_file(gold_file), opt_ascii, opt_unlabelled)
	text_words = re_WS.split(text)
	gold_words = re_WS.split(gold)

	alignment = SequenceMatcher(None, text_words, gold_words)
	diff = make_diff(alignment, text_words, gold_words)
	eval_list = evaluate(diff)

	if not opt_total:
		print filename + "\t" + ("%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%d\t%d\t%d\t%d\t%d\t%d" % eval_list)

	for i, x in enumerate(eval_list):
		sum[i] += x
		ss[i] += x ** 2

	if align_dir:
		align_file = align_dir + "/" + os.path.splitext(filename)[0] + ".align"
		write_alignment(diff, align_file)
		
	n_processed += 1

if n_processed < n_files:
	print >> sys.stderr, "Warning: %d files not found in gold standard (skipped)" % (n_files - n_processed)

## calculate micro-averaged word-level accuracy / tag accuracy
tp = sum[6]
fp = sum[7]
fn = sum[8]
n_text = tp + fp if tp + fp > 0 else 1
n_gold = tp + fn if tp + fn > 0 else 1
precision = float(tp) / n_text
recall = float(tp) / n_gold
precision_plus_recall = precision + recall if precision + recall > 0 else 1
f_score = 2 * precision * recall / precision_plus_recall

tag_tp = sum[9]
tag_fp = sum[10]
tag_fn = sum[11]
tags_text = tag_tp + tag_fp if tag_tp + tag_fp > 0 else 1
tags_gold = tag_tp + tag_fn if tag_tp + tag_fn > 0 else 1
tag_precision = float(tag_tp) / tags_text
tag_recall = float(tag_tp) / tags_gold
precision_plus_recall = tag_precision + tag_recall if tag_precision + tag_recall > 0 else 1
tag_f_score = 2 * tag_precision * tag_recall / precision_plus_recall

if opt_total:
	eval_list = (100 * f_score, 100 * precision, 100 * recall, 100 * tag_f_score, 100 * tag_precision, 100 * tag_recall, tp, fp, fn, tag_tp, tag_fp, tag_fn)
	print "total" + "\t" + ("%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%d\t%d\t%d\t%d\t%d\t%d" % eval_list)
	
mean = [ x / n_files for x in sum ]
sd = [ sqrt((x - n_files * y ** 2) / (n_files - 1)) for x, y in zip(ss, mean) ]

if opt_summary:
	is_labelled = "unlabelled" if opt_unlabelled else "labelled"
	if n_processed < n_files:
		print >> sys.stderr, "Evaluation report for on %d out of %d files:" % (n_processed, n_files)
	else:
		print >> sys.stderr, "Evaluation report for on %d files:" % (n_files)		
	print >> sys.stderr, "Word-level accuracy (macro-averaged):"
	print >> sys.stderr, "  F-score:   %6.2f%% %s %.2f%%" % (mean[0], '+/-', sd[0])
	print >> sys.stderr, "  Precision: %6.2f%% %s %.2f%%" % (mean[1], '+/-', sd[1])
	print >> sys.stderr, "  Recall:    %6.2f%% %s %.2f%%" % (mean[2], '+/-', sd[2])
	print >> sys.stderr, "Segmentation accuracy (%s):" % (is_labelled)
	print >> sys.stderr, "  F-score:   %6.2f%% %s %.2f%%" % (mean[3], '+/-', sd[3])
	print >> sys.stderr, "  Precision: %6.2f%% %s %.2f%%" % (mean[4], '+/-', sd[4])
	print >> sys.stderr, "  Recall:    %6.2f%% %s %.2f%%" % (mean[5], '+/-', sd[5])
	print >> sys.stderr, "Word-level accuracy (micro-averaged):"
	print >> sys.stderr, "  F-score:   %6.2f%%" % (100 * f_score)
	print >> sys.stderr, "  Precision: %6.2f%%" % (100 * precision)
	print >> sys.stderr, "  Recall:    %6.2f%%" % (100 * recall)
	print >> sys.stderr, "Segmentation accuracy (%s, micro-averaged):" % (is_labelled)
	print >> sys.stderr, "  F-score:   %6.2f%%" % (100 * tag_f_score)
	print >> sys.stderr, "  Precision: %6.2f%%" % (100 * tag_precision)
	print >> sys.stderr, "  Recall:    %6.2f%%" % (100 * tag_recall)
