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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics.vocabulary;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Ivan Habernal
 */
public class TopNWordsCorrelation
{
    /**
     * Computes Spearman correlation by comparing order of two corpora vocabularies
     *
     * @param goldCorpus  gold corpus
     * @param otherCorpus other corpus
     * @param topN        how many entries from the gold corpus should be taken
     * @throws IOException
     */
    public static void spearmanCorrelation(File goldCorpus, File otherCorpus,
            int topN)
            throws IOException
    {
        LinkedHashMap<String, Integer> gold = loadCorpusToRankedVocabulary(
                new FileInputStream(goldCorpus));
        LinkedHashMap<String, Integer> other = loadCorpusToRankedVocabulary(
                new FileInputStream(otherCorpus));

        double[][] matrix = new double[topN][];

        if (gold.size() < topN) {
            throw new IllegalArgumentException(
                    "topN (" + topN + ") cannot be greater than vocabulary size (" + gold.size()
                            + ")");
        }

        Iterator<Map.Entry<String, Integer>> iterator = gold.entrySet().iterator();
        int counter = 0;
        while (counter < topN) {
            Map.Entry<String, Integer> next = iterator.next();
            String goldWord = next.getKey();
            Integer goldValue = next.getValue();

            // look-up position in other corpus
            Integer otherValue = other.get(goldWord);
            if (otherValue == null) {
                //                System.err.println("Word " + goldWord + " not found in the other corpus");
                otherValue = Integer.MAX_VALUE;
            }

            matrix[counter] = new double[2];
            matrix[counter][0] = goldValue;
            matrix[counter][1] = otherValue;

            counter++;
        }

        RealMatrix realMatrix = new Array2DRowRealMatrix(matrix);

        SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation(realMatrix);
        double pValue = spearmansCorrelation.getRankCorrelation().getCorrelationPValues()
                .getEntry(0, 1);
        double correlation = spearmansCorrelation.getRankCorrelation().getCorrelationMatrix()
                .getEntry(0, 1);

        System.out.println("Gold: " + goldCorpus.getName());
        System.out.println("Other: " + otherCorpus.getName());
        System.out.printf(Locale.ENGLISH, "Top N:\n%d\nCorrelation\n%.3f\np-value\n%.3f\n", topN,
                correlation, pValue);
    }

    public static LinkedHashMap<String, Integer> loadCorpusToRankedVocabulary(InputStream corpus)
            throws IOException
    {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();

        LineIterator lineIterator = IOUtils.lineIterator(corpus, "utf-8");
        int counter = 0;
        while (lineIterator.hasNext()) {
            String line = lineIterator.next();

            String word = line.split("\\s+")[0];

            result.put(word, counter);
            counter++;
        }

        return result;
    }

    public static void main(String[] args)
            throws IOException
    {
        // create tuples from params
        for (int i = 0; i < args.length; i++) {
            for (int j = 0; j < args.length; j++) {
                if (i != j) {
                    for (Integer size : Arrays.asList(100, 1000, 10000)) {
                        spearmanCorrelation(new File(args[i]), new File(args[j]),
                                size);
                    }
                }
            }
        }
    }
}
