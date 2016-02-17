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
import de.tudarmstadt.ukp.dkpro.c4corpus.license.LicenseDetector;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
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
        //set from the command line
        ConfigurationHelper.configureJob(job, Phase1FullJob.class, SimpleMapper.class,
                WARCWriterReducerClass.class, args[0], args[1]);

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

            // bottleneck of single reducer for all "Lic_none_Lang_en" pages (majority of Web)
            int binNumber = 0;
            if ("en".equals(language) && LicenseDetector.NO_LICENCE.equals(license)) {
                // get the last two digits from the simhash
                binNumber = WARCWriterReducerClass
                        .getBinNumberFromSimHash(Long.valueOf(docSimHash));
            }

            context.write(new Text(WARCWriterReducerClass.createOutputFilePrefix(license, language,
                    header.getField(WARCRecord.WARCRecordFieldConstants.NO_BOILERPLATE),
                    binNumber)), value);
        }
    }
}
