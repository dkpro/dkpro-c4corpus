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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.standalone;

import de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.BoilerPlateRemoval;
import de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.impl.JusTextBoilerplateRemoval;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.CharsetDetector;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.impl.ICUCharsetDetectorWrapper;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCFileWriter;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * This class takes one warc.gz file as input, removes boilerplate for each entry, and write the
 * results to the output warc.gz file.
 *
 * @author Ivan Habernal
 */
public class WarcBoilerplateRemoval
{
    public static void main(String[] args)
            throws IOException
    {
        File input = new File(args[0]);
        File output = new File(args[1]);

        processWarcGzFile(input, output);
    }

    public static void processWarcGzFile(File input, File outFile)
            throws IOException
    {
        System.out.printf("Reading from %s, writing to %s%n", input, outFile);

        Configuration conf = new Configuration();
        // set limit to 100 GB (= almost unlimited)
        conf.setLong("warc.output.segment.size", WARCFileWriter.DEFAULT_MAX_SEGMENT_SIZE * 100);

        //Opens a file for reading.
        CompressionCodec codec = WARCFileWriter.getGzipCodec(conf);
        InputStream byteStream = new BufferedInputStream(new FileInputStream(input));
        DataInputStream dataStream = new DataInputStream(
                codec == null ? byteStream : codec.createInputStream(byteStream));

        BoilerPlateRemoval boilerPlateRemoval = new JusTextBoilerplateRemoval();

        long startTime = System.currentTimeMillis();
        int counter = 0;

        int recordsRead = 0;

        Path outputPath = new Path(outFile.getAbsolutePath());
        WARCFileWriter warcFileWriter = new WARCFileWriter(conf, codec, outputPath);

        // detecting the correct charset
        final CharsetDetector charsetDetector = new ICUCharsetDetectorWrapper();

        while (true) {
            try {
                //Reads the next record from the file.
                WARCRecord wc = new WARCRecord(dataStream);

                // detect charset
                byte[] bytes = wc.getContent();
                Charset charset = charsetDetector.detectCharset(bytes);

                String html = new String(bytes, charset);

                // strip HTTP header
                html = html.substring(html.indexOf("\r\n\r\n") + 4);

                String plainText = boilerPlateRemoval.getPlainText(html, null);

                counter++;
                if (counter % 100 == 0) {
                    System.out.printf(Locale.ENGLISH, "~%.1f entries per second%n",
                            counter * 1000f / (double) (System.currentTimeMillis() - startTime));
                    System.out.printf(Locale.ENGLISH, "%d records processed%n", recordsRead);
                }

                recordsRead++;

                // create copy of WarcRecord
                WARCRecord newWarcRecord = new WARCRecord(wc);
                newWarcRecord.setContent(plainText);

                warcFileWriter.write(newWarcRecord);
            }
            catch (EOFException e) {
                break;
            }
        }

        warcFileWriter.close();

        // rename from out.warc.gz.seg-00000.warc.gz to out.warc.gz
        File actualOutputFile = new File(outFile.getAbsolutePath() + ".seg-00000.warc.gz");
        if (!actualOutputFile.exists()) {
            throw new IOException("File " + actualOutputFile + " does not exist");
        }
        if (!actualOutputFile.renameTo(outFile)) {
            throw new IOException(
                    "Renaming file " + actualOutputFile + " to " + outFile + " failed");
        }

        // delete .crc file
        File crcFile = new File(actualOutputFile.getParentFile(),
                "." + actualOutputFile.getName() + ".crc");
        if (!crcFile.delete()) {
            throw new IOException(crcFile + " was not deleted");
        }

        System.out.printf(Locale.ENGLISH, "%d records written to %s, total time %f%n", recordsRead,
                outFile.getName(),
                counter * 1000f / (double) (System.currentTimeMillis() - startTime));
    }
}
