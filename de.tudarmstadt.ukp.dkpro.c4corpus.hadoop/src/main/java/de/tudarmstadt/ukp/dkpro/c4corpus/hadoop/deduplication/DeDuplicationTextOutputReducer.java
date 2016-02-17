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
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

/**
 * Documents that have the same bands are grouped together.
 *
 * @author Omnia Zayed
 */
public class DeDuplicationTextOutputReducer
        extends Reducer<Text, DocumentInfo, NullWritable, List<DocumentInfo>> {

    private MultipleOutputs<NullWritable, List<DocumentInfo>> multipleOutputs;

    @Override
    protected void setup(Context context)
            throws IOException, InterruptedException {
        multipleOutputs = new MultipleOutputs<NullWritable, List<DocumentInfo>>(context);
    }

    @Override
    protected void reduce(Text key, Iterable<DocumentInfo> values, Context context)
            throws IOException, InterruptedException {
        List<DocumentInfo> documents = new ArrayList<DocumentInfo>();

        //collect the values of each band#_bitString
        for (DocumentInfo v : values) {
            // we really need the copy here!
            DocumentInfo documentInfo = new DocumentInfo();
            documentInfo.setDocSimHash(new LongWritable(v.getDocSimHash().get()));
            documentInfo.setDocLength(new IntWritable(v.getDocLength().get()));
            documentInfo.setDocID(new Text(v.getDocID().toString()));
            documentInfo.setDocLanguage(new Text(v.getDocLang().toString()));

            documents.add(documentInfo);
        }

        //choose candidates for similarity check
        if (documents.size() >= 2) {
            //sort the list to be able to remove redundancies later
            Collections.sort(documents, new DocIDComparator());
            // set the file name prefix
            String fileName = documents.get(0).getDocLang().toString();
           
            multipleOutputs.write(NullWritable.get(), documents, fileName);
        }
    }

    @Override
    protected void cleanup(Context context)
            throws IOException, InterruptedException {
        multipleOutputs.close();
    }

    /**
     * A comparable that will be used to sort the DocumentInfo data structure.
     * Sorting is done according to id
     */
    public static class DocIDComparator
            implements Comparator<DocumentInfo> {

        @Override
        public int compare(DocumentInfo d1, DocumentInfo d2) {
            return d1.getDocID().compareTo(d2.getDocID());
        }
    }
}
