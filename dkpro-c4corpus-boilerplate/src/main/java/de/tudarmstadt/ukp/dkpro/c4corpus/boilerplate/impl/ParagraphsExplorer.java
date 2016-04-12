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

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extract a list of paragraphs from html page. Paragraphs here means blocks of
 * the document that might be a boilerplate or not.
 * <br>
 * Based on https://github.com/duongphuhiep/justext/ by Duong Phu-Hiep
 *
 * @author Duong Phu-Hiep
 * @author Omnia Zayed
 */
public class ParagraphsExplorer
        implements NodeVisitor
{

    private static final Pattern HEADING_PATTERN = Pattern.compile("h[1-6]");
    private static final Set<String> PARAGRAPH_TAGS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(new String[] { "blockquote", "caption", "center", "col", "colgroup", "dd",
                    "div", "dl", "dt", "fieldset", "form", "legend", "optgroup", "option", "p", "pre", "table", "td",
                    "textarea", "tfoot", "th", "thead", "tr", "ul", "li", "h1", "h2", "h3", "h4", "h5", "h6" })));
    private final LinkedList<Paragraph> paragraphs;
    private Paragraph currentParagraph = null;
    private boolean lastBR = false;
    private boolean inHeading = false;
    private boolean isLink = false;
    private int headingDepth = 0;

    public enum AncestorState
    {
        INNERTEXT_ONLY, BLOCKLEVEL, UNKNOW
    }

    public ParagraphsExplorer()
    {
        this.paragraphs = new LinkedList<>();
    }

    @Override
    public void head(Node node, int depth)
    {
        if (!inHeading && node instanceof Element) {
            inHeading = HEADING_PATTERN.matcher(((Element) node).tagName()).matches();
            if (inHeading) {
                headingDepth = depth;
            }
        }
        String name = node.nodeName().toLowerCase();
        if (PARAGRAPH_TAGS.contains(name) || (lastBR && "br".equals(name))) {
            startNewParagraph(node);
        } else {
            lastBR = "br".equals(name);
            isLink = "a".equals(name);
            if (currentParagraph != null) {
                appendToLastParagraph(node);
            }
        }
    }

    @Override
    public void tail(Node node, int depth)
    {
        if (depth == headingDepth) {
            // Headings can't be nested
            inHeading = false;
        }
        if (PARAGRAPH_TAGS.contains(node.nodeName())) {
            startNewParagraph(node);
        }
        if ("a".equals(node.nodeName())) {
            isLink = false;
        }
    }

    /**
     * Get the paragraphs after visiting the document
     *
     * @return paragraphs
     */
    public LinkedList<Paragraph> getParagraphs()
    {
        return paragraphs;
    }


    private void startNewParagraph(Node node)
    {
        if (currentParagraph != null && currentParagraph.getRawText().length() > 0) {
            paragraphs.add(currentParagraph);
        }
        currentParagraph = new Paragraph(node, inHeading);
    }

    private void appendToLastParagraph(Node node)
    {
        if (node instanceof TextNode) {
            //TextNode.text() can be used if we don't want to preserve whitespace
            String text = ((TextNode) node).getWholeText();
            // TODO: Do we want spaces between <span>, <sup>, <sub>, etc elements?
            currentParagraph.setRawText(currentParagraph.getRawText() + " " + text);
            if (isLink) {
                currentParagraph.charsCountInLinks += text.length();
            }
        }
    }

}
