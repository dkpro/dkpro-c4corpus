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
package de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.standalone;

import de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.BoilerPlateRemoval;
import de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.impl.JusTextBoilerplateRemoval;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.CharsetDetector;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.impl.ICUCharsetDetectorWrapper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * This class takes one HTML file as input, removes boilerplate for each entry,
 * and write the results to the output txt file. args[0] is the input html file,
 * args[1] is the output file, args[2] is a flag for keeping minimal html which is either
 * true or false
 *
 * @author Omnia Zayed
 */
public class HTMLBoilerplateRemoval
{
    private static final BoilerPlateRemoval boilerPlateRemoval = new JusTextBoilerplateRemoval();
    private final static CharsetDetector CHARSET_DETECTOR = new ICUCharsetDetectorWrapper();

    public static void main(String[] args)
            throws IOException
    {
        if (args.length < 3) {
            System.out.println("Not enough arguments - Usage: infile outfile true/false (output HTML tags)");
        }
        File input = new File(args[0]);
        File output = new File(args[1]);
        boolean keepMinimalHtml = Boolean.valueOf(args[2]);

        processHtmlFile(input, output, keepMinimalHtml);
    }

    public static void processHtmlFile(File input, File outFile, boolean keepMinimalHtml)
            throws IOException
    {
        // read the html file
        byte[] bytes = FileUtils.readFileToByteArray(input);
        Charset charset = CHARSET_DETECTOR.detectCharset(bytes);
        String html = new String(bytes, charset);

        // boilerplate removal
        String cleanText;
        if (keepMinimalHtml) {
            cleanText = boilerPlateRemoval.getMinimalHtml(html, null);
        }
        else {
            cleanText = boilerPlateRemoval.getPlainText(html, null);
        }

        // write to the output file
        PrintWriter writer = new PrintWriter(outFile, "utf-8");
        writer.write(cleanText);

        writer.close();
    }
}
