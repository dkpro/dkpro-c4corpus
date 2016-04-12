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
 * Based on https://github.com/duongphuhiep/justext/ by Duong Phu-Hiep
 *
 * @author Duong Phu-Hiep
 * @author Omnia Zayed
 */
public class Paragraph
        extends LinkedList<Node>
{
    private static final long serialVersionUID = 1L;


    public enum PARAGRAPH_TYPE {UNKNOWN, SHORT, GOOD, NEAR_GOOD, BAD};

    int charsCountInLinks = 0;
    private PARAGRAPH_TYPE classType = PARAGRAPH_TYPE.UNKNOWN;
    private PARAGRAPH_TYPE contextFreeClass = PARAGRAPH_TYPE.UNKNOWN;
    private String tagName = "";
    private String rawText = "";
    private boolean isHeading = false;

    public Paragraph(Node firstNode, boolean heading)
    {
        add(firstNode);
        Node node = firstNode;
        while (NodeHelper.isInnerText(node) || node instanceof TextNode) {
            node = node.parent();
        }
        if (node != null) {
            this.tagName = node.nodeName();
        }
        this.isHeading = heading;
        if (firstNode instanceof TextNode) {
            String nodeRawText = ((TextNode) firstNode).text();
            this.rawText = nodeRawText.trim();

            if (NodeHelper.isLink(firstNode)) {
                charsCountInLinks += nodeRawText.length();
            }
        }
    }


    public int getLinksLength()
    {
        return this.charsCountInLinks;
    }

    public PARAGRAPH_TYPE getClassType()
    {
        return this.classType;
    }

    public void setClassType(PARAGRAPH_TYPE classType)
    {
        this.classType = classType;
    }

    public PARAGRAPH_TYPE getContextFreeClass()
    {
        return this.contextFreeClass;
    }

    public void setContextFreeClass(PARAGRAPH_TYPE contextFreeClass)
    {
        this.contextFreeClass = contextFreeClass;
    }

    public String getTagName()
    {
        return this.tagName;
    }


    public void setTagName(String name)
    {
        this.tagName = name;
    }

    public boolean isHeading()
    {
        return isHeading;
    }

    public boolean isBoilerplate()
    {
        return this.getClassType() != PARAGRAPH_TYPE.GOOD;
    }

    public String getRawText()
    {
        return rawText;
    }

    public void setRawText(String rawText)
    {
        this.rawText = rawText;
    }

    public int getWordsCount()
    {
        return this.getRawText().split("\\s+").length;
    }

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
     * Links density is the number of characters of the sentence defining the link
     * divide by the length of the whole paragraph.
     * e.g: hi {@code <a ...>omnia</a>} this is an example
     * link density = 5 (length of omnia) / 26 (paragraph length)
     *
     * @return Links density
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
