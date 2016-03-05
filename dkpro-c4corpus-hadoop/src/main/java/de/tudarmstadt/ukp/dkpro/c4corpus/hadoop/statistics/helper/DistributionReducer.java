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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.helper;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * For each text key, writes the distribution of values into text outupt (with graduation: 100)
 */
public class DistributionReducer
        extends Reducer<Text, LongWritable, Text, Text>
{
    @Override
    protected void reduce(Text key, Iterable<LongWritable> values, Context context)
            throws IOException, InterruptedException
    {
        //
        Map<Long, Long> counts = new TreeMap<>();

        for (LongWritable intWritable : values) {
            // bin = 100, 200, 300, etc. kB
            long bin = ((intWritable.get() / 100000) + 1) * 100;

            if (!counts.containsKey(bin)) {
                counts.put(bin, 1L);
            }
            else {
                counts.put(bin, counts.get(bin) + 1);
            }
        }

        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            context.write(key, new Text(entry.getKey() + "\t" + entry.getValue()));
        }
    }
}
