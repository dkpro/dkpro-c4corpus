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

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import java.io.IOException;
import java.util.Locale;

/**
 * Reducer for writing WARC records to output files given the language, license, etc.
 *
 * @author Ivan Habernal
 */
public class WARCWriterReducerClass
        extends Reducer<Text, WARCWritable, NullWritable, WARCWritable>
{
    /**
     * Returns prefix of the output warc file given the parameters; this method is also as a key
     * for distributing entries to reducers.
     * <br>
     * The result has this format:
     * {@code Lic_LICENSE_Lang_LANGUAGE_NoBoilerplate_BOOLEAN_Bin_BINNUMBER}
     * or
     * {@code Lic_LICENSE_Lang_LANGUAGE_NoBoilerplate_BOOLEAN} if binNumber is zero
     *
     * @param license       license
     * @param language      lang
     * @param noBoilerplate boolean value
     * @param minimalHtml   boolean value
     * @return string prefix
     * @throws IllegalArgumentException if any of the parameter is {@code null} or empty
     */
    public static String createOutputFilePrefix(String license, String language,
            String noBoilerplate, String minimalHtml)
    {
        if (license == null || license.isEmpty()) {
            throw new IllegalArgumentException("Licence is null/empty (val: '" + license + "')");
        }

        if (language == null || language.isEmpty()) {
            throw new IllegalArgumentException("Language is null/empty (val: '" + language + "')");
        }

        if (noBoilerplate == null || noBoilerplate.isEmpty()) {
            throw new IllegalArgumentException(
                    "noBoilerplate is null/empty (val: '" + noBoilerplate + "')");
        }

        if (minimalHtml == null || minimalHtml.isEmpty()) {
            throw new IllegalArgumentException(
                    "minimalHtml is null/empty (val: '" + minimalHtml + "')");
        }

        return String.format(Locale.ENGLISH, "Lic_%s_Lang_%s_NoBoilerplate_%s_MinHtml_%s", license,
                language,
                noBoilerplate, minimalHtml);
    }

    @Override
    protected void reduce(Text key, Iterable<WARCWritable> values, Context context)
            throws IOException, InterruptedException
    {
        for (WARCWritable warcWritable : values) {
            context.write(NullWritable.get(), warcWritable);
        }
    }

    /**
     * Writes single WARCWritable to the output with specific output file prefix
     *
     * @param warcWritable    warc record
     * @param multipleOutputs output
     * @throws IOException          exception
     * @throws InterruptedException exception
     */
    // TODO move somewhere else?
    public static void writeSingleWARCWritableToOutput(WARCWritable warcWritable,
            MultipleOutputs<NullWritable, WARCWritable> multipleOutputs)
            throws IOException, InterruptedException
    {
        WARCRecord.Header header = warcWritable.getRecord().getHeader();
        String license = header.getField(WARCRecord.WARCRecordFieldConstants.LICENSE);
        String language = header.getField(WARCRecord.WARCRecordFieldConstants.LANGUAGE);
        String noBoilerplate = header
                .getField(WARCRecord.WARCRecordFieldConstants.NO_BOILERPLATE);
        String minimalHtml = header.getField(WARCRecord.WARCRecordFieldConstants.MINIMAL_HTML);

        // set the file name prefix
        String fileName = createOutputFilePrefix(license, language, noBoilerplate, minimalHtml);

        // bottleneck of single reducer for all "Lic_none_Lang_en" pages (majority of Web)
        //        if ("en".equals(language) && LicenseDetector.NO_LICENCE.equals(license)) {
        //            long simHash = Long
        //                    .valueOf(header.getField(WARCRecord.WARCRecordFieldConstants.SIMHASH));
        //            int binNumber = getBinNumberFromSimHash(simHash);
        //            fileName = createOutputFilePrefix(license, language, noBoilerplate);
        //        }

        multipleOutputs.write(NullWritable.get(), warcWritable, fileName);
    }
}
