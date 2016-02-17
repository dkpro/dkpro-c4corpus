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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.vocabulary;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;

import java.io.IOException;

/**
 * @author Ivan Habernal
 */
public abstract class WordDistributionStatisticsCollector
        extends Configured
        implements Tool
{
    abstract Class<? extends InputFormat> getInputFormatClass();

    abstract Class<? extends Mapper> getMapperClass();

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        job.setJarByClass(WordDistributionStatisticsCollector.class);
        job.setJobName(WordDistributionStatisticsCollector.class.getName());

        // mapper
        job.setMapperClass(getMapperClass());
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // reducer
        job.setReducerClass(SumReducer.class);
        job.setInputFormatClass(getInputFormatClass());
        job.setOutputFormatClass(TextOutputFormat.class);

        // paths
        String commaSeparatedInputFiles = args[0];
        String outputPath = args[1];

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static final String REPLACED_CHARACTERS = "[_|$#<>\\^=\\[\\]\\*/\\\\,;,.\\-:()?!\"`'{}]";

    public static class SumReducer
            extends Reducer<Text, IntWritable, Text, IntWritable>
    {
        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException
        {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            context.write(new Text(key), new IntWritable(sum));
        }
    }
}