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

import de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.BoilerPlateRemoval;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Re-implementing the jusText python boilerplate removal algorithm (Pomikalek,
 * 2011)
 * <br>
 * References:
 * <br>
 * Pomikalek, J. (2011). Removing boilerplate and duplicate content from web corpora.
 * Ph.D. thesis, Masaryk university, Faculty of informatics, Brno, Czech Republic.
 *
 * @author Omnia Zayed
 */
public class JusTextBoilerplateRemoval
        implements BoilerPlateRemoval
{

    static final double MAX_LINK_DENSITY_DEFAULT = 0.20;
    static final double LENGTH_LOW_DEFAULT = 70;
    static final double LENGTH_HIGH_DEFAULT = 200;
    static final double STOPWORDS_LOW_DEFAULT = 0.30;
    static final double STOPWORDS_HIGH_DEFAULT = 0.32;

    // Short and near-good headings within MAX_HEADING_DISTANCE characters before
    // a good paragraph are classified as good unless --no-headings is specified.
    static final int MAX_HEADING_DISTANCE_DEFAULT = 200;

    //to optimize the time complexity of getNeighbour method
    Pair prevNeighbourCache;
    Pair nextNeighbourCache;

    //To optimize getting next neighbours (when doing re-classification of short & near-good)
    static final int LOOP_THRESHOLD_OF_NEIGHBOURS = 10;
    // for storing stopwords (key = lang, value = stopword set); new languages are added on-demand
    Map<Locale, Set<String>> lazyStopwordMap = new HashMap<>();

    /**
     * covert html to a jsoup document
     */
    private Document convertHtmlToDoc(String html)
    {
        //remove comments
        html = html.replaceAll("<!--(.*?)-->", "");
        Document document;
        try {
            document = Jsoup.parse(html);
        }
        catch (Throwable ex) {
            System.err.println(
                    "Exception raised when parsing HTML with JSoup; returning empty document");
            System.err.println("Stack trace:");
            ex.printStackTrace(System.err);
            System.err.println("----- Original HTML begin -----");
            System.err.println(html);
            System.err.println("----- Original HTML end -----");

            // create empty dummy document
            document = new Document("www.example.com");
        }

        return document;
    }

    /**
     * remove unwanted parts from a jsoup doc
     */
    private Document cleanDom(Document jsoupDoc)
    {
        String[] tagsToRemove = { "head", "script", ".hidden", "embedded" };

        for (String tag : tagsToRemove) {
            Elements selectedTags = jsoupDoc.select(tag);
            for (Element element : selectedTags) {
                element.remove();
            }
        }

        return jsoupDoc;
    }

    /**
     * Initialize the Paragraph explorer class in order to convert a document to
     * a list of blocks (paragraphs)
     */
    private LinkedList<Paragraph> makeParagraphs(Node node)
    {
        ParagraphsExplorer pe = new ParagraphsExplorer();
        node.traverse(pe); //begin the traversal of the doc
        return pe.getParagraphs();
    }

    /**
     * Context-free paragraph classification. assigns each block (paragraph) to
     * one of four classes: bad – boilerplate blocks good – main content blocks
     * short – too short to make a reliable decision about the class near-good –
     * somewhere in-between short and good
     */
    private void classifyContextFree(List<Paragraph> paragraphs, Set<String> stoplist,
            double lengthLow, double lengthHigh, double stopwordsLow,
            double stopwordsHigh, double maxLinkDensity)
    {

        Set<String> stopListLower = new HashSet<>();
        for (String word : stoplist) {
            stopListLower.add(word.toLowerCase().trim());
        }
        for (Paragraph paragraph : paragraphs) {
            int length = paragraph.getRawText().length();
            float stopWordDensity = paragraph.stopwords_density(stopListLower);
            double link_density = paragraph.calcLinksDensity();

            if (link_density > maxLinkDensity) {
                paragraph.setContextFreeClass("bad");
            }
            else if (paragraph.getRawText().contains("\\xa9") || paragraph.getRawText()
                    .contains("&copy")) {
                paragraph.setContextFreeClass("bad");
            }
            else if (length < lengthLow) {
                if (paragraph.getLinksLength() > 0 || length == 0) {
                    paragraph.setContextFreeClass("bad");
                }
                else {
                    paragraph.setContextFreeClass("short");
                }
            }
            else if (stopWordDensity >= stopwordsHigh) {
                if (length > lengthHigh) {
                    paragraph.setContextFreeClass("good");
                }
                else {
                    paragraph.setContextFreeClass("neargood");
                }
            }
            else if (stopWordDensity >= stopwordsLow) {
                paragraph.setContextFreeClass("neargood");
            }
            else {
                paragraph.setContextFreeClass("bad");
            }
        }
    }

    /**
     * Optimization of the original getNeighbour method to reduce the time
     * complexity. used to save the nearest previous neighbour.
     */
    private String getPrevNeighbourOptimized(int i, List<Paragraph> paragraphs,
            boolean ignoreNearGood, int inc, int boundary)
    {
        while (i + inc != boundary) {
            i += inc;
            String c = paragraphs.get(i).getClassType();
            if (c.equalsIgnoreCase("good") || c.equalsIgnoreCase("bad")) {
                prevNeighbourCache = new Pair(i, c);
                return c;
            }
            if (c.equalsIgnoreCase("neargood") && !ignoreNearGood) {
                return c;
            }
            if (prevNeighbourCache != null
                    && i > prevNeighbourCache.getID() && c.equalsIgnoreCase("short")) {
                //render the prev class
                return prevNeighbourCache.getClassType();
            }
        }
        return "bad";
    }

    /**
     * Optimization of the original getNeighbour method to reduce the time
     * complexity. used to save the nearest next neighbour.
     */
    private String getNextNeighbourOptimized(int i, List<Paragraph> paragraphs,
            boolean ignoreNeargood, int inc, int boundary)
    {
        int counter = 0; // to avoid infinite loop in case the whole document is short
        while (i + inc != boundary && counter < LOOP_THRESHOLD_OF_NEIGHBOURS) {
            i += inc;
            String c = paragraphs.get(i).getClassType();
            if (c.equalsIgnoreCase("good") || c.equalsIgnoreCase("bad")) {
                //newly visited paragraph
                nextNeighbourCache = new Pair(i, c);
                return c;
            }
            if (c.equalsIgnoreCase("neargood") && !ignoreNeargood) {
                return c;

            }
            if (nextNeighbourCache != null
                    && i < nextNeighbourCache.getID() && c.equalsIgnoreCase("short")) {
                //render the prev class if this paragraph was visited before                 
                return nextNeighbourCache.getClassType();
            }

            //corner case if the whole document is initially short and no bad
            //or good classes at the end of the paragraphs.
            if (nextNeighbourCache == null && i == boundary - 1) {
                nextNeighbourCache = new Pair(i, "bad");
                return nextNeighbourCache.getClassType();
            }
            counter++;
        }
        return "bad";
    }

    /**
     * optimized version to be used only if the class is short and
     * ignoreNeargood is false.
     */
    private String getPrevNeighbourOptimized(int i, List<Paragraph> paragraphs,
            boolean ignoreNeargood)
    {
        return getPrevNeighbourOptimized(i, paragraphs, ignoreNeargood, -1, -1);
    }

    /**
     * optimized version to be used only if the class is short and
     * ignoreNeargood is false.
     */
    private String getNextNeighbourOptimized(int i, List<Paragraph> paragraphs,
            boolean ignoreNeargood)
    {
        return getNextNeighbourOptimized(i, paragraphs, ignoreNeargood, 1, paragraphs.size());
    }

    /**
     * Context-sensitive paragraph classification. Assumes that context free
     * classification of paragraphs has already been called. The purpose is to
     * re classify neargood and short paragraphs according to the classes of the
     * surrounding blocks.
     */
    private void reclassifyContextSensitive(List<Paragraph> paragraphs, int maxHeadingDistance)
    {

        // copy classes 
        for (Paragraph p : paragraphs) {
            p.setClassType(p.getContextFreeClass());
        }

        // re-classify good headings
        for (int i = 0; i < paragraphs.size(); i++) {
            Paragraph paragraph = paragraphs.get(i);
            if (!(paragraph.isHeading() && paragraph.getClassType().equalsIgnoreCase("short"))) {
                continue;
            }
            int j = i + 1;
            int distance = 0;
            while (j < paragraphs.size() && distance <= maxHeadingDistance) {
                if (paragraphs.get(j).getClassType().equalsIgnoreCase("good")) {
                    paragraph.setClassType("neargood");
                    break;
                }
                distance += paragraphs.get(j).getRawText().length();
                j += 1;
            }
        }

        //re-classify short
        //a new data structure is used for storage as we don't want to mess the
        //original classification. It will be used by other parts of the code later.       
        Map<Integer, String> newClasses = new LinkedHashMap<>();

        for (int i = 0; i < paragraphs.size(); i++) {
            if (!paragraphs.get(i).getClassType().equalsIgnoreCase("short")) {
                continue;
            }

            String prevNeighbour = getPrevNeighbourOptimized(i, paragraphs, true); //ignore_neargood
            String nextNeighbour = getNextNeighbourOptimized(i, paragraphs, true); //ignore_neargood

            Set<String> neighbours = new LinkedHashSet<>();
            neighbours.add(prevNeighbour);
            neighbours.add(nextNeighbour);

            if (neighbours.size() == 1 && neighbours.contains("good")) {
                newClasses.put(i, "good");
            }
            else if (neighbours.size() == 1 && neighbours.contains("bad")) {
                newClasses.put(i, "bad");
            } // it must be set(['good', 'bad'])
            else if ((prevNeighbour.equalsIgnoreCase("bad") && getPrevNeighbourOptimized(i,
                    paragraphs,
                    false).equalsIgnoreCase("neargood"))
                    || (nextNeighbour.equalsIgnoreCase("bad") && getNextNeighbourOptimized(i,
                    paragraphs,
                    false).equalsIgnoreCase("neargood"))) {
                newClasses.put(i, "good");
            }
            else {
                newClasses.put(i, "bad");
            }
        }

        //set the final class type with the new classes
        for (Integer i : newClasses.keySet()) {
            paragraphs.get(i).setClassType(newClasses.get(i));
        }

        // revise neargood        
        for (int i = 0; i < paragraphs.size(); i++) {
            Paragraph paragraph = paragraphs.get(i);
            if (!paragraph.getClassType().equalsIgnoreCase("neargood")) {
                continue;
            }
            String prevNeighbour = getPrevNeighbourOptimized(i, paragraphs, true);
            String nextNeighbour = getNextNeighbourOptimized(i, paragraphs, true);
            if (prevNeighbour.equalsIgnoreCase("bad") && nextNeighbour.equalsIgnoreCase("bad")) {
                paragraph.setClassType("bad");
            }
            else {
                paragraph.setClassType("good");
            }
        }

        // re-classify more good headings
        for (int i = 0; i < paragraphs.size(); i++) {
            Paragraph paragraph = paragraphs.get(i);
            if (!(paragraph.isHeading() && paragraph.getClassType().equalsIgnoreCase("bad")
                    && !paragraph.getContextFreeClass().equalsIgnoreCase("bad"))) {
                continue;
            }
            int j = i + 1;
            int distance = 0;
            while (j < paragraphs.size() && distance <= maxHeadingDistance) {
                if (paragraphs.get(j).getClassType().equalsIgnoreCase("good")) {
                    paragraph.setClassType("good");
                    break;
                }
                distance += paragraphs.get(j).getRawText().length();
                j += 1;
            }

        }
    }

    /**
     * Converts an HTML page into a list of classified paragraphs. Each
     * paragraph is represented as instance of class "Paragraph"
     */
    private List<Paragraph> classify(String htmlText, Set<String> stopwordsSet, double lengthLow,
            double lengthHigh, double stopwordsLow,
            double stopwordsHigh, double maxLinkDensity,
            int maxHeadingDistance)
    {

        //language-independent mode
        if (stopwordsSet.isEmpty()) {
            //empty stop list, switch to language-independent mode
            stopwordsHigh = 0;
            stopwordsLow = 0;
        }

        Document jSoupDoc = convertHtmlToDoc(htmlText);
        Document cleanJSoupDoc = cleanDom(jSoupDoc);
        LinkedList<Paragraph> paragraphs = makeParagraphs(cleanJSoupDoc);
        //context-free classification
        classifyContextFree(paragraphs, stopwordsSet, lengthLow, lengthHigh,
                stopwordsLow, stopwordsHigh, maxLinkDensity);
        //context-sensitive classification.
        reclassifyContextSensitive(paragraphs, maxHeadingDistance);

        return paragraphs;
    }

    /**
     * using defaults and allowing language-independent mode
     */
    private List<Paragraph> classify(String htmlText, Locale locale)
            throws IOException
    {

        //activate the language-independent mode if language is set to null
        Set<String> stopwordsSet;
        if (locale != null && !lazyStopwordMap.containsKey(locale)) {
            lazyStopwordMap.put(locale, Utils.loadStopWords(locale));
        }
        if (locale == null) {
            stopwordsSet = new HashSet<>();
        }
        else {
            //            stopwordsSet = Utils.loadStopWords(language);
            stopwordsSet = lazyStopwordMap.get(locale);
        }

        return classify(htmlText, stopwordsSet, JusTextBoilerplateRemoval.LENGTH_LOW_DEFAULT,
                JusTextBoilerplateRemoval.LENGTH_HIGH_DEFAULT,
                JusTextBoilerplateRemoval.STOPWORDS_LOW_DEFAULT,
                JusTextBoilerplateRemoval.STOPWORDS_HIGH_DEFAULT,
                JusTextBoilerplateRemoval.MAX_LINK_DENSITY_DEFAULT,
                JusTextBoilerplateRemoval.MAX_HEADING_DISTANCE_DEFAULT
        );
    }

    @Override
    public String getPlainText(String html, Locale locale)
            throws IOException
    {

        List<Paragraph> paragraphs = classify(html, locale);

        StringBuilder sb = new StringBuilder();
        for (Paragraph p : paragraphs) {
            if (!p.isBoilerplate()) {
                // extract raw text
                String rawText = Utils.normalize(p.getRawText());
                rawText = StringEscapeUtils.unescapeHtml(rawText).trim();

                sb.append(rawText);
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    @Override
    public String getMinimalHtml(String html, Locale locale)
            throws IOException
    {

        List<Paragraph> paragraphs = classify(html, locale);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        for (Paragraph p : paragraphs) {
            if (!p.isBoilerplate()) {
                // get the tag name
                String tag = p.getTagName();

                //edited as sometimes  the tag is empty because it is div or br
                if (tag.trim().isEmpty()) {
                    tag = "p";
                }
                // extract raw text
                String rawText = Utils.normalize(p.getRawText());

                // for non-empty text and non-empty tags, print the output
                if (!tag.trim().isEmpty() && !rawText.isEmpty()) {
                    pw.printf("<%s>%s</%s>%n", tag, rawText, tag);
                }
            }
        }

        IOUtils.closeQuietly(pw);
        return sw.toString();
    }
}
