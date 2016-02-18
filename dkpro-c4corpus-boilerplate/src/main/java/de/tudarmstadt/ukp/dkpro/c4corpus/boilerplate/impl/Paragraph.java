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

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.LinkedList;
import java.util.Set;

/**
 * Data structure representing one block of text in HTML
 *
 * @author Omnia Zayed
 */
public class Paragraph
        extends LinkedList<Node>
{

    //    private ArrayList<String> textNodes;
    int charsCountInLinks = 0;
    private String classType = "";
    private String contextFreeClass = "";
    private String tagName = "";
    private String rawText = "";

    public Paragraph(Node firstNode)
    {
        add(firstNode);
    }

    public void initRawInfo()
    {
        StringBuilder sb = new StringBuilder();
        for (Node n : this) {
            //            NodeHelper.cleanEmptyElements(n);
            if (n instanceof TextNode) {
                this.setTagName(getPath(n));
                String nodeRawText = ((TextNode) n).text();
                sb.append(Utils.normalizeBreaks(nodeRawText).trim());

                if (NodeHelper.isLink(n)) {
                    charsCountInLinks += nodeRawText.length();
                }
            }
        }

        rawText = sb.toString();
    }

    public int getLinksLength()
    {
        return this.charsCountInLinks;
    }

    public String getClassType()
    {
        return this.classType;
    }

    public void setClassType(String classType)
    {
        this.classType = classType;
    }

    public String getContextFreeClass()
    {
        return this.contextFreeClass;
    }

    public void setContextFreeClass(String contextFreeClass)
    {
        this.contextFreeClass = contextFreeClass;
    }

    public String getTagName()
    {
        return this.tagName;
    }

    public String getPath(Node n)
    {
        String nodePath = "";
        while (n != null) {
            if (n instanceof TextNode) {
                n = n.parent();
            }
            if (NodeHelper.isInnerText(n)) {
                n = n.parent();
            }
            String parentNodeName = n.nodeName();
            nodePath = parentNodeName + "." + nodePath;

            if (!parentNodeName.equalsIgnoreCase("html")) {
                n = n.parent();
            }
            else {
                break;
            }
        }

        return nodePath;
    }

    public void setTagName(String name)
    {
        this.tagName = name;
    }

    public boolean isHeading()
    {
        return this.getTagName().matches(".*\\.h\\d\\.");
    }

    public boolean isBoilerplate()
    {
        return !this.getClassType().equalsIgnoreCase("good");
    }

    public String getRawText()
    {

        return Utils.normalizeBreaks(rawText.trim());
    }

    public void setRawText(String rawText)
    {
        this.rawText = Utils.normalizeBreaks(rawText.trim());

    }

    public int getWordsCount()
    {
        return this.getRawText().split("\\s+").length;
    }

    // TODO unused
    //    public boolean containsText() {
    //        return !this.textNodes.isEmpty();
    //    }

    public int stopwordsCount(Set<String> stopwords)
    {
        int count = 0;

        for (String word : this.getRawText().split("\\s+")) {
            if (stopwords.contains(word.toLowerCase())) {
                count += 1;
            }
        }
        return count;
    }

    public float stopwords_density(Set<String> stopwords)
    {
        int wordsCount = this.getWordsCount();
        if (wordsCount == 0) {
            return 0;
        }

        return this.stopwordsCount(stopwords) / (float) wordsCount;
    }

    /**
     * links density means the number of characters of the sentence defining the link
     * divide by the length of the whole paragraph.
     * e.g: hi <a ...>omnia</a> this is an example
     * link density = 5 (length of omnia) / 26 (paragraph length)
     *
     * @return
     */
    public float calcLinksDensity()
    {
        int textLength = this.getRawText().length();
        if (textLength == 0) {
            return 0;
        }

        return this.getLinksLength() / (float) textLength;
    }

}
