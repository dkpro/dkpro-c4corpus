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

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.ConfigurationHelper;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Iterator;

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
        ConfigurationHelper.configureJob(job, Phase1FullJob.class, ExactMatchDetectionMapper.class,
                ExactMatchDetectionReducer.class, args[0], args[1]);

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

    public static class ExactMatchDetectionReducer
            extends Reducer<Text, WARCWritable, NullWritable, WARCWritable>
    {

        private MultipleOutputs<NullWritable, WARCWritable> multipleOutputs;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            multipleOutputs = new MultipleOutputs<>(context);
        }

        @Override
        protected void reduce(Text key, Iterable<WARCWritable> values, Context context)
                throws IOException, InterruptedException
        {
            // write only the first value, as the others are exact duplicates
            Iterator<WARCWritable> iterator = values.iterator();
            if (iterator.hasNext()) {
                WARCWritable warcWritable = iterator.next();

                WARCWriterReducerClass
                        .writeSingleWARCWritableToOutput(warcWritable, multipleOutputs);
            }
        }

        @Override
        protected void cleanup(Context context)
                throws IOException, InterruptedException
        {
            multipleOutputs.close();
        }
    }
}
