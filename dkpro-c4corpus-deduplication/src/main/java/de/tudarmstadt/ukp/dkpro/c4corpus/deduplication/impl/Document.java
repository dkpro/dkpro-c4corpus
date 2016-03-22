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

/**
 * A data structure to hold the document information
 *
 * @author Omnia ayed
 */
public class Document
        implements Comparable<Document>
{

    private final String docID;
    private final Integer docLength;
    private final Long docSimHash;
    private final String docLang;

    public Document(String documentInfoCommaSep)
    {
        //process the doc
        String[] idLenSimHashOfDoc = documentInfoCommaSep.split(";");

        this.docID = idLenSimHashOfDoc[0].replaceAll("\\[", "").trim();
        this.docLength = Integer.parseInt(idLenSimHashOfDoc[1].trim());
        this.docSimHash = Long.parseLong(idLenSimHashOfDoc[2].trim());
        this.docLang = idLenSimHashOfDoc[3].replaceAll("\\]", "").trim();
    }

    public String getDocID()
    {
        return docID;
    }

    public Integer getDocLength()
    {
        return docLength;
    }

    public Long getDocSimHash()
    {
        return docSimHash;
    }

    public String getDocLang()
    {
        return docLang;
    }

    /*
     * a compare method to be used when adding Document to a TreeSet or similar
     * sorted data structure
     */
    @Override
    public int compareTo(Document d1)
    {

        int cmp = docLength.compareTo(d1.docLength);

        if (cmp == 0) {
            //if the length of two documents is equal so there are two cases
            //either the documents have exact match content or it is just a coincidance
            //so the SimHash must be checked
            //exact match documents will have identical SimHash
            //            return docSimHash.compareTo(d1.getDocSimHash());
            //it is better to sort the ID alphabeticly if two documents have the same length
            return docID.compareTo(d1.docID);
        }

        return cmp;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Document other = (Document) obj;
        return (this.docID == null) ? other.docID == null : this.docID.equals(other.docID);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 53 * hash + (this.docID != null ? this.docID.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString()
    {
        return this.docID + ";" + this.docLength + ";" + this.docSimHash;
    }
}
