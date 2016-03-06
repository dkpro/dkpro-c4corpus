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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics;

import com.google.common.net.InternetDomainName;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.helper.TextLongCountingReducer;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Collecting statistics about the top domains in the crawled corpus. URLs are taken from
 * the WARC header.
 *
 * @author Ivan Habernal
 */
public class TopDomainCounter
        extends Configured
        implements Tool
{
    @Override
    public int run(String[] args)
            throws Exception
    {
        Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

        Job job = Job.getInstance();
        job.setJarByClass(TopDomainCounter.class);

        job.setJobName(TopDomainCounter.class.getName());

        // mapper
        job.setMapperClass(DomainMapper.class);
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

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new TopDomainCounter(), args);
    }

    /**
     * Extracts the top-domain name, i.e.
     * {@code https://www.ukp.tu-darmstadt.de/ukp-home/} becomes {@code tu-darmstadt.de}
     * and omits the domain and count
     */
    public static class DomainMapper
            extends Mapper<LongWritable, WARCWritable, Text, LongWritable>
    {
        private static final LongWritable ONE = new LongWritable(1L);

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            String domain = null;
            String host = null;

            try {
                // extract the top domain
                URI uri = new URI(value.getRecord().getHeader().getTargetURI());
                host = uri.getHost();
            }
            catch (URISyntaxException e) {
                // ignore entry
            }

            if (host != null) {
                try {
                    domain = InternetDomainName.from(host).topPrivateDomain().toString();
                }
                catch (IllegalStateException ex) {
                    // this fails for some cases which are valid, e.g.
                    // Error: java.lang.IllegalStateException: Not under a public suffix: freedom.press
                    domain = host;
                }
                catch (IllegalArgumentException ex) {
                    // Error: java.lang.IllegalArgumentException: Not a valid domain name: '78.20.42.79'
                    // ignore
                }
            }

            if (domain != null) {
                context.write(new Text(domain), ONE);
            }

        }
    }
}
