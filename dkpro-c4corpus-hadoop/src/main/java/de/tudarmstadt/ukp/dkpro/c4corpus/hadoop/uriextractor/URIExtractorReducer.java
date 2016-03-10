package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.uriextractor;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Removes all the duplicates from the URIs and stores them as key.
 *
 * @author Chris Stahlhut
 */
class URIExtractorReducer extends Reducer<Text, NullWritable, Text, NullWritable> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reduce(Text key, Iterable<NullWritable> value, Context context)
			throws IOException, InterruptedException {
		context.write(key, NullWritable.get());
	}
}
