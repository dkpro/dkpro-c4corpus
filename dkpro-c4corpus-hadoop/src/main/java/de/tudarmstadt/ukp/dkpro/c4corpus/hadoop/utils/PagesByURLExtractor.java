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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.utils;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full.Phase1FullJob;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCOutputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Given a list of URLs, this MR job extracts only these pages. I/O = warc.gz
 * <br>
 * In order to create only a single warc.gz file, run with
 * {@code -Dmapreduce.job.reduces=1} parameter
 * <br>
 * Usage
 * <pre>
 * $ hadoop jar dkpro-c4corpus-hadoop-1.0.0.jar \
 * de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.utils.PagesByURLExtractor \
 * s3://ukp-research-data/c4corpus/CC-MAIN-2015-48/cc-final-2015-48/*.warc.gz \
 * s3://your-bucket/output/ \
 * local-file-with-URLs.txt
 * </pre>
 * where the {@code local-file-with-URLs.txt} contains one URL per line, e.g.
 * <pre>
 * arbitrary-info-token \t url1
 * another-arbitrary-info-token \t url2
 * ...
 * </pre>
 *
 * @author Ivan Habernal
 */
public class PagesByURLExtractor
        extends Configured
        implements Tool
{
    /**
     * Property for storing URL list
     */
    public static final String MAPREDUCE_MAPPER_URLS = "mapreduce.mapper.urls";

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        for (Map.Entry<String, String> next : job.getConfiguration()) {
            System.out.println(next.getKey() + ": " + next.getValue());
        }

        job.setJarByClass(PagesByURLExtractor.class);
        job.setJobName(PagesByURLExtractor.class.getName());

        // mapper
        job.setMapperClass(MapperClass.class);

        // input
        job.setInputFormatClass(WARCInputFormat.class);

        // output
        job.setOutputFormatClass(WARCOutputFormat.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(WARCWritable.class);
        FileOutputFormat.setCompressOutput(job, true);

        // paths
        String commaSeparatedInputFiles = args[0];
        String outputPath = args[1];

        // load IDs to be searched for
        job.getConfiguration().set(MAPREDUCE_MAPPER_URLS, loadURLs(args[2]));

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    /**
     * Returns a line-delimited string of URLs from the given file
     * <pre>
     * info url1
     * info url2
     * ...
     * </pre>
     *
     * @param urlFile file
     * @return a new-line delimited URLs
     * @throws IOException
     */
    String loadURLs(String urlFile)
            throws IOException
    {
        // the path needs to be handled by Hadoop FS
        Path path = new Path(urlFile);
        FileSystem fileSystem = path.getFileSystem(getConf());

        FSDataInputStream fsDataInputStream = fileSystem.open(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(fsDataInputStream));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            // split line
            sb.append(line.split("\t")[1]);
            sb.append("\n");
        }

        // close
        fsDataInputStream.close();

        return sb.toString();
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new PagesByURLExtractor(), args);
    }

    /**
     * Mapper; omits WARCWritable for matching entries (with particular URL)
     */
    public static class MapperClass
            extends Mapper<LongWritable, WARCWritable, NullWritable, WARCWritable>
    {
        final Set<String> urls = new HashSet<>();

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            super.setup(context);

            String input = context.getConfiguration().get(MAPREDUCE_MAPPER_URLS);
            urls.addAll(Arrays.asList(input.split("\n")));
        }

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            // only html
            if (!Phase1FullJob.MapperClass.ignoreWARCRecord(value)) {
                String url = value.getRecord().getHeader().getTargetURI();

                if (urls.contains(url)) {
                    context.write(NullWritable.get(), value);
                }
            }
        }
    }

}
