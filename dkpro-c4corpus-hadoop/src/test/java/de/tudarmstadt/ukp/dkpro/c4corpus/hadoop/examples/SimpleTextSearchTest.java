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
package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.examples;

import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * @author Ivan Habernal
 */
public class SimpleTextSearchTest
{

    @Test
    public void testRegexSearch()
            throws Exception
    {
        String s = "This is a text containing a test phrase and some other dummy text with context.";

        // search for exact 'text' with 10 chars context (left, right)
        List<String> list = SimpleTextSearch.TextSearchMapper
                .searchRegex(s, Pattern.compile(".{10}text.{10}"));
        assertEquals("This is a text containin", list.get(0));
        assertEquals("her dummy text with cont", list.get(1));

        // non-continuous phrase
        list = SimpleTextSearch.TextSearchMapper
                .searchRegex(s, Pattern.compile("containing.*phrase"));
        assertEquals("containing a test phrase", list.get(0));
    }
}