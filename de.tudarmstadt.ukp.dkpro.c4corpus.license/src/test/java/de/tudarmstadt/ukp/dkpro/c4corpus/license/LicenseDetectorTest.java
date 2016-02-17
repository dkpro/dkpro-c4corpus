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
package de.tudarmstadt.ukp.dkpro.c4corpus.license;

import de.tudarmstadt.ukp.dkpro.c4corpus.license.impl.FastRegexLicenceDetector;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Omnia Zayed
 * @author Ivan Habernal
 */
@RunWith(value = Parameterized.class)

public class LicenseDetectorTest
{

    private String data;
    private String licence;

    public LicenseDetectorTest(String data, String licence)
    {
        this.data = data;
        this.licence = licence;
    }

    @Parameterized.Parameters(name = "{index}: file {0} is {1}")
    public static Iterable<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                        { "multi-links.txt", "by-nc-nd" },
                        { "multi-line-link1.txt", "by-nc-sa" },
                        { "multi-line-link2.txt", "by-sa" }
                }
        );
    }

    @Test
    public final void testDetection()
            throws IOException
    {

        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(
                this.data);

        String html = IOUtils.toString(stream);
        //        LicenseDetectorBasic detector = new LicenseDetectorBasic();
        LicenseDetector detector = new FastRegexLicenceDetector();
        String detectedLicence = detector.detectLicence(html);

        assertEquals(this.licence, detectedLicence);
    }
}