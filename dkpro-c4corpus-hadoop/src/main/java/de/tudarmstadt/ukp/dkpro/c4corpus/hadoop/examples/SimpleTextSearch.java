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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example how to search for a given regex in the processed C4Corpus
 *
 * @author Ivan Habernal
 */
public class SimpleTextSearch
        extends Configured
        implements Tool
{

    public static final String MAPREDUCE_MAP_REGEX = "mapreduce.map.search.regex";

    @Override
    public int run(String[] args)
            throws Exception
    {
        Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

        Job job = Job.getInstance();
        job.setJarByClass(SimpleTextSearch.class);

        job.setJobName(SimpleTextSearch.class.getName());

        // mapper
        job.setMapperClass(TextSearchMapper.class);
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

        // regex with a phrase to be searched for
        String regex = otherArgs[2];
        job.getConfiguration().set(MAPREDUCE_MAP_REGEX, regex);

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new SimpleTextSearch(), args);
    }

    /**
     * Reads clean text (can contain "minimal html") and emits occurrences of given patterns
     */
    public static class TextSearchMapper
            extends Mapper<LongWritable, WARCWritable, Text, LongWritable>
    {
        private static final LongWritable ONE = new LongWritable(1L);

        private Pattern pattern;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            pattern = Pattern.compile(context.getConfiguration().get(MAPREDUCE_MAP_REGEX));
        }

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

                // now we have a plain text for each paragraph, let's do a regex search
                for (String match : searchRegex(clean, pattern)) {
                    context.write(new Text(match), ONE);
                }
            }
        }

        /**
         * Searches using the given regex
         *
         * @param text    text
         * @param pattern regex
         * @return list of matching text (never null)
         */
        public static List<String> searchRegex(String text, Pattern pattern)
        {
            List<String> result = new ArrayList<>();

            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                result.add(matcher.group());
            }

            return result;
        }
    }

}
