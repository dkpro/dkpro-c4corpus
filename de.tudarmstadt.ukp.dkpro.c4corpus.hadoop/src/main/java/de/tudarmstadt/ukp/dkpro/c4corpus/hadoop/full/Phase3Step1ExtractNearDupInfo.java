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

import de.tudarmstadt.ukp.dkpro.c4corpus.deduplication.impl.SimHashUtils;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.deduplication.DeDuplicationTextOutputReducer;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.deduplication.DocumentInfo;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.deduplication.DocumentInfoOutputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCRecord;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * This class takes warc.gz files as input and clusters similar candidates
 * according to the SimHash value. The simHash value is converted to bands of
 * bits. The output is a data structure called Document Info which includes the
 * doc id, simhash & length. the document info is written as a text file.
 *
 * @author Omnia Zayed
 */
public class Phase3Step1ExtractNearDupInfo
        extends Configured
        implements Tool
{

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        job.setJarByClass(Phase3Step1ExtractNearDupInfo.class);
        job.setJobName(Phase3Step1ExtractNearDupInfo.class.getName());

        // mapper
        job.setMapperClass(MapperClass.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(DocumentInfo.class);

        // reducer
        job.setReducerClass(DeDuplicationTextOutputReducer.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(List.class);

        job.setInputFormatClass(WARCInputFormat.class);
        LazyOutputFormat.setOutputFormatClass(job, DocumentInfoOutputFormat.class);

        // paths
        String commaSeparatedInputFiles = args[0];
        String outputPath = args[1];

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;

    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase3Step1ExtractNearDupInfo(), args);
    }

    public static class MapperClass
            extends Mapper<LongWritable, WARCWritable, Text, DocumentInfo>
    {

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            String docText = new String(value.getRecord().getContent());

            //ID of single warc record (document)
            String docID = value.getRecord().getHeader().getRecordID();

            String docSimHashString = value.getRecord().getHeader().getField(
                    WARCRecord.WARCRecordFieldConstants.SIMHASH);
            if (docSimHashString == null) {
                throw new IOException(
                        WARCRecord.WARCRecordFieldConstants.SIMHASH + " metadata not found");
            }
            long docSimHash = Long.valueOf(docSimHashString);

            int docLength = docText.length();

            String language = value.getRecord().getHeader().getField(
                    WARCRecord.WARCRecordFieldConstants.LANGUAGE);

            //get the binary representation of this document split into bands
            Set<String> bandsOfBitsHashIndex = SimHashUtils.computeHashIndex(docSimHash);

            // create a new container
            DocumentInfo d = new DocumentInfo();
            d.setDocID(new Text(docID));
            d.setDocLength(new IntWritable(docLength));
            d.setDocSimHash(new LongWritable(docSimHash));
            d.setDocLanguage(new Text(language));

            for (String bandOfBits : bandsOfBitsHashIndex) {
                context.write(new Text(bandOfBits), d);
            }
        }
    }
}
