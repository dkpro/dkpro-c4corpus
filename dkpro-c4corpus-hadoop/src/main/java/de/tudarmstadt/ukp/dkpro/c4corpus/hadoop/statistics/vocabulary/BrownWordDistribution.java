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

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * @author Omnia Zayed
 */
public class BrownWordDistribution
        extends WordDistributionStatisticsCollector
{
    @Override Class<? extends InputFormat> getInputFormatClass()
    {
        return TextInputFormat.class;
    }

    @Override Class<? extends Mapper> getMapperClass()
    {
        return TokenizerMapper.class;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new BrownWordDistribution(), args);
    }

    public static class TokenizerMapper
            extends Mapper<LongWritable, Text, Text, IntWritable>
    {
        private final static IntWritable ONE = new IntWritable(1);

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException
        {
            //pre-process the lines by removing the pos tags
            String lineWithoutTags = value.toString().replaceAll("(/\\S+\\s)", " ");
            String cleanLine = lineWithoutTags.toLowerCase().replaceAll(REPLACED_CHARACTERS, " ");

            StringTokenizer itr = new StringTokenizer(cleanLine);
            while (itr.hasMoreTokens()) {
                String word = itr.nextToken().trim();
                context.write(new Text(word), ONE);
            }
        }
    }

}
