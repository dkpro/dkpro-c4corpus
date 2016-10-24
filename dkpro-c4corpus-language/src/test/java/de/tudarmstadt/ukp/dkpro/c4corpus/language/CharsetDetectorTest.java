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

package de.tudarmstadt.ukp.dkpro.c4corpus.language;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.CharsetDetector;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.impl.ICUCharsetDetectorWrapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Simple test for character encoding
 *
 * @author Ivan Habernal
 */
public class CharsetDetectorTest
{

    final static CharsetDetector detector = new ICUCharsetDetectorWrapper();

    @Test
    public void testDetectCharset()
            throws Exception
    {
        // resources/charset-detector contains *.txt files
        URL url = getClass().getClassLoader().getResource("charset-detector");
        assertNotNull(url);

        File dir = new File(url.getFile());

        File[] files = dir.listFiles();
        assertNotNull(files);

        for (File file : files) {
            // file names are: charset_lang.txt (_lang is optional!)
            String charsetName = file.getName().replaceAll("\\.txt$", "").split("_")[0];

            Charset goldCharset = Charset.forName(charsetName);

            byte[] bytes = IOUtils.toByteArray(new FileInputStream(file));

            // distracting the method by wrong declared charset
            String distract = "utf-16LE";
            Charset detectedCharset = detector.detectCharset(bytes, distract);

            // System.out.println(detectedCharset);

            assertEquals(goldCharset, detectedCharset);
        }
    }
}