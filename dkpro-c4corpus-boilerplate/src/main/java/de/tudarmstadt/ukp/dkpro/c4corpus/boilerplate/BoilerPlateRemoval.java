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

package de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate;

import java.io.IOException;
import java.util.Locale;

/**
 * @author Ivan Habernal
 */
public interface BoilerPlateRemoval
{
    /**
     * Removes boiler plate from the html and outputs only plain text; html blocks (such as
     * paragraphs and headers) are separated by a line break
     *
     * @param html   html
     * @param locale Locale (language) of the html page, if known in advance. If {@code locale} is
     *               {@code null}, the implementing class should take care of language identification
     * @return plain text string
     * @throws IOException exception
     */
    String getPlainText(String html, Locale locale)
            throws IOException;

    /**
     * Removes boiler plate from the html and returns a minimal html page; non-removed html blocks
     * will keep their original tags, such as {@code p, h1, h2, pre, li, ...}
     * Minimal html header is also produced
     *
     * @param html   html
     * @param locale Locale (language) of the html page, if known in advance. If {@code locale} is
     *               {@code null}, the implementing class should take care of language identification
     * @return html string
     * @throws IOException exception
     */
    String getMinimalHtml(String html, Locale locale)
            throws IOException;
}
