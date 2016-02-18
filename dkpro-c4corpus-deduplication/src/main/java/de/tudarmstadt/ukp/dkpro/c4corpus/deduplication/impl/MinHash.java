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
package de.tudarmstadt.ukp.dkpro.c4corpus.deduplication.impl;

import java.util.*;

/**
 * Can be deleted, Not used.
 * https://mymagnadata.wordpress.com/2011/01/04/minhash-java-implementation/
 */
@Deprecated
public class MinHash<T> {

    private int hash[];
    private int numHash;

    /**
     *
     */
    public MinHash(int numHash) {
        this.numHash = numHash;
        hash = new int[numHash];

        Random r = new Random(11);
        for (int i = 0; i < numHash; i++) {
            int a = r.nextInt();
            int b = r.nextInt();
            int c = r.nextInt();
            int x = hash(a * b * c, a, b, c);
            hash[i] = x;
        }
    }

    public double similarity(Set<T> set1, Set<T> set2) {

        int numSets = 2;
        Map<T, boolean[]> bitMap = buildBitMap(set1, set2);

        int[][] minHashValues = initializeHashBuckets(numSets, numHash);

        computeMinHashForSet(set1, 0, minHashValues, bitMap);
        computeMinHashForSet(set2, 1, minHashValues, bitMap);

        return computeSimilarityFromSignatures(minHashValues, numHash);
    }

    /**
     *
     */
    private static int[][] initializeHashBuckets(int numSets, int numHashFunctions) {
        int[][] minHashValues = new int[numSets][numHashFunctions];

        for (int i = 0; i < numSets; i++) {
            for (int j = 0; j < numHashFunctions; j++) {
                minHashValues[i][j] = Integer.MAX_VALUE;
            }
        }
        return minHashValues;
    }

    private static double computeSimilarityFromSignatures(int[][] minHashValues,
            int numHashFunctions) {
        int identicalMinHashes = 0;
        for (int i = 0; i < numHashFunctions; i++) {
            if (minHashValues[0][i] == minHashValues[1][i]) {
                identicalMinHashes++;
            }
        }
        return (1.0 * identicalMinHashes) / numHashFunctions;
    }

    private static int hash(int x, int a, int b, int c) {
        int hashValue = (a * (x >> 4) + b * x + c) & 131071;
        return Math.abs(hashValue);
    }

    private void computeMinHashForSet(Set<T> set, int setIndex, int[][] minHashValues,
            Map<T, boolean[]> bitArray) {

        int index = 0;

        for (T element : bitArray.keySet()) { // for every element in the bit array
            for (int i = 0; i < numHash; i++) { // for every hash
                if (set.contains(element)) { // if the set contains the element
                    int hindex = hash[index]; // get the hash
                    if (hindex < minHashValues[setIndex][index]) {
                        // if current hash is smaller than the existing hash in the slot then replace with the smaller hash value
                        minHashValues[setIndex][i] = hindex;
                    }
                }
            }
            index++;
        }
    }

    public Map<T, boolean[]> buildBitMap(Set<T> set1, Set<T> set2) {

        Map<T, boolean[]> bitArray = new HashMap<T, boolean[]>();

        for (T t : set1) {
            bitArray.put(t, new boolean[]{true, false});
        }

        for (T t : set2) {
            if (bitArray.containsKey(t)) {
                // item is not present in set1
                bitArray.put(t, new boolean[]{true, true});
            } else if (!bitArray.containsKey(t)) {
                // item is not present in set1
                bitArray.put(t, new boolean[]{false, true});
            }
        }

        return bitArray;
    }

    public static void main(String[] args) {
        Set<String> set1 = new HashSet<String>();
        set1.add("FRANCISCO");
        set1.add("MISSION");
        set1.add("SAN");

        Set<String> set2 = new HashSet<String>();
        set2.add("FRANCISCO");
        set2.add("MISSION");
        set2.add("SAN");
        set2.add("USA");

        MinHash<String> minHash = new MinHash<String>(100);
        System.out.println(minHash.similarity(set1, set2));
    }
}
