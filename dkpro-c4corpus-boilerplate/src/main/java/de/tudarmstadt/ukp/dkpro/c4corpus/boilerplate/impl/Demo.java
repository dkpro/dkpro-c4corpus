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

package de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.impl;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/**
 * @author Omnia Zayed
 */
public class Demo
{
    public static void deleteDirectory(String directoryName)
            throws IOException
    {
        try {
            FileUtils.deleteDirectory(new File(directoryName));
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            throw ioe;
        }
    }

    public static void runSingleFile(String htmlFileName, Locale locale)
    {
        JusTextBoilerplateRemoval c = new JusTextBoilerplateRemoval();

        try {

            //html file
            File htmlFile = new File(htmlFileName);
            String html = FileUtils.readFileToString(htmlFile);

            List<Paragraph> paragraphs = c.classify(html, locale);
            System.out.println("length of paragraphs array is: " + paragraphs.size());
            int i = 0;
            for (Paragraph p : paragraphs) {
                System.out.println("new paragraph is here: ");
                System.out.println(i + " => " + p.getTagName());
                System.out.println(p.getClassType());
                System.out.println(p.getRawText());
                System.out.println();
                i++;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void runDir(String inputDirName, Locale locale, String outputDirName)
            throws URISyntaxException, IOException
    {
        JusTextBoilerplateRemoval c = new JusTextBoilerplateRemoval();

       //create an output dir
        File outputDir = new File(outputDirName);
        if (outputDir.exists()) {
            deleteDirectory(outputDirName);
        }
     
        //input dir of html files
        File htmlDir = new File(inputDirName);
        File[] htmlFiles = htmlDir.listFiles();

        System.out.println("Size of files to be processed: " + htmlFiles.length);

        for (File htmlFile : htmlFiles) {
            File output = new File(
                    outputDirName + "/" + htmlFile.getName().replaceAll("\\.html", ".txt"));
            FileUtils.writeStringToFile(output, "\n", "utf-8", true);
            System.out.println("processing: " + htmlFile.getName());
            String html = FileUtils.readFileToString(htmlFile);

            List<Paragraph> paragraphs = c.classify(html, locale);
            for (Paragraph p : paragraphs) {
                if (!p.isBoilerplate()) {
                    FileUtils.writeStringToFile(output,
                            p.getRawText() + "\n", "utf-8", true);
                }
            }
        }
        /*      String[] outputFilesNames = outputDir.list();
                System.out.println("Size of files after processing: "+outputFilesNames.length);
                 Set<String> outputFilesNamesSet = new HashSet<String>(Arrays.asList(outputFilesNames));
                 for(File f : htmlFiles){
                     if(!outputFilesNamesSet.contains(f.getName().replaceAll("\\.html", ".txt"))){
                      System.out.println("Missing:  "+f.getName());
                     }
                 }
        */

    }

    public static void main(String args[])
            throws URISyntaxException, IOException
    {

        /*
        String inputDir = "CleanEvalHTMLTestSubset";
        String lang = "English";
        String outputDir = "Output_Defaults_"+inputDir;
        runDir(RESOURCES_PATH+"/"+inputDir, "English", RESOURCES_PATH+"/"+outputDir);
//        runSingleFile(RESOURCES_PATH+"/583.html","English");
*/

        //        runDir("/tmp/dbs-in", "German", "/tmp/dbs-out");

        JusTextBoilerplateRemoval c = new JusTextBoilerplateRemoval();
//        List<Paragraph> paragraphs = c.classify(
//                FileUtils.readFileToString(new File("locale-140624.html"), "utf-8"),
//                FileUtils.readFileToString(new File("/tmp/377610-automotodrom-nemuze-dostat-dotaci-statu-je-zadluzeny.html"), "utf-8"),
//                FileUtils.readFileToString(new File("/tmp/377606-ceska-pujde-na-slovensku-na-devet-let-do-vezeni-za-pripravu-vrazdy-manzela.html"), "utf-8"),
//                FileUtils.readFileToString(new File("/tmp/index.html"), "utf-8"),
//                new Locale("cs"));
//                Locale.ENGLISH);
//        System.out.println(c.recreateHTML(paragraphs));
        
        System.out.println(c.getMinimalHtml(FileUtils.readFileToString(new File("<urn:uuid:ffaf1d37-b88a-4916-9085-e11bd6a9daba>.txt"), "utf-8"), 
                null));


    }

}
