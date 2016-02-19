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

import com.google.common.collect.Iterables;
import de.tudarmstadt.ukp.dkpro.c4corpus.deduplication.DeDuplication;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;

/**
 * Duplicates and Near duplicates removal using SimHash and Hamming distance.
 * Part of this implementation is following the steps & theory described here:
 * https://moz.com/devblog/near-duplicate-detection/
 *
 * @author Omnia Zayed
 * @author Ivan Habernal
 */
@Deprecated // remove later, core functionality moved to ParallelDocumentDeDuplication
public class DocumentDeDuplication
        implements DeDuplication
{

    public long getSimHash(String text)
    {
        Set<String> shingles = SimHashUtils.createCharGramsShingles(text);
        Set<Integer> hashPhrases = SimHashUtils.hash(shingles);
        return SimHashUtils.simHash(hashPhrases);
    }

    /**
     * This function process a file from the MR 1st task. the file contains
     * lists of documents which are candidates for "similarity checking" example
     * is [id-doc1, length-doc1, simHash-doc1; id-doc2, length-doc2,
     * simHash-doc2,...] ... The goal is to convert this file into a set (to
     * remove redundancies) of lists (each line is convereted to a list of
     * documents)
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static Set<List<String>> preprocessFileOfCandidates(InputStream inputStream)
            throws IOException
    {
        //TODO: can change the list of string to a list of DocumentInfo Data structure 
        Set<List<String>> similarCandidatesLists = new HashSet<List<String>>();

        List<String> lines = IOUtils.readLines(inputStream, "utf-8");
        //have to convert it to set to remove redundances
        Set<String> similarCandidatesSets = new HashSet<String>(lines);

        for (String candidatesAsString : similarCandidatesSets) {
            String[] documents = candidatesAsString.split(";");
            similarCandidatesLists.add(Arrays.asList(documents));
        }
        return similarCandidatesLists;
    }

    /**
     * create clusters of similar documents based on the hamming distance
     * threshold.
     *
     * @param similarCandidatesLists
     * @return
     */
    public static Set<TreeSet<Document>> createClusters(Set<List<String>> similarCandidatesLists)
    {
        Set<TreeSet<Document>> candidatesClusters = new HashSet<TreeSet<Document>>();
        for (List<String> similarCandidates : similarCandidatesLists) {
            Set<TreeSet<Document>> candidateCluster = createCandidateCluster(similarCandidates);

            candidatesClusters.addAll(candidateCluster);
        }
        return candidatesClusters;
    }

    /**
     * each set of similar candidates is converted to list of tuples based on
     * the hamming distance threshold
     *
     * @param similarCandidates
     * @return
     */
    public static Set<TreeSet<Document>> createCandidateCluster(List<String> similarCandidates)
    {
        Set<TreeSet<Document>> candidatesClusters = new HashSet<TreeSet<Document>>();

        for (int i = 0; i < similarCandidates.size() - 1; i++) {

            //process the head doc
            Document headDoc = new Document();
            headDoc.createDocument(similarCandidates.get(i));
            long headDocSimHash = headDoc.getDocSimHash();
            //other candidates
            for (int j = i + 1; j < similarCandidates.size(); j++) {
                Document similarDoc = new Document();
                similarDoc.createDocument(similarCandidates.get(j));
                long similarDocSimHash = similarDoc.getDocSimHash();

                //calc the hamming distance
                int hammingDist = SimHashUtils.diffOfBits(headDocSimHash, similarDocSimHash);
                //if the hamming distance is <=3
                if (hammingDist <= SimHashUtils.HAMMING_DISTANCE_THRESHOLD) {
                    //save the doc in one cluster
                    //the Document datastructure must implement a compare method
                    //in order to be able to add the document iinto the TreeSet
                    TreeSet<Document> cluster = new TreeSet<Document>();
                    cluster.add(headDoc);
                    cluster.add(similarDoc);
                    if (cluster.size() > 1) {
                        candidatesClusters.add(cluster);
                    }
                }
            }
        }

        return candidatesClusters;
    }

    public static boolean documentPairExistsInACluster(Document docR, Document docJ,
            Set<TreeSet<Document>> clustersOfSimilarDocuments)
    {
        boolean pairFound = false;
        for (TreeSet<Document> t : clustersOfSimilarDocuments) {
            if (t.contains(docR) && t.contains(docJ)) {
                //check if docJ is in the same cluster with docR
                //break looping over the clusters once docJ is found with docR in the same cluster               
                pairFound = true;
                break;
            }
        }
        return pairFound;
    }

    /**
     * choose the documents that will be deleted from each cluster, based on the
     * document length.
     *
     * @param clustersOfSimilarDocuments
     * @return
     */
    public static Set<Document> getDocumentsToBeDeleted(
            Set<TreeSet<Document>> clustersOfSimilarDocuments)
    {

        //keep the longest document
        // a set to keep the final unique docs
        Set<Document> results = new HashSet<Document>();
        //to be deleted documents
        Set<Document> docsToBeDeleted = new HashSet<Document>();
        //for each Ci belongs to CS
        for (TreeSet<Document> cluster : clustersOfSimilarDocuments) {
            // create descending iterator
            Iterator<Document> docInTheClusterItr = cluster.descendingIterator();
            //for each Dj in Ci
            while (docInTheClusterItr.hasNext()) {
                //longest doc
                Document docJ = docInTheClusterItr.next();
                boolean addToFinalResultsOfUniqueDocs = true;
                if (!results.isEmpty()) {
                    for (Document docR : results) {
                        if (!docR.equals(docJ)) {
                            addToFinalResultsOfUniqueDocs = !documentPairExistsInACluster(docR,
                                    docJ, clustersOfSimilarDocuments);
                            //break looping over the results once docJ is found with docR in the same cluster
                            if (!addToFinalResultsOfUniqueDocs) {
                                docsToBeDeleted.add(docJ);
                                break;
                            }
                        }
                        else {
                            //if docJ is already in result (also no need to check the rest of documents in the cluster)
                            break;
                        }
                    }
                }
                if (addToFinalResultsOfUniqueDocs) {
                    results.add(docJ);
                    //add the rest of documents in the cluster to the deleted documents
                    cluster.remove(docJ);
                    docsToBeDeleted.addAll(cluster);
                    //break the cluter loop
                    break;
                }
            }
        }
        return docsToBeDeleted;
    }

    /**
     * Stores the ids of warc records to be deleted in a text file.
     *
     * @throws IOException
     */
    public static void storeIDsOfDeletedRecords(Set<Document> recordsToDelete,
            OutputStream outputStream)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputStream, "utf-8"));
        for (Document d : recordsToDelete) {
            pw.println(d.getDocID());
        }
        IOUtils.closeQuietly(pw);
    }

    // not used
    @Deprecated
    public static void runDeDuplication(InputStream candidatesFileName, OutputStream outputStream)
            throws IOException
    {

        //preprocess the candidantes file for redundancy
        System.out.println("Preprocess Candidates File");
        Set<List<String>> similarCandidatesLists = preprocessFileOfCandidates(candidatesFileName);

        //creating clusters of near duplicate
        System.out.println("create clusters");
        Set<TreeSet<Document>> clusters = createClusters(similarCandidatesLists);
        System.out.println("number of similar pairs: " + clusters.size());
        //        writeOutputToConsole(clusters);

        //get the documents to be deleted 
        System.out.println("get docs to be deleted");
        Set<Document> toBeDeleted = getDocumentsToBeDeleted(clusters);

        //store the files
        System.out.println("store");
        storeIDsOfDeletedRecords(toBeDeleted, outputStream);
    }

    /**
     * partitioning means to split the result array in which we keep the
     * documents toBeDeleted in order to optimize the greedy algorithm
     *
     * @param candidatesFileName
     * @param outputStream
     * @throws IOException
     */
    // not used
    @Deprecated
    public void runDeDuplicationWithPartitioning(InputStream candidatesFileName,
            OutputStream outputStream)
            throws IOException
    {

        //preprocess the candidantes file for redundancy
        //optimized using pig script
        System.out.println("Preprocess Candidates File");
        Set<List<String>> similarCandidatesLists = preprocessFileOfCandidates(candidatesFileName);

        //creating clusters of near duplicated
        //optimized using MR job to create the tuples
        System.out.println("create clusters");
        Set<TreeSet<Document>> clusters = createClusters(similarCandidatesLists);
        System.out.println("number of similar pairs: " + clusters.size());
        //        writeOutputToConsole(clusters);

        //get the documents to be deleted 
        Set<Document> toBeDeleted = new HashSet<Document>();
        if (clusters.size() > SimHashUtils.CLUSTER_PARTITION_SIZE) {
            //split and return a list of clusters
            //for each get doc to be deleted & store
            System.out.println("partitioning");
            toBeDeleted = splitClusters(clusters, SimHashUtils.CLUSTER_PARTITION_SIZE, toBeDeleted);
        }
        else {
            System.out.println("get docs to be deleted");
            toBeDeleted = getDocumentsToBeDeleted(clusters);
        }
        //store the files
        System.out.println("store");
        storeIDsOfDeletedRecords(toBeDeleted, outputStream);
    }

    /**
     * splits a set of clusters pairs if it is greater than certain threshold to
     * lesser the computing time (Not tested if it will produce redundancies or
     * not)
     *
     * @param clusters
     * @param partionSize
     * @param toBeDeleted
     * @return
     * @throws IOException
     */
    public static Set<Document> splitClusters(Set<TreeSet<Document>> clusters, int partionSize,
            Set<Document> toBeDeleted)
            throws IOException
    {

        for (List<TreeSet<Document>> partitionAsList : Iterables.partition(clusters, partionSize)) {
            Set<TreeSet<Document>> partition = new HashSet<TreeSet<Document>>(partitionAsList);
            System.out.println("get docs to be deleted");
            toBeDeleted = getDocumentsToBeDeleted(partition);
        }
        return toBeDeleted;
    }

    /**
     * to be deleted (for debugging purpose)
     *
     * @param clusters
     */
    // not used
    @Deprecated
    public static void writeOutputToConsole(Set<TreeSet<Document>> clusters)
    {
        for (TreeSet<Document> cluster : clusters) {
            Iterator<Document> itr = cluster.iterator();
            System.out.print("[");
            while (itr.hasNext()) {
                Document d = itr.next();
                System.out.print(d.getDocID() + ";" + d.getDocLength() + ";" + d.getDocSimHash()
                        + " ,");
            }
            System.out.print("]" + "\n");
        }
    }

    /**
     * convert a file to a set of documents tuples (given that each line
     * contains a tuple of documents information)
     *
     * @param inputStreamFile
     * @return
     * @throws java.io.IOException
     */
    public static Set<TreeSet<Document>> fileToClusters(InputStream inputStreamFile)
            throws IOException
    {
        Set<TreeSet<Document>> clusters = new HashSet<TreeSet<Document>>();
        List<String> clustersAsStrings = IOUtils.readLines(inputStreamFile, "utf-8");
        for (String clusterString : clustersAsStrings) {
            TreeSet<Document> cluster = new TreeSet<Document>();
            for (String docAsString : clusterString.split(",")) {
                Document document = new Document();
                document.createDocument(docAsString);
                cluster.add(document);
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    public static void selectIDsToDelete(InputStream candidatesFileName, OutputStream outputStream)
            throws IOException
    {
        Set<TreeSet<Document>> clusters = fileToClusters(candidatesFileName);
        //get the documents to be deleted 
        Set<Document> toBeDeleted = new HashSet<Document>();
        if (clusters.size() > SimHashUtils.CLUSTER_PARTITION_SIZE) {
            //split and return a list of clusters
            //for each get doc to be deleted & store
            System.out.println("partitioning");
            toBeDeleted = splitClusters(clusters, SimHashUtils.CLUSTER_PARTITION_SIZE, toBeDeleted);
        }
        else {
            System.out.println("get docs to be deleted");
            toBeDeleted = getDocumentsToBeDeleted(clusters);
        }
        //store the files
        System.out.println("store");
        storeIDsOfDeletedRecords(toBeDeleted, outputStream);
    }

    public static void main(String args[])
            throws IOException
    {

        File dir = new File(args[0]);
        String outputDir = args[1] + "/";
        File[] candidatesFiles = dir.listFiles();
        for (File f : candidatesFiles) {
            DocumentDeDuplication.selectIDsToDelete(new FileInputStream(f),
                    new FileOutputStream(outputDir + f.getName() + "ToDelete"));
        }
    }

}
