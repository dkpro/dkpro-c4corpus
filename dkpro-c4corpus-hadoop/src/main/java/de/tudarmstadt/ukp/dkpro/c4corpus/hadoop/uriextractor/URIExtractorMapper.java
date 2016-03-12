/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.uriextractor;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord.Header;

/**
 * Extracts the target-URIs from a warc file.
 * 
 * @author Chris Stahlhut
 */
class URIExtractorMapper extends Mapper<LongWritable, WARCWritable, Text, NullWritable> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void map(LongWritable key, WARCWritable value, Context context) throws IOException, InterruptedException {
		WARCRecord record = value.getRecord();
		Header header = record.getHeader();
		String targetURI = header.getTargetURI();
		if (null != targetURI) {
			context.write(new Text(targetURI), NullWritable.get());
		}
	}
}