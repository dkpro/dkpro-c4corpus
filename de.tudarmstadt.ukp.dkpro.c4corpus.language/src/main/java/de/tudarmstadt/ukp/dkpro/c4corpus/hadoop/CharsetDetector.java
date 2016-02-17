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

import java.nio.charset.Charset;

/**
 * Detects the correct (or most probable) charset of the given byte stream (text).
 *
 * @author Ivan Habernal
 */
public interface CharsetDetector
{
    /**
     * Returns the detected charset. If no charset is detected, falls back to UTF-8
     *
     * @param bytes byte array
     * @return detected charset, never null
     */
    Charset detectCharset(byte[] bytes);

    /**
     * Returns the detected charset. If no charset is detected, falls back UTF-8. Declared charset
     * can help the algorithm to make the decision.
     *
     * @param bytes           byte array
     * @param declaredCharset declared charset
     * @return detected charset, never null
     */
    Charset detectCharset(byte[] bytes, String declaredCharset);
}
