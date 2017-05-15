/*
 * Copyright 2017
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
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Very simple sentence extractor from processed C4Corpus. Splits text given a dot and
 * writes to the text output. Suitable for getting plain text data for language modelling.
 *
 * @author Ivan Habernal
 */
public class TextToSentencesSplitter
        extends Configured
        implements Tool
{
    @Override
    public int run(String[] args)
            throws Exception
    {
        Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

        Job job = Job.getInstance(conf);
        job.setJarByClass(TextToSentencesSplitter.class);

        job.setJobName(TextToSentencesSplitter.class.getName());

        // mapper
        job.setMapperClass(TextToSentencesSplitter.MapperClass.class);
        job.setInputFormatClass(WARCInputFormat.class);

        // reducer
        job.setReducerClass(ReducerClass.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
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
        ToolRunner.run(new TextToSentencesSplitter(), args);
    }

    public static class ReducerClass
            extends Reducer<NullWritable, Text, NullWritable, Text>
    {
        @Override protected void reduce(NullWritable key, Iterable<Text> values,
                Context context)
                throws IOException, InterruptedException
        {
            for (Text text : values) {
                context.write(NullWritable.get(), text);
            }
        }
    }

    public static class MapperClass
            extends Mapper<LongWritable, WARCWritable, NullWritable, Text>
    {

        private boolean keepCase;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            super.setup(context);

            // parametrize the mapper
            this.keepCase = context.getConfiguration().getBoolean("c4corpus.keepcase", true);
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
                if (paragraph.startsWith("<p>")) {

                    String clean = paragraph.replaceAll("^<\\w+>", "").replaceAll("</\\w+>$", "");

                    String[] sentences = clean.split("\\. ");
                    for (String sentence : sentences) {
                        context.write(NullWritable.get(), new Text(sentence));
                    }
                }
            }
        }

    }

    /*
    I ran it on 10 warc files which provided enough data for training a language model; with one reducer

    hadoop jar dkpro-c4corpus-hadoop-1.0.1-SNAPSHOT-standalone.jar \
    de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.utils.TextToSentencesSplitter \
    -Dmapreduce.job.queuename=shortrunning -Dmapreduce.job.reduces=1 \
    c4corpus-CC-MAIN-2016-07/cc-phase5out-2016-07/Lic_none_Lang_en_NoBoilerplate_true_MinHtml_true-r-00017.seg-0013*.warc.gz \
    c4enplaintext-sample
     */
}