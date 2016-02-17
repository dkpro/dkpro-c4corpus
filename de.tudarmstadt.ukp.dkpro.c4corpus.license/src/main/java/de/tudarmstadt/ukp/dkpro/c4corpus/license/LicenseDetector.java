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

/**
 * Interface for classes that detect the license of the given HTML page; i.e. "cc-by-sa",
 * and similar.
 *
 * @author Omnia Zayed
 */
public interface LicenseDetector
{

    /**
     * No license found
     */
    String NO_LICENCE = "none";

    /**
     * Unspecified Creative Commons (e.g. for pages where different parts are under different licenses)
     */
    String CC_UNSPECIFIED = "cc-unspecified";

    /**
     * Returns a licence under which is the given HTML page licenced, such as "cc-by-sa",
     * "cc-by", or "none" if no license can be identified.
     *
     * @param html html page
     * @return identified licence (never null)
     */
    String detectLicence(String html);

}
