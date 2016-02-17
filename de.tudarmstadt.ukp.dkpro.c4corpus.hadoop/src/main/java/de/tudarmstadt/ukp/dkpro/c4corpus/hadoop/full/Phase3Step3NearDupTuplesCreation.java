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
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.deduplication.DocumentInfo;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * This class create tuples from a given set of documents by calculating the
 * hamming distance. Two documents are said to be similar if their hamming
 * distance is greater than a certain threshold.
 *
 * @author Omnia Zayed
 */
public class Phase3Step3NearDupTuplesCreation
        extends Configured
        implements Tool
{

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        job.setJarByClass(Phase3Step3NearDupTuplesCreation.class);
        job.setJobName(Phase3Step3NearDupTuplesCreation.class.getName());

        // mapper
        job.setMapperClass(CreateTuplesMapper.class);
        job.setMapOutputKeyClass(NullWritable.class);
        job.setMapOutputValueClass(TreeSet.class);

        job.setInputFormatClass(TextInputFormat.class);
        LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);

        // paths
        String commaSeparatedInputFiles = args[0];
        String outputPath = args[1];

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        job.setNumReduceTasks(0); //must be added or the mapper wont be called

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase3Step3NearDupTuplesCreation(), args);
    }

    public static class CreateTuplesMapper
            extends Mapper<LongWritable, Text, NullWritable, TreeSet<DocumentInfo>>
    {

        private MultipleOutputs<NullWritable, TreeSet<DocumentInfo>> multipleOutputs;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            multipleOutputs = new MultipleOutputs<>(context);
        }

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException
        {

            //get the file name
            FileSplit fileSplit = (FileSplit) context.getInputSplit();
            String fileName = fileSplit.getPath().getName();

            //process text as docInfo enteries 
            String[] documents = value.toString().split(",");

            List<String> similarCandidates = new ArrayList<>(Arrays.asList(documents));

            for (int i = 0; i < similarCandidates.size() - 1; i++) {

                //process the head doc
                DocumentInfo headDoc = new DocumentInfo();
                headDoc.createDocumentInfo(similarCandidates.get(i));
                long headDocSimHash = headDoc.getDocSimHash().get();
                //other candidates
                for (int j = i + 1; j < similarCandidates.size(); j++) {
                    DocumentInfo similarDoc = new DocumentInfo();
                    similarDoc.createDocumentInfo(similarCandidates.get(j));
                    long similarDocSimHash = similarDoc.getDocSimHash().get();

                    //calc the hamming distance
                    int hammingDist = SimHashUtils.diffOfBits(headDocSimHash, similarDocSimHash);
                    //if the hamming distance is <=3
                    if (hammingDist <= SimHashUtils.HAMMING_DISTANCE_THRESHOLD) {
                        //save the doc in one cluster
                        //the Document datastructure must implement a compare method
                        //in order to be able to add the document iinto the TreeSet
                        TreeSet<DocumentInfo> cluster = new TreeSet<>();
                        cluster.add(headDoc);
                        cluster.add(similarDoc);
                        if (cluster.size() > 1) {
                            //                            context.write(NullWritable.get(), cluster);
                            multipleOutputs.write(NullWritable.get(), cluster, fileName);
                        }
                    }
                }
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
