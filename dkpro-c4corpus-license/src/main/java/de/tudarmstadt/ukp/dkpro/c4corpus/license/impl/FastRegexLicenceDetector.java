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

package de.tudarmstadt.ukp.dkpro.c4corpus.license.impl;

import de.tudarmstadt.ukp.dkpro.c4corpus.license.LicenseDetector;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Licence detector based on simplified regex for faster processing
 *
 * @author Ivan Habernal
 */
public class FastRegexLicenceDetector
        implements LicenseDetector
{
    static final Pattern LICENSE_ATTRIBUTE_PATTERN = Pattern.compile(
            "http[s]?://creativecommons\\.org/licenses/(by|by-sa|by-nd|by-nc|by-nc-sa|by-nc-nd|publicdomain)[\"/ >]");

    @Override
    public String detectLicence(String html)
    {
        Matcher licenseAttributeMatcher = LICENSE_ATTRIBUTE_PATTERN.matcher(html);

        // storing counts of all difference occurrences of link to CC
        Map<String, Integer> multipleOccurrencesMap = new HashMap<String, Integer>();

        // add all of them to the list
        while (licenseAttributeMatcher.find()) {
            String licence = licenseAttributeMatcher.group(1);

            // add entry
            if (!multipleOccurrencesMap.containsKey(licence)) {
                multipleOccurrencesMap.put(licence, 0);
            }

            // and increase count
            multipleOccurrencesMap.put(licence, multipleOccurrencesMap.get(licence) + 1);
        }

        // no licence found
        if (multipleOccurrencesMap.isEmpty()) {
            return NO_LICENCE;
        }

        // only one link found or if multiple links found but the same type
        if (multipleOccurrencesMap.size() == 1) {
            return multipleOccurrencesMap.entrySet().iterator().next().getKey();
        }

        // if multiple different links found, we return a general CC-UNSPECIFIED
        return CC_UNSPECIFIED;
    }
}
