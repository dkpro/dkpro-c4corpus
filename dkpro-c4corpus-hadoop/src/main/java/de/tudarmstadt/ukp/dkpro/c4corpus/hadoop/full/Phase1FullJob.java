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
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.LanguageIdentifier;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.impl.CybozuLanguageIdentifier;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.impl.ICUCharsetDetectorWrapper;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCInputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCOutputFormat;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.license.LicenseDetector;
import de.tudarmstadt.ukp.dkpro.c4corpus.license.impl.FastRegexLicenceDetector;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.io.compress.SnappyCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Single Map-Reduce task for performing license identification, boilerplate
 * removal, language identification and sim hashing. Only non-empty texts after
 * boilerplate removal are kept.
 * <p/>
 * Configuration parameters
 * {@code c4corpus.keepminimalhtml} - boolean (keep minimal html in boilerplate removal?)
 *
 * @author Omnia Zayed
 * @author Ivan Habernal
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
        // set from the command line

        job.setJarByClass(Phase1FullJob.class);
        job.setJobName(Phase1FullJob.class.getName());

        // mapper
        job.setMapperClass(MapperClass.class);

        // we will compress the mapper's output (use fast Snappy compressor)
        job.getConfiguration().setBoolean(Job.MAP_OUTPUT_COMPRESS, true);
        job.getConfiguration()
                .setClass(Job.MAP_OUTPUT_COMPRESS_CODEC, SnappyCodec.class, CompressionCodec.class);

        // reducer
        job.setReducerClass(SimpleReducer.class);

        // input-output is warc
        job.setInputFormatClass(WARCInputFormat.class);
        job.setOutputFormatClass(WARCOutputFormat.class);

        // mapper output data
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(WARCWritable.class);

        // set output compression to GZip
        FileOutputFormat.setCompressOutput(job, true);
        FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);

        FileInputFormat.addInputPaths(job, args[0]);
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase1FullJob(), args);
    }

    public static class MapperClass
            extends Mapper<LongWritable, WARCWritable, IntWritable, WARCWritable>
    {

        private final static CharsetDetector CHARSET_DETECTOR = new ICUCharsetDetectorWrapper();
        private final static LicenseDetector LICENSE_DETECTOR = new FastRegexLicenceDetector();
        private final static BoilerPlateRemoval BOILER_PLATE_REMOVAL = new JusTextBoilerplateRemoval();
        private final static LanguageIdentifier LANGUAGE_IDENTIFIER = new CybozuLanguageIdentifier();

        private long recordCounter = 0;

        private long sizeCounter = 0;

        // logger
        private static final Log LOG = LogFactory.getLog(MapperClass.class);

        // utf-8 charset
        private static final Charset UTF8_CHARSET = Charset.forName("utf-8");

        // only meaningful html pages
        private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(
                Arrays.asList("text/html", "application/xhtml+xml"));

        // mapper parameter
        private boolean keepMinimalHTML;

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            super.setup(context);

            // parametrize the mapper
            this.keepMinimalHTML = context.getConfiguration()
                    .getBoolean("c4corpus.keepminimalhtml", false);
        }

        /**
         * Checks whether the given WARC record should be ignored; this applies for documents
         * longer than 10 MB and documents that are not text/html
         *
         * @param value WARC record
         * @return true if ignored, false otherwise
         */
        protected static boolean ignoreWARCRecord(WARCWritable value)
                throws IOException
        {
            // avoid documents bigger than 10 MB as in ClueWeb12
            int contentLength = value.getRecord().getHeader().getContentLength();
            if (contentLength >= 10000000) {
                return true;
            }

            // we're only interested in processing the responses, not requests or metadata
            if (!value.getRecord().isContentApplicationHttpResponse()) {
                return true;
            }

            // HTTP header in CommonCrawl is delimited by newline
            String httpHeaderText = value.getRecord().getHTTPHeaders();

            // we're only interested in text/html
            if (httpHeaderText == null) {
                return true;
            }

            String contentType = WARCRecord.extractHTTPHeaderContentType(httpHeaderText);
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                return true;
            }

            // we accept the page
            return false;
        }

        /**
         * Extracts HTML from the CommonCrawl WARC record with correctly identified encoding and
         * stripped the leading HTTP header
         *
         * @param value WARC record
         * @return HTML as string
         */
        protected String extractHTML(WARCWritable value)
        {
            // detect charset
            byte[] bytes = value.getRecord().getContent();
            Charset charset = CHARSET_DETECTOR.detectCharset(bytes);

            String html = new String(bytes, charset);

            // strip HTTP header
            return html.substring(html.indexOf("\r\n\r\n") + 4);
        }

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            // check first if it's worth processing
            if (ignoreWARCRecord(value)) {
                return;
            }

            // extract HTML
            String html = extractHTML(value);

            // license detection
            String license = LICENSE_DETECTOR.detectLicence(html);

            // boilerplate removal
            String plainText;
            if (this.keepMinimalHTML) {
                plainText = BOILER_PLATE_REMOVAL.getMinimalHtml(html, null);
            }
            else {
                plainText = BOILER_PLATE_REMOVAL.getPlainText(html, null);
            }

            // skip empty documents
            if (plainText.isEmpty()) {
                return;
            }

            // keeping the location and ID of the original file in HDFS in header meta-data
            FileSplit inputSplit = (FileSplit) context.getInputSplit();
            final String origFile = inputSplit.getPath().toString();

            // language identification
            final String language = LANGUAGE_IDENTIFIER.identifyLanguage(plainText);

            // compute simhash
            long docSimHash = ParallelDocumentDeDuplication.getSimHash(plainText);

            WARCRecord.Header header = value.getRecord().getHeader();

            // original warc split location
            header.setField(WARCRecord.WARCRecordFieldConstants.ORIGINAL_LOCATION, origFile);

            // set the license to the metadata
            header.setField(WARCRecord.WARCRecordFieldConstants.LICENSE, license);

            //set the language to meta data
            header.setField(WARCRecord.WARCRecordFieldConstants.LANGUAGE, language);

            // add info about boilerplate removal
            String noBoilerplate = Boolean.TRUE.toString();
            header.setField(WARCRecord.WARCRecordFieldConstants.NO_BOILERPLATE, noBoilerplate);

            // add simhash
            header.setField(WARCRecord.WARCRecordFieldConstants.SIMHASH, Long.toString(docSimHash));

            // replace the content with the plain text
            value.getRecord().setContent(plainText);

            // warning: never call getBytes() without specifying charset; will behave
            // differently on different computers (due to default locales!!!)
            byte[] plainTextBytes = plainText.getBytes(UTF8_CHARSET);
            header.setField("Content-Length", String.valueOf(plainTextBytes.length));

            // create random hash from docSimHash which breaks the hamming distance
            // never use NullWritable as output key!
            // https://support.pivotal.io/hc/en-us/articles/202810986-Mapper-output-key-
            // value-NullWritable-can-cause-reducer-phase-to-move-slowly
            int randomHash = String.valueOf(docSimHash).hashCode() % 1000;

            // create prefix as a key
            context.write(new IntWritable(randomHash), value);

            // collect some stats to logs
            recordCounter++;
            sizeCounter += plainText.length();
            if ((recordCounter % 1000) == 0) {
                LOG.info(String.format("Processed %d records, total length %d characters",
                        recordCounter, sizeCounter));
            }
        }
    }

    /**
     * Keeps only values
     */
    private class SimpleReducer
            extends Reducer<IntWritable, WARCWritable, NullWritable, WARCWritable>
    {
        @Override
        protected void reduce(IntWritable key, Iterable<WARCWritable> values, Context context)
                throws IOException, InterruptedException
        {
            for (WARCWritable warcWritable : values) {
                context.write(NullWritable.get(), warcWritable);
            }
        }
    }
}
