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

import de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.BoilerPlateRemoval;
import de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.impl.JusTextBoilerplateRemoval;
import de.tudarmstadt.ukp.dkpro.c4corpus.deduplication.impl.ParallelDocumentDeDuplication;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.CharsetDetector;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.ConfigurationHelper;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.LanguageIdentifier;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.impl.CybozuLanguageIdentifier;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.impl.ICUCharsetDetectorWrapper;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCRecord;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.license.LicenseDetector;
import de.tudarmstadt.ukp.dkpro.c4corpus.license.impl.FastRegexLicenceDetector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Single Map-Reduce task for performing license identification, boilerplate
 * removal, language identification and sim hashing. Only non-empty texts after
 * boilerplate removal are kept.
 *
 * @author Omnia Zayed
 */
public class Phase1FullJob
        extends Configured
        implements Tool
{

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());
        //set from the command line
        ConfigurationHelper.configureJob(job, Phase1FullJob.class, MapperClass.class,
                WARCWriterReducerClass.class, args[0], args[1]);

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase1FullJob(), args);
    }

    public static class MapperClass
            extends Mapper<LongWritable, WARCWritable, Text, WARCWritable>
    {

        private final static CharsetDetector charsetDetector = new ICUCharsetDetectorWrapper();
        private final static LicenseDetector licD = new FastRegexLicenceDetector();
        private final static BoilerPlateRemoval boilPlRem = new JusTextBoilerplateRemoval();
        private final static LanguageIdentifier langD = new CybozuLanguageIdentifier();

        private int recordCounter = 0;

        private long sizeCounter = 0;

        // logger
        private static final Log LOG = LogFactory.getLog(MapperClass.class);

        // utf-8 charset
        private static final Charset UTF8_CHARSET = Charset.forName("utf-8");

        private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<String>(
                Arrays.asList("text/html", "application/xhtml+xml"));

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            // avoid documents bigger than 10 MB as in ClueWeb12
            int contentLength = value.getRecord().getHeader().getContentLength();
            if (contentLength >= 10000000) {
                return;
            }

            // we're only interested in processing the responses, not requests or metadata
            if (!value.getRecord().isContentApplicationHttpResponse()) {
                return;
            }

            // HTTP header in CommonCrawl is delimited by newline
            String httpHeaderText = value.getRecord().getHTTPHeaders();

            // we're only interested in text/html
            if (httpHeaderText == null) {
                return;
            }

            String contentType = WARCRecord.extractHTTPHeaderContentType(httpHeaderText);
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                return;
            }

            // detect charset
            byte[] bytes = value.getRecord().getContent();
            Charset charset = charsetDetector.detectCharset(bytes);

            String html = new String(bytes, charset);

            // strip HTTP header
            html = html.substring(html.indexOf("\r\n\r\n") + 4);

            //License Detection
            long licenseDetectStartTime = System.currentTimeMillis();
            String license = licD.detectLicence(html);
            double timeTakenForLic = System.currentTimeMillis() - licenseDetectStartTime;
            if (timeTakenForLic > 500000) {
                LOG.info(String.format(Locale.ENGLISH,
                        "~%.1f time taken for License Detection in milli-seconds%n The record ID is %s",
                        timeTakenForLic, value.getRecord().getHeader().getRecordID()));
            }

            //Boilerplate removal
            long boilerplateStartTime = System.currentTimeMillis();
            String plainText = boilPlRem.getPlainText(html, null);
            double timeTakenForBoilerplate = System.currentTimeMillis() - boilerplateStartTime;
            if (timeTakenForBoilerplate > 500000) {
                LOG.info(String.format(Locale.ENGLISH,
                        "~%.1f time taken for boilerplate removal in milli-seconds%n The record ID is %s",
                        timeTakenForBoilerplate, value.getRecord().getHeader().getRecordID()));

            }

            if (!plainText.isEmpty()) {

                // keeping the location and ID of the original file in HDFS
                FileSplit inputSplit = (FileSplit) context.getInputSplit();
                // add the while HDFS/AWS path to the register key,
                // if u want only the .gz name add .getName()
                final String mapInputFileName = inputSplit.getPath().toString();

                //Language Detection
                final String language = langD.identifyLanguage(plainText);

                // compute simhash
                long docSimHash = ParallelDocumentDeDuplication.getSimHash(plainText);

                WARCRecord.Header header = value.getRecord().getHeader();

                //original warc split location
                header.setField(WARCRecord.WARCRecordFieldConstants.ORIGINAL_LOCATION,
                        mapInputFileName);
                // set the license to the metadata
                header.setField(WARCRecord.WARCRecordFieldConstants.LICENSE, license);

                //set the language to meta data
                header.setField(WARCRecord.WARCRecordFieldConstants.LANGUAGE, language);

                // add info about boilerplate removal
                String noBoilerplate = Boolean.TRUE.toString();
                header.setField(WARCRecord.WARCRecordFieldConstants.NO_BOILERPLATE, noBoilerplate);

                // add simhash
                header.setField(WARCRecord.WARCRecordFieldConstants.SIMHASH,
                        Long.toString(docSimHash));

                //replace the content with the plain text
                value.getRecord().setContent(plainText);

                // warning: never call getBytes() without specifying charset; will behave
                // differently on different computers (due to default locales!!!)
                byte[] plainTextBytes = plainText.getBytes(UTF8_CHARSET);
                header.setField("Content-Length", String.valueOf(plainTextBytes.length));

                // bottleneck of single reducer for all "Lic_none_Lang_en" pages (majority of Web)
                int binNumber = 0;
                if ("en".equals(language) && LicenseDetector.NO_LICENCE.equals(license)) {
                    // get the last two digits from the simhash
                    binNumber = WARCWriterReducerClass.getBinNumberFromSimHash(docSimHash);
                }

                // create prefix as a key
                context.write(new Text(WARCWriterReducerClass.createOutputFilePrefix(license,
                        language, noBoilerplate, binNumber)), value);

                // collect some stats to logs
                recordCounter++;
                sizeCounter += plainText.length();
                if ((recordCounter % 1000) == 0) {
                    LOG.info(String.format("Processed %d records, total length %d characters",
                            recordCounter, sizeCounter));
                }
            }
        }
    }

}
