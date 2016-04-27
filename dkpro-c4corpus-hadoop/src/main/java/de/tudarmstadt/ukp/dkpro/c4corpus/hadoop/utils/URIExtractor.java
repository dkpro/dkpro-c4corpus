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
package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.utils;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * I the entry point for the URI extractor. It looks at all WARC files and saves
 * the TARGET-URI property.
 *
 * @author Chris Stahlhut
 */
public class URIExtractor
        extends Configured
        implements Tool
{

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new URIExtractor(), args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int run(String[] args)
            throws Exception
    {

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

    /**
     * Extracts the target-URIs from a warc file.
     */
    public static class URIExtractorMapper
            extends Mapper<LongWritable, WARCWritable, Text, NullWritable>
    {
        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws
                IOException, InterruptedException
        {
            WARCRecord record = value.getRecord();
            WARCRecord.Header header = record.getHeader();
            String targetURI = header.getTargetURI();
            if (null != targetURI) {
                context.write(new Text(targetURI), NullWritable.get());
            }
        }
    }

    /**
     * Removes all the duplicates from the URIs and stores them as key.
     */
    public static class URIExtractorReducer
            extends Reducer<Text, NullWritable, Text, NullWritable>
    {
        @Override
        protected void reduce(Text key, Iterable<NullWritable> value, Context context)
                throws IOException, InterruptedException
        {
            context.write(key, NullWritable.get());
        }
    }
}
