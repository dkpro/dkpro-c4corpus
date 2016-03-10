package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.uriextractor;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;

/**
 * I the entry point for the URI extractor. It looks at all WARC files and saves
 * the TARGET-URI property.
 * 
 * @author Chris Stahlhut
 */
public class URIExtractor extends Configured implements Tool {

	public static void main(String... args) throws Exception {
		ToolRunner.run(new URIExtractor(), args);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int run(String[] args) throws Exception {

		Job job = Job.getInstance(getConf());
		// set from the command line
		job.setJarByClass(URIExtractor.class);
		job.setJobName(URIExtractor.class.getName());

		// mapper
		job.setMapperClass(URIExtractorMapper.class);
		job.setReducerClass(URIExtractorReducer.class);

		// input-output is warc
		job.setInputFormatClass(WARCInputFormat.class);
		// is necessary, so that Hadoop does not mix the map input format up.
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);

        // set output compression to GZip
        FileOutputFormat.setCompressOutput(job, true);
        FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);

		FileInputFormat.addInputPaths(job, args[0]);
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		return job.waitForCompletion(true) ? 0 : 1;
	}

}
