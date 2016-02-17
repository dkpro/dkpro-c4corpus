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

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCOutputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCRecord;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Delete warc records given a list of the files IDs to be deleted (Text File)
 * arg0 the txt file of IDs to be deleted arg1 is the original warc data set,
 * arg2 is the output
 *
 * @author Omnia Zayed
 * @author Ivan Habernal
 */
public class Phase4RemoveDuplicatesUsingReduceSideJoins
        extends Configured
        implements Tool
{

    @Override
    public int run(String[] args)
            throws Exception
    {

        Job job = Job.getInstance(getConf());

        job.setJarByClass(Phase4RemoveDuplicatesUsingReduceSideJoins.class);
        job.setJobName(Phase4RemoveDuplicatesUsingReduceSideJoins.class.getName());

        // paths
        String textFilePath = args[0]; //text files of ids to be deleted
        String commaSeparatedInputFiles = args[1]; //corpora
        String outputPath = args[2];
        //second input the look up text file
        MultipleInputs.addInputPath(job, new Path(textFilePath),
                TextInputFormat.class, JoinTextMapper.class);
        //first input the data set (check comma separated availability)
        MultipleInputs.addInputPath(job, new Path(commaSeparatedInputFiles),
                WARCInputFormat.class, JoinWARCMapper.class);

        job.setPartitionerClass(SourceJoiningKeyPartitioner.class);
        job.setGroupingComparatorClass(SourceJoiningGroupingComparator.class);

        job.setMapOutputKeyClass(CompositeKey.class);
        job.setMapOutputValueClass(WARCWritable.class);

        job.setReducerClass(JoinReducer.class);

        job.setOutputFormatClass(WARCOutputFormat.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(WARCWritable.class);

        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        FileOutputFormat.setCompressOutput(job, true);
        FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase4RemoveDuplicatesUsingReduceSideJoins(), args);
    }

    public static class SourceJoiningKeyPartitioner
            extends Partitioner<CompositeKey, WARCWritable>
    {

        @Override
        public int getPartition(CompositeKey compositeKey, WARCWritable warc, int numPartitions)
        {
            return (compositeKey.getJoinKey().hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    public static class SourceJoiningGroupingComparator
            extends WritableComparator
    {

        public SourceJoiningGroupingComparator()
        {
            super(CompositeKey.class, true);
        }

        @Override
        public int compare(WritableComparable a, WritableComparable b)
        {
            CompositeKey compositeKey1 = (CompositeKey) a;
            CompositeKey compositeKey2 = (CompositeKey) b;
            return compositeKey1.getJoinKey().compareTo(compositeKey2.getJoinKey());
        }
    }

    /**
     * Reads the original collection with *warc.gz files, emits under the composite key
     * with {@code sourceIndex} set to 0
     */
    public static class JoinWARCMapper
            extends Mapper<LongWritable, WARCWritable, CompositeKey, WARCWritable>
    {

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {

            String warcId = value.getRecord().getHeader().getRecordID();

            CompositeKey compositeWARCKey = new CompositeKey();
            compositeWARCKey.setJoinKey(new Text(warcId));
            //to determine order of sorting
            compositeWARCKey.setSourceIndex(new IntWritable(0));

            context.write(compositeWARCKey, value);

        }
    }

    /**
     * This class creates a new dummy WARC record with a constant content (but correct ID)
     */
    public static class DummyWARCRecord
            extends WARCRecord
    {
        private static final String DUMMY_CONTENT_PREFIX =
                "WARC/1.0\r\n" + "WARC-Type: warcinfo\r\n"
                        + "WARC-Date: 2014-03-18T17:47:38Z\r\n"
                        + "WARC-Record-ID: ";

        private static final String DUMMY_CONTENT_POSTFIX =
                "\r\n" + "Content-Length: 19\r\n" + "Content-Type: application/warc-fields\r\n"
                        + "WARC-Filename: XXX\r\n" + "\r\n"
                        + "robots: classic\r\n"
                        + "\r\n" + "\r\n" + "\r\n";

        public DummyWARCRecord(String warcId)
                throws IOException
        {
            super(new DataInputStream(new ByteArrayInputStream(
                    (DUMMY_CONTENT_PREFIX + warcId + DUMMY_CONTENT_POSTFIX).getBytes("UTF-8"))));
        }
    }

    /**
     * Reads the the ward IDs intended for deletion, emits the warc ID with a dummy entry
     * under {@code sourceIndex} with value 1 (thus as a second entry)
     */
    public static class JoinTextMapper
            extends Mapper<LongWritable, Text, CompositeKey, WARCWritable>
    {

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException
        {
            String warcId = value.toString();

            CompositeKey compositeWARCKey = new CompositeKey();
            compositeWARCKey.setJoinKey(new Text(warcId));

            // to determine order of sorting
            compositeWARCKey.setSourceIndex(new IntWritable(1));

            WARCWritable emptyWARC = new WARCWritable(new DummyWARCRecord(warcId));

            context.write(compositeWARCKey, emptyWARC);
        }
    }

    /**
     * {@code values} have either two entries, or one entry. If there are two entries, the first
     * one holds the original WARC record (from the {@link JoinWARCMapper}) and the second one
     * the dummy warc file; this entry should be thus deleted (not written to the output). If there
     * is only one entry, it is either the original WARC record (and should be written to
     * the output) of the dummy record, which will be ignored.
     */
    public static class JoinReducer
            extends Reducer<CompositeKey, WARCWritable, NullWritable, WARCWritable>
    {

        private static final Log LOG = LogFactory.getLog(JoinReducer.class);

        @Override
        protected void reduce(CompositeKey key, Iterable<WARCWritable> values, Context context)
                throws IOException, InterruptedException
        {
            List<WARCWritable> documents = new ArrayList<WARCWritable>();

            for (WARCWritable v : values) {
                documents.add(new WARCWritable(v.getRecord()));
            }

            // only max two documents are allowed here: the original one and the dummy one
            // if (documents.size() > 2) {
            //    throw new IllegalStateException("More than 2 documents (" + documents.size()
            //            + " found under the same composite key " + key);
            // }

            // get the first document
            WARCWritable warcWritable = documents.get(0);

            if (documents.size() == 1) {
                // now make sure we don't write out the dummy warc
                // how that can happen? Probably, the warc ID is missing in the input collection
                // but is present in the text files for deletion
                if (warcWritable.getRecord() instanceof DummyWARCRecord) {
                    LOG.info(warcWritable.getRecord().getHeader().getRecordID()
                            + " marked for deletion, but not found in the input collection");
                }
                else {
                    // writing to the output
                    context.write(NullWritable.get(), warcWritable);
                }
            }
            else {
                LOG.info("Deleting near-duplicate: " + warcWritable.getRecord().getHeader()
                        .getRecordID());
            }
        }
    }
}
