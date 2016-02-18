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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCOutputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;

import java.io.IOException;

/**
 * Common configuration for all jobs with warc.gz input and warc.gz output
 *
 * @author Ivan Habernal
 */
public class ConfigurationHelper
{
    /**
     * Job configurator TODO javadoc
     *
     * @param job
     * @param jarByClass
     * @param mapperClass
     * @param reducerClass
     * @param commaSeparatedInputFiles
     * @param outputPath
     * @throws IOException
     */
    public static void configureJob(Job job, Class<?> jarByClass,
            Class<? extends Mapper> mapperClass, Class<? extends Reducer> reducerClass,
            String commaSeparatedInputFiles, String outputPath)
            throws IOException
    {
        job.setJarByClass(jarByClass);
        job.setJobName(jarByClass.getName());

        // mapper
        job.setMapperClass(mapperClass);

        // reducer
        job.setReducerClass(reducerClass);

        // input-output is warc
        job.setInputFormatClass(WARCInputFormat.class);
        // prevent producing empty files
        LazyOutputFormat.setOutputFormatClass(job, WARCOutputFormat.class);

        // intermediate data
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(WARCWritable.class);

        // output data
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(WARCWritable.class);

        // set output compression to GZip
        FileOutputFormat.setCompressOutput(job, true);
        FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
    }

}
