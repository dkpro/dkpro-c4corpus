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

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.helper.TextLongCountingReducer;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * Document length distribution over content type and charset (mixed in one output)
 *
 * @author Ivan Habernal
 */
public class ContentTypeAndSizeDistribution
        extends Configured
        implements Tool
{
    @Override public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        job.setJarByClass(ContentTypeAndSizeDistribution.class);

        job.setJobName(ContentTypeAndSizeDistribution.class.getName());

        // mapper
        job.setMapperClass(ContentAndSizeMapper.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        // reducer
        //        job.setReducerClass(DistributionReducer.class);
        job.setReducerClass(TextLongCountingReducer.class);

        job.setInputFormatClass(WARCInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

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
        ToolRunner.run(new ContentTypeAndSizeDistribution(), args);
    }

    /**
     * Mapper; omits ContentType and size
     */
    public static class ContentAndSizeMapper
            extends Mapper<LongWritable, WARCWritable, Text, LongWritable>
    {
        @Override protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            // avoid documents bigger than 10 MB as in ClueWeb12
            int contentLength = value.getRecord().getHeader().getContentLength();

            // we're only interested in processing the responses, not requests or metadata
            if (!value.getRecord().isContentApplicationHttpResponse()) {
                return;
            }

            // HTTP header in CommonCrawl is delimited by newline
            String httpHeaderText = value.getRecord().getHTTPHeaders();

            // we're only interested in text/html
            if (httpHeaderText != null) {
                String httpHeaderContentType = WARCRecord
                        .extractHTTPHeaderContentType(httpHeaderText);
                String httpHeaderCharset = WARCRecord.extractHTTPHeaderCharset(httpHeaderText);

                if (httpHeaderContentType != null) {
                    context.write(new Text("content\t" + httpHeaderContentType),
                            new LongWritable(contentLength));
                }

                if (httpHeaderCharset != null) {
                    context.write(new Text("charset\t" + httpHeaderCharset),
                            new LongWritable(contentLength));
                }
            }
        }
    }

}

