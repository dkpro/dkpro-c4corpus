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

import com.ibm.icu.text.CharsetMatch;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.CharsetDetector;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;

/**
 * Charset detector wrapper for {@link com.ibm.icu.text.CharsetDetector}.
 *
 * @author Ivan Habernal
 */
public class ICUCharsetDetectorWrapper
        implements CharsetDetector
{
    /**
     * 8k bytes is sufficient for determining the charset, as it's also used in the wrapped
     * charset detector.
     */
    private static final int SUFFICIENT_BYTE_ARRAY_SIZE = 8000;

    private static final Charset FALLBACK_CHARSET = Charset.forName("utf-8");

    private final com.ibm.icu.text.CharsetDetector charsetDetector = new com.ibm.icu.text.CharsetDetector();

    @Override
    public Charset detectCharset(byte[] bytes)
    {
        return detectCharset(bytes, null);
    }

    @Override
    public Charset detectCharset(byte[] bytes, String declaredCharset)
    {
        // prepare fallback first
        Charset result = FALLBACK_CHARSET;

        // truncate to 8k bytes max
        byte[] truncatedBytes = Arrays.copyOf(bytes, SUFFICIENT_BYTE_ARRAY_SIZE);

        charsetDetector.setText(truncatedBytes);

        if (declaredCharset != null) {
            charsetDetector.setDeclaredEncoding(declaredCharset);
        }

        CharsetMatch charsetMatch = charsetDetector.detect();

        if (charsetMatch != null) {
            try {
                result = Charset.forName(charsetMatch.getName());
            }
            catch (UnsupportedCharsetException ex) {
                // fallback to default
            }
        }

        return result;
    }
}
