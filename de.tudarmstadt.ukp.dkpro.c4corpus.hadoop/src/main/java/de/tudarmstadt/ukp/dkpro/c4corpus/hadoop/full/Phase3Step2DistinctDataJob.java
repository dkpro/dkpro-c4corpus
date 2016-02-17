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

import java.io.IOException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * this class is used as a preprocessing step to remove redundant (duplicates)
 * lines from a text file. It will be basically used instead of the pig distinct
 * command after step 1 & 3 in the Near Duplicates De-duplication
 *
 * @author Omnia Zayed
 */
public class Phase3Step2DistinctDataJob extends Configured
        implements Tool {

    @Override
    public int run(String[] args)
            throws Exception {

//         Configuration conf = new Configuration();
//        //allocte memory (from command line)
//        conf.setInt("mapreduce.map.memory.mb", 5120);
//        conf.set("mapreduce.map.java.opts", "-Xmx4280m");
//        conf.setInt("mapreduce.reduce.memory.mb", 5120);
//        conf.set("mapreduce.reduce.java.opts", "-Xmx4096m");
        Job job = Job.getInstance(getConf());
//        //set from the command line
//        job.getConfiguration().set("mapreduce.job.queuename", "longrunning");
        job.setJarByClass(Phase3Step2DistinctDataJob.class);
        job.setJobName(Phase3Step2DistinctDataJob.class.getName());

        //mapper
        job.setMapperClass(RemoveRedundantDataMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(NullWritable.class);

        //reducer
        job.setReducerClass(RemoveRedundantDataReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        //paths
        String commaSeparatedInputFiles = args[0];
        String outputPath = args[1];

        job.setInputFormatClass(TextInputFormat.class);
        LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
        
        //i/o paths
        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception {
        ToolRunner.run(new Phase3Step2DistinctDataJob(), args);
    }

    public static class RemoveRedundantDataMapper
            extends Mapper<LongWritable, Text, Text, NullWritable> {

        @Override
        public void map(LongWritable ignore, Text value, Context context)
                throws java.io.IOException, InterruptedException {
            FileSplit fileSplit = (FileSplit) context.getInputSplit();
            String fileName = fileSplit.getPath().getName().replaceAll("-r-.*", "");
            String outputKey = fileName + "_" + value.toString();
            context.write(new Text(outputKey), NullWritable.get());
        }
    }

    public static class RemoveRedundantDataReducer
            extends Reducer<Text, NullWritable, Text, NullWritable> {

        private MultipleOutputs<Text, NullWritable> multipleOutputs;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException {
            multipleOutputs = new MultipleOutputs<Text, NullWritable>(context);
        }

        @Override
        public void reduce(Text key, Iterable<NullWritable> values, Context context)
                throws IOException, InterruptedException {
            String fileName = key.toString().split("_")[0];
            String outputKey = key.toString().split("_")[1];
            
            multipleOutputs.write(new Text(outputKey), NullWritable.get(), fileName);
        }

        @Override
        protected void cleanup(Context context)
                throws IOException, InterruptedException {
            multipleOutputs.close();
        }
    }
}
