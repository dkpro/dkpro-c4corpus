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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.statistics;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;

/**
 * Collects the output from {@link LangLicenseStatistics} and creates a single multi-column table
 *
 * @author Ivan Habernal
 */
public class StatisticsTableCreator
{
    public static Table<String, String, Integer> loadTable(InputStream stream)
            throws IOException
    {
        Table<String, String, Integer> result = TreeBasedTable.create();

        LineIterator lineIterator = IOUtils.lineIterator(stream, "utf-8");
        while (lineIterator.hasNext()) {
            String line = lineIterator.next();

            System.out.println(line);

            String[] split = line.split("\t");
            String language = split[0];
            String license = split[1];
            Integer documents = Integer.valueOf(split[2]);
            Integer tokens = Integer.valueOf(split[3]);

            result.put(language, "docs " + license, documents);
            result.put(language, "tokens " + license, tokens);
        }

        return result;
    }

    public static void saveTableToCsv(Table<String, String, Integer> table,
            OutputStream outputStream)
    {
        PrintWriter pw = new PrintWriter(outputStream);
        pw.write(";");
        for (String columnKey : table.columnKeySet()) {
            pw.printf("%s;", columnKey);
        }
        pw.println();

        for (String rowKey : table.rowKeySet()) {
            pw.printf("%s;", rowKey);
            for (String columnKey : table.columnKeySet()) {
                Integer value = table.get(rowKey, columnKey);
                pw.printf("%d;", value != null ? value : 0);
            }
            pw.println();
        }

        IOUtils.closeQuietly(pw);
    }

    public static void main(String[] args)
            throws Exception
    {
        File inFile = new File(args[0]);
        File outFile = new File(args[1]);
        saveTableToCsv(loadTable(new FileInputStream(inFile)), new FileOutputStream(outFile));
    }
}
