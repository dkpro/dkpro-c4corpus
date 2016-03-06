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

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * @author Omnia Zayed
 */
public class WARCWordDistribution
        extends WordDistributionStatisticsCollector
{
    @Override Class<? extends InputFormat> getInputFormatClass()
    {
        return WARCInputFormat.class;
    }

    @Override Class<? extends Mapper> getMapperClass()
    {
        return TokenizerMapper.class;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new WARCWordDistribution(), args);
    }

    public static class TokenizerMapper
            extends Mapper<LongWritable, WARCWritable, Text, IntWritable>
    {

        private static final Text word = new Text();
        private final static IntWritable one = new IntWritable(1);
        private String tokens = "[_|$#<>\\^=\\[\\]\\*/\\\\,;.\\-:()?!\"`'{}]";

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            WARCRecord warcRecord = value.getRecord();
            String content = new String(warcRecord.getContent(), "utf-8");
            String cleanContent = content.toLowerCase().replaceAll(tokens, " ");
            StringTokenizer itr = new StringTokenizer(cleanContent);
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken().trim());
                context.write(word, one);
            }
        }
    }
}
