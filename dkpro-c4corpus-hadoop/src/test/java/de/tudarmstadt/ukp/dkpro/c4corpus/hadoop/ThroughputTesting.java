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

/**
 * @author Ivan Habernal
 */
public class ThroughputTesting
{

    /*
    @Test
    public void testReading()
            throws Exception
    {
        //gz file
        File input = new File("0009wb-86.warc.gz");
        //        File input = new File("/home/user-ukp/data2/c4corpus-tests/0001wb-34.warc.gz");
        //        File input = new File("/home/user-ukp/data2/c4corpus-tests/0310wb-59.warc.gz");
        //        File input = new File("/home/user-ukp/data2/c4corpus-tests/0308wb-83.warc.gz");
        //        File input = new File("split.00_20150305143820.warc.gz");

        //Opens a file for reading. 
        CompressionCodec codec = WARCFileWriter.getGzipCodec(new Configuration());
        InputStream byteStream = new BufferedInputStream(new FileInputStream(input));
        DataInputStream dataStream = new DataInputStream(
                codec == null ? byteStream : codec.createInputStream(byteStream));

        LicenseDetector licenseDetectorBasic = new FastRegexLicenceDetector();

        BoilerPlateRemoval boilerPlateRemoval = new JusTextBoilerplateRemoval();

        LanguageIdentifier languageIdentifier = new CybozuLanguageIdentifier();

        long startTime = System.currentTimeMillis();
        int counter = 0;

        Frequency langFrequency = new Frequency();
        Frequency lengthFrequency = new Frequency();
        Frequency licenseFrequency = new Frequency();

        int recordsRead = 0;

        while (true) {
            try {
                long documentStartTime = System.currentTimeMillis();
                //Reads the next record from the file.
                WARCRecord wc = new WARCRecord(dataStream);

                WARCRecord.Header header = wc.getHeader();
                byte[] content = wc.getContent();

                System.out.println(header.getContentType());
                System.out.println(header.getRecordID());

                String html = new String(content, "utf-8");
                //                //write content to file
                //                FileUtils.writeStringToFile(new File("RecordsIn_" + input.getName().replaceAll("\\.warc\\.gz", "")
                //                        + "/" + header.getRecordID() + ".txt"), html, "utf-8");

                //License Detcetion
                System.out.println("Start License detection");
                String licence = licenseDetectorBasic.detectLicence(html);
                //            System.out.println(licence);
                //            System.out.println(html.length());
                licenseFrequency.addValue(licence);

                //Boilerplate removal
                System.out.println("Start Boilerplate");
                String minimalHtml = boilerPlateRemoval.getMinimalHtml(html, null);
                lengthFrequency.addValue(minimalHtml.length() / 100);

                //Language Detection
                System.out.println("Start Language detection");
                String language = languageIdentifier.identifyLanguage(minimalHtml);
                langFrequency.addValue(language);
                System.out.println(language);

                counter++;
                System.out.println("Time taken to process this document in millis: "
                        + String.valueOf(System.currentTimeMillis() - documentStartTime));
                System.out.printf(Locale.ENGLISH, "%f entries per second%n",
                        counter * 1000f / (double) (System.currentTimeMillis() - startTime));

                // full licence detection = 2 entries per second
                // without licence detection = 1324.718956 entries per second
                System.out.println(recordsRead);
                recordsRead++;
            }
            catch (EOFException e) {
                break;
            }
        }

        System.out.println(langFrequency);
        System.out.println(lengthFrequency);
        System.out.println(licenseFrequency);

    }
    */
}
