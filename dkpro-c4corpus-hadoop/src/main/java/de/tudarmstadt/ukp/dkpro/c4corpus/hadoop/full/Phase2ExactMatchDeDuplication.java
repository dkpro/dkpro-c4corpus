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
package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCOutputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.io.compress.SnappyCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * Detecting exact match duplicates from a dataset. The output is a dataset free
 * of exact matches. Two records are considered exact matches if they have the
 * same length & SimHash.
 *
 * @author Omnia Zayed
 * @author Ivan Habernal
 */
public class Phase2ExactMatchDeDuplication
        extends Configured
        implements Tool
{

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());
        //set from the command line

        job.setJarByClass(Phase2ExactMatchDeDuplication.class);
        job.setJobName(Phase2ExactMatchDeDuplication.class.getName());

        // mapper
        job.setMapperClass(ExactMatchDetectionMapper.class);

        // we will compress the mapper's output (use fast Snappy compressor)
        job.getConfiguration().setBoolean(Job.MAP_OUTPUT_COMPRESS, true);
        job.getConfiguration()
                .setClass(Job.MAP_OUTPUT_COMPRESS_CODEC, SnappyCodec.class, CompressionCodec.class);

        // reducer
        job.setReducerClass(UniqueWarcWriterReducer.class);
        // no combiner, as the output classes in mapper and reducer are different!

        // input-output is warc
        job.setInputFormatClass(WARCInputFormat.class);
        job.setOutputFormatClass(WARCOutputFormat.class);

        // mapper output data
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(WARCWritable.class);

        // set output compression to GZip
        FileOutputFormat.setCompressOutput(job, true);
        FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);

        FileInputFormat.addInputPaths(job, args[0]);
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase2ExactMatchDeDuplication(), args);
    }

    public static class ExactMatchDetectionMapper
            extends Mapper<LongWritable, WARCWritable, Text, WARCWritable>
    {

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {

            String docSimHashString = value.getRecord().getHeader()
                    .getField(WARCRecord.WARCRecordFieldConstants.SIMHASH);

            if (docSimHashString == null) {
                throw new IOException(WARCRecord.WARCRecordFieldConstants.SIMHASH
                        + " metadata not found. Did you run Phase1 on the data?");
            }

            String docLength = String.valueOf(value.getRecord().getHeader().getContentLength());

            Text outputKey = new Text(docLength + "_" + docSimHashString);

            context.write(outputKey, value);
        }
    }

    /**
     * Keeps only the first value as all warc records in the reducer are duplicate
     */
    public static class UniqueWarcWriterReducer
            extends Reducer<Text, WARCWritable, NullWritable, WARCWritable>
    {
        @Override
        protected void reduce(Text key, Iterable<WARCWritable> values, Context context)
                throws IOException, InterruptedException
        {
            context.write(NullWritable.get(), values.iterator().next());
        }
    }
}
