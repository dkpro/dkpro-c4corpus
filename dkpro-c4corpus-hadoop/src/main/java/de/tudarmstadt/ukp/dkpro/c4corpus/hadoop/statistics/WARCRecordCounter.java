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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Arrays;

/**
 * Simple counter; produces the number or records in given WARC files. Intended to test the API
 * on the Hadoop cluster.
 *
 * @author Ivan Habernal
 */
public class WARCRecordCounter
        extends Configured
        implements Tool
{
    @Override public int run(String[] args)
            throws Exception
    {
        Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

        System.out.println("Other args: " + Arrays.toString(otherArgs));

        Job job = Job.getInstance();
        job.setJarByClass(WARCRecordCounter.class);

        job.setJobName(WARCRecordCounter.class.getName());

        // mapper
        job.setMapperClass(ResponseMapper.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // combiner + reducer
        job.setCombinerClass(MyReducer.class);
        job.setReducerClass(MyReducer.class);

        job.setInputFormatClass(WARCInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        // paths
        String commaSeparatedInputFiles = otherArgs[0];
        String outputPath = otherArgs[1];

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new WARCRecordCounter(), args);
    }

    /**
     * Mapper; simply omits one for each entry (under the same key)
     */
    public static class ResponseMapper
            extends Mapper<LongWritable, WARCWritable, Text, IntWritable>
    {
        private static final Text word = new Text("WARC records");

        private final static IntWritable one = new IntWritable(1);

        @Override protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            // just increase the count
            context.write(word, one);
        }
    }

    /**
     * Sums up the counts
     */
    public static class MyReducer
            extends Reducer<Text, IntWritable, Text, IntWritable>
    {
        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException
        {
            int sum = 0;
            for (IntWritable intWritable : values) {
                sum += intWritable.get();
            }

            context.write(key, new IntWritable(sum));
        }
    }

}

