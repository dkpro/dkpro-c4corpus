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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.examples;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.helper.TextLongCountingReducer;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Example how to read processed data with the famous Hadoop word counter
 *
 * @author Ivan Habernal
 */
public class WordCounterExample
        extends Configured
        implements Tool
{
    @Override
    public int run(String[] args)
            throws Exception
    {
        Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

        Job job = Job.getInstance();
        job.setJarByClass(WordCounterExample.class);

        job.setJobName(WordCounterExample.class.getName());

        // mapper
        job.setMapperClass(WordCounterMapper.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        // combiner + reducer
        job.setCombinerClass(TextLongCountingReducer.class);
        job.setReducerClass(TextLongCountingReducer.class);

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
        ToolRunner.run(new WordCounterExample(), args);
    }

    /**
     * Reads "boilerplated" text (can contain "minimal html") emits token counts
     */
    static class WordCounterMapper
            extends Mapper<LongWritable, WARCWritable, Text, LongWritable>
    {
        private static final LongWritable ONE = new LongWritable(1L);

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            // extract paragraphs
            List<String> paragraphs = IOUtils
                    .readLines(new ByteArrayInputStream(value.getRecord().getContent()), "utf-8");

            // for each paragraphs, remove leading and closing html tag (if present)
            for (String paragraph : paragraphs) {
                // remove opening and closing tag if present; no need to check if they match,
                // this is ensured by the boilerplate removal
                String clean = paragraph.replaceAll("^<\\w+>", "").replaceAll("</\\w+>$", "");

                // now we have a plain text for each paragraph; you can do anything you want

                // tokenize; this is indeed a very naive but fast tokenizer
                String[] tokens = clean.split("\\W");

                // and emit each token
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        context.write(new Text(token), ONE);
                    }
                }
            }
        }
    }

}
