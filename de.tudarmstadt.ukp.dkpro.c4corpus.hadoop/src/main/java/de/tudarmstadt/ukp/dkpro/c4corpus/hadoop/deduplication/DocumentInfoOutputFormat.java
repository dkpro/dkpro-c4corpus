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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * An output writer to write the DocumentInfo data structure to a machine readable file
 * TODO add description of the output format (example)
 *
 * @author Omnia Zayed
 */
public class DocumentInfoOutputFormat
        extends TextOutputFormat<NullWritable, List<DocumentInfo>>
{

    @Override
    public RecordWriter<NullWritable, List<DocumentInfo>> getRecordWriter(TaskAttemptContext job)
            throws IOException
    {

        //get the current path
        Configuration conf = job.getConfiguration();
        String extension = ".txt";
        //create the full path with the output directory plus our filename
        Path file = getDefaultWorkFile(job, extension);
        //create the file in the file system
        FileSystem fs = file.getFileSystem(conf);
        FSDataOutputStream fileOut = fs.create(file, false);

        //create our record writer with the new file
        return new DocumentInfoRecordWriter(fileOut);
    }

    protected class DocumentInfoRecordWriter
            extends RecordWriter<NullWritable, List<DocumentInfo>>
    {

        private DataOutputStream outStream;

        public DocumentInfoRecordWriter(DataOutputStream out)
        {
            this.outStream = out;
        }

        @Override
        public synchronized void write(NullWritable key, List<DocumentInfo> values)
                throws IOException
        {
            outStream.writeBytes("[");

            //loop through all values associated with the key and write them with semi colon between
            for (int i = 0; i < values.size(); i++) {
                DocumentInfo value = values.get(i);
                if (i > 0) {
                    outStream.writeBytes(",");
                }
                outStream.writeBytes(String.valueOf(value.getDocID()) + ";");
                outStream.writeBytes(String.valueOf(value.getDocLength()) + ";");
                outStream.writeBytes(String.valueOf(value.getDocSimHash())+ ";");
                outStream.writeBytes(String.valueOf(value.getDocLang()));
            }
            outStream.writeBytes("]\n");
        }

        @Override
        public synchronized void close(TaskAttemptContext arg0)
                throws IOException, InterruptedException
        {
            outStream.close();
        }
    }

}
