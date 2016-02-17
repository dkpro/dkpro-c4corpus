package de.tudarmstadt.ukp.dkpro.c4corpus.deduplication.impl;

import java.io.IOException;
import java.util.*;

/**
 * Modified version of {@link DocumentDeDuplication} that can be called from a Map/Reduce
 * program
 * <p/>
 * (c) 2016 Ivan Habernal
 */
public class ParallelDocumentDeDuplication
{

    /**
     * Runs the local greedy algorihtm over the input lines containing document tuples and returns
     * the set of IDs found to be near-duplicite
     *
     * @param inputLines input lines
     * @return set of docs
     * @throws IOException IO exception
     */
    public static Set<String> selectIDsToDelete(List<String> inputLines)
            throws IOException
    {
        Set<TreeSet<Document>> clusters = linesToClusters(inputLines);
        //get the documents to be deleted
        Set<Document> toBeDeleted = getDocumentsToBeDeleted(clusters);

        return collectIDsOfDeletedRecords(toBeDeleted);
    }

    /**
     * choose the documents that will be deleted from each cluster, based on the
     * document length.
     *
     * @param clustersOfSimilarDocuments clusters
     * @return set of documents
     */
    protected static Set<Document> getDocumentsToBeDeleted(
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

    protected static boolean documentPairExistsInACluster(Document docR, Document docJ,
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
     * Collects the ids of warc records to be deleted.
     *
     * @throws IOException
     */
    protected static Set<String> collectIDsOfDeletedRecords(Set<Document> recordsToDelete)
            throws IOException
    {
        Set<String> result = new TreeSet<>();
        for (Document d : recordsToDelete) {
            result.add(d.getDocID());
        }
        return result;
    }

    /**
     * Converts a list of lines to a set of documents tuples (given that each line
     * contains a tuple of documents information)
     *
     * @return set of document tuples
     */
    protected static Set<TreeSet<Document>> linesToClusters(List<String> lines)
            throws IOException
    {
        Set<TreeSet<Document>> result = new HashSet<>();

        for (String line : lines) {
            // trim leading and ending brackets ('[' and ']')
            String trimmedLine = line.replaceAll("^\\[", "").replaceAll("\\]$", "");

            TreeSet<Document> cluster = new TreeSet<>();
            for (String docAsString : trimmedLine.split(",")) {
                Document document = new Document();
                document.createDocument(docAsString.trim());
                cluster.add(document);
            }
            result.add(cluster);
        }
        return result;
    }
}
