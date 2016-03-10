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