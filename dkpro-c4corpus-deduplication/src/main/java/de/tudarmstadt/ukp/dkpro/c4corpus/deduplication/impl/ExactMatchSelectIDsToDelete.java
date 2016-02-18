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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

/**
 * Not used anymore,can be deleted, substituted by MR job. This class contains
 * local methods that select the ids to be deleted from a text file that
 * contains lists of exact matches ids
 *
 * @author Omnia Zayed
 */
@Deprecated
public class ExactMatchSelectIDsToDelete {

    public void getIDsToDelete(InputStream inputStream, OutputStream outputStream)
            throws IOException {

        //the input file will be very huge so it need to be split
        LineIterator iterator = IOUtils.lineIterator(inputStream, "utf-8");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputStream, "utf-8"));
        try {
            while (iterator.hasNext()) {
                String line = iterator.nextLine();
                line = line.replaceAll("[\\[\\]]", "").trim();
                String[] docsIDs = line.split(",");
                //skip first ID
                for (int i = 1; i < docsIDs.length; i++) {
                    pw.println(docsIDs[i].trim());
                }
            }

        } finally {
            iterator.close();
            IOUtils.closeQuietly(pw);
        }
    }

    public static void main(String args[]) throws FileNotFoundException, IOException {
        ExactMatchSelectIDsToDelete exactMatchSelection = new ExactMatchSelectIDsToDelete();

        exactMatchSelection.getIDsToDelete(new FileInputStream(args[0]),
                new FileOutputStream(args[1]));
    }

}
