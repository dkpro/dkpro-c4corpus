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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.impl;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.LanguageIdentifier;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class CybozuLanguageIdentifier
        implements LanguageIdentifier
{
    private static final double PROBABILITY_THRESHOLD = 0.80;

    static final String[] PROFILES = { "af", "bn", "de", "es", "fi", "he", "hu", "ja", "lt", "ml",
            "nl", "pl", "ru", "so", "sw", "th", "uk", "zh-cn", "ar", "cs", "el", "et", "fr", "hi",
            "id", "kn", "lv", "mr", "no", "pt", "sk", "sq", "ta", "tl", "ur", "zh-tw", "bg", "da",
            "en", "fa", "gu", "hr", "it", "ko", "mk", "ne", "pa", "ro", "sl", "sv", "te", "tr",
            "vi" };

    public CybozuLanguageIdentifier()
    {
        List<String> jsonProfiles = new ArrayList<String>();
        for (String profile : PROFILES) {
            // locate the stream
            String resourceName = "profiles/" + profile;
            InputStream inputStream = CybozuLanguageIdentifier.class.getClassLoader()
                    .getResourceAsStream(resourceName);

            if (inputStream == null) {
                throw new RuntimeException(
                        "Cannot locate resource " + resourceName + " on the classpath.");
            }

            // read the profile to string
            StringWriter sw = new StringWriter();
            try {
                IOUtils.copy(inputStream, sw, "utf-8");
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            // add to all profiles
            jsonProfiles.add(sw.toString());
        }

        // and load all languages
        try {
            DetectorFactory.loadProfile(jsonProfiles);
        }
        catch (LangDetectException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String identifyLanguage(String html)
            throws IOException
    {
        // extracting plain html text
        Document doc = Jsoup.parse(html);
        String text = doc.text();

        // we might have removed everything -> no lang
        if (text.isEmpty()) {
            return UNKNOWN_LANGUAGE;
        }

        try {
            Detector detector = DetectorFactory.create();
            detector.append(text);
            String detectedLang = detector.detect();

            ArrayList<Language> detectedProbabilities = detector.getProbabilities();

            if (detectedProbabilities.get(0).prob > PROBABILITY_THRESHOLD) {
                return detectedLang;
            }
            else {
                return UNKNOWN_LANGUAGE;
            }
        }
        catch (LangDetectException e) {
            return UNKNOWN_LANGUAGE;
        }
    }

}
