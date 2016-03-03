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
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCRecord;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
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

/**
 * Reads all input warc.gz files and writes them into separated files based on language and
 * license
 *
 * @author Ivan Habernal
 */
public class Phase5MergeByLangLicJob
        extends Configured
        implements Tool
{

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());
        // set from the command line
        ConfigurationHelper.configureJob(job, Phase5MergeByLangLicJob.class, SimpleMapper.class,
                WARCWriterReducerPhase5.class, args[0], args[1]);

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase5MergeByLangLicJob(), args);
    }

    public static class SimpleMapper
            extends Mapper<LongWritable, WARCWritable, Text, WARCWritable>
    {

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            WARCRecord.Header header = value.getRecord().getHeader();

            String language = header.getField(WARCRecord.WARCRecordFieldConstants.LANGUAGE);
            if (language == null) {
                throw new NullPointerException(
                        "Field " + WARCRecord.WARCRecordFieldConstants.LANGUAGE
                                + " is null. Header: " + header);
            }

            String license = header.getField(WARCRecord.WARCRecordFieldConstants.LICENSE);
            if (license == null) {
                throw new NullPointerException(
                        "Field " + WARCRecord.WARCRecordFieldConstants.LICENSE
                                + " is null. Header: " + header);
            }

            String docSimHash = header.getField(WARCRecord.WARCRecordFieldConstants.SIMHASH);
            if (docSimHash == null) {
                throw new NullPointerException(
                        "Field " + WARCRecord.WARCRecordFieldConstants.SIMHASH
                                + " is null. Header: " + header);
            }

            // fixing previously wrong association in Phase 1 of original location and register
            String originalLocation = header
                    .getField(WARCRecord.WARCRecordFieldConstants.ORIGINAL_LOCATION);
            String register = header.getField(WARCRecord.WARCRecordFieldConstants.REGISTER);
            if (originalLocation == null && register != null) {
                // swap
                header.setField(WARCRecord.WARCRecordFieldConstants.ORIGINAL_LOCATION, register);
                header.setField(WARCRecord.WARCRecordFieldConstants.REGISTER, null);
            }

            // submit to reducers by language
            context.write(new Text(language), value);
        }
    }

    public static class WARCWriterReducerPhase5
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
            for (WARCWritable warcWritable : values) {
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
