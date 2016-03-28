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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full;

import org.apache.hadoop.io.Text;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ivan Habernal
 */
public class Phase3Step2DistinctDataJobTest
{

    @Test
    public void testSplit()
            throws Exception
    {
        Text key = new Text("123_456789");

        // hard-split using array copy
        int i = key.find("_", 0);

        Text outputKey = new Text("");
        byte[] bytes = key.getBytes();
        outputKey.append(bytes, i + 1, bytes.length - i - 2);

        String fileName = new String(bytes, 0, i);

        assertEquals("123", fileName);
        assertEquals("456789", outputKey.toString());
    }
}