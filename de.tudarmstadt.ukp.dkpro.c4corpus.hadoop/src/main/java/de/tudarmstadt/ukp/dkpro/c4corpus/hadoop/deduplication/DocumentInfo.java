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
package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.deduplication;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.WritableComparable;

/**
 * A custom Writable implementation for document information.
 *
 * @author Omnia Zayed
 *
 */
public class DocumentInfo
        implements WritableComparable<DocumentInfo> {

    private Text docID = new Text();
    private IntWritable docLength = new IntWritable();
    private LongWritable docSimHash = new LongWritable();
    private Text language = new Text();

    public void createDocumentInfo(String commaSeparatedInfo) {
        String[] idLengSimHashOfDoc = commaSeparatedInfo.split(";");

        String docID = idLengSimHashOfDoc[0].replaceAll("\\[", "").trim();
        this.docID = new Text(docID);

        int docLength = Integer.valueOf(idLengSimHashOfDoc[1].trim());
        this.docLength = new IntWritable(docLength);

        long docSimHash = Long.valueOf(idLengSimHashOfDoc[2].trim());
        this.docSimHash = new LongWritable(docSimHash);

        String lang = idLengSimHashOfDoc[3].replaceAll("\\]", "").trim();
        this.language = new Text(lang);
    }

    public void setDocID(Text docID) {
        //deep copy is a must
        this.docID = new Text(docID);
    }

    public void setDocLength(IntWritable docLength) {
        this.docLength = new IntWritable(docLength.get());
    }

    public void setDocSimHash(LongWritable docSimHash) {
        this.docSimHash = new LongWritable(docSimHash.get());
    }

    public void setDocLanguage(Text lang) {
        this.language = new Text(lang);
    }

    public Text getDocID() {
        return docID;
    }

    public IntWritable getDocLength() {
        return docLength;
    }

    public LongWritable getDocSimHash() {
        return docSimHash;
    }

    public Text getDocLang() {
        return language;
    }

    @Override
    public void write(DataOutput d)
            throws IOException {
        docID.write(d);
        docLength.write(d);
        docSimHash.write(d);
        language.write(d);
    }

    @Override
    public void readFields(DataInput di)
            throws IOException {
        docID.readFields(di);
        docLength.readFields(di);
        docSimHash.readFields(di);
        language.readFields(di);
    }

    @Override
    public int compareTo(DocumentInfo o) {
        int compareValue = this.docID.compareTo(o.getDocID());
        if (compareValue == 0) {
            compareValue = this.docLength.compareTo(o.getDocLength());
            if (compareValue == 0) {
                compareValue = this.docSimHash.compareTo(o.getDocSimHash());
            }
            if (compareValue == 0) {
                compareValue = this.language.compareTo(o.getDocLang());
            }
        }
        return compareValue;
    }

    @Override
    public int hashCode() {
        return Math.abs(docID.hashCode()) * 163 + docSimHash.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DocumentInfo) {
            DocumentInfo di = (DocumentInfo) o;
            return docID.equals(di.getDocID())
                    && docLength.equals(di.getDocLength())
                    && docSimHash.equals(di.getDocSimHash())
                    && language.equals(di.getDocLang());
        }
        return false;
    }

    @Override
    public String toString() {
        String result = this.docID + ";" + this.docLength + ";"
                + this.docSimHash + ";" + this.language;
        return result;
    }
}
