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

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.ConfigurationHelper;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCRecord;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collecting statistics over languages and licenses in processed Web corpus.
 *
 * @author Ivan Habernal
 */
public class LangLicenseStatistics
        extends Configured
        implements Tool
{

    private static final Text KEY_DOCUMENTS = new Text("documents");
    private static final Text KEY_TOKENS = new Text("tokens");

    private static final Pattern PATTERN = Pattern.compile("\\s+");

    /**
     * Returns an approximate number of words based on number of whitespace blocks.
     *
     * @param string string
     * @return word count
     */
    public static int fastApproximateWordCount(String string)
    {
        Matcher m = PATTERN.matcher(string);

        int count = 1;
        while (m.find()) {
            count++;
        }

        return count;
    }

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        ConfigurationHelper
                .configureJob(job, LangLicenseStatistics.class, MapperClass.class,
                        ReducerClass.class,
                        args[0], args[1]);

        // intermediate data
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(MapWritable.class);

        // output data
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        job.setOutputFormatClass(TextOutputFormat.class);

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new LangLicenseStatistics(), args);
    }

    public static class MapperClass
            extends Mapper<LongWritable, WARCWritable, Text, MapWritable>
    {

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            WARCRecord warcRecord = value.getRecord();

            // get metadata: language & license
            String lang = warcRecord.getHeader().getField(
                    WARCRecord.WARCRecordFieldConstants.LANGUAGE);

            String license = warcRecord.getHeader()
                    .getField(WARCRecord.WARCRecordFieldConstants.LICENSE);

            if (lang == null) {
                throw new IOException(WARCRecord.WARCRecordFieldConstants.LANGUAGE
                        + " metadata not found. Run language identification first");
            }

            if (license == null) {
                throw new IOException(WARCRecord.WARCRecordFieldConstants.LICENSE
                        + " metadata not found. Run license identification first");
            }

            // get number of tokens
            String content = new String(warcRecord.getContent(), "utf-8");
            int tokenCount = fastApproximateWordCount(content);

            // collect output statistics into map
            MapWritable mapWritable = new MapWritable();
            mapWritable.put(KEY_DOCUMENTS, new IntWritable(1));
            mapWritable.put(KEY_TOKENS, new IntWritable(tokenCount));

            // create tab-delimited key
            Text outputKey = new Text(lang + "\t" + license);

            // write
            context.write(outputKey, mapWritable);
        }
    }

    public static class ReducerClass
            extends Reducer<Text, MapWritable, NullWritable, Text> {

        @Override
        protected void reduce(Text key, Iterable<MapWritable> values, Context context)
                throws IOException, InterruptedException {
            // collect statistics (int is too short for that!)
            long documentCounts = 0;
            long tokensCount = 0;
            for (MapWritable mapWritable : values) {
                documentCounts += ((IntWritable) mapWritable.get(KEY_DOCUMENTS)).get();
                tokensCount += ((IntWritable) mapWritable.get(KEY_TOKENS)).get();
            }

            // print as tab separated: language \t license \t documents \t tokens
            context.write(NullWritable.get(),
                    new Text(String.format("%s\t%d\t%d", key, documentCounts, tokensCount)));
        }

    }
}
