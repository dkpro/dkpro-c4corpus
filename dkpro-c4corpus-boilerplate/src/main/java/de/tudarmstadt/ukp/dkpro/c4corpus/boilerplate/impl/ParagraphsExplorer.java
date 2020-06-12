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

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.security.InvalidParameterException;
import java.util.LinkedList;
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
    private final LinkedList<Paragraph> paragraphs;
    private Node lastAddedNode = null;
    private boolean inHeading = false;
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
        if (node.childNodeSize() == 0) {
            if (node instanceof TextNode && StringUtil.isBlank(node.outerHtml())) {
                return;
            }
            mergeToResult(node);
            lastAddedNode = node;
        }
    }

    @Override
    public void tail(Node node, int depth)
    {
        if (depth == headingDepth) {
            // Headings can't be nested
            inHeading = false;
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

    private void mergeToResult(Node node)
    {
        //the <br><br> is a paragraph separator 
        if (lastAddedNode != null && node.nodeName().equalsIgnoreCase("br") && lastAddedNode
                .nodeName().equalsIgnoreCase("br")) {
            insertAsNewParagraph(node);
            return;
        }
        if (lastAddedNode == null) {
            insertAsNewParagraph(node);
            return;
        }

        AncestorState ancestorState = getAncestorState(lastAddedNode, node);
        switch (ancestorState) {
        case BLOCKLEVEL:
            insertAsNewParagraph(node);
            return;
        case INNERTEXT_ONLY:
            appendToLastParagraph(node);
        case UNKNOW:
            break;
        default:
            break;
        }
    }

    /**
     * Visit from lastNode and currentNode to the first common ancestor of these
     * 2 nodes, - if all the visited ancestors are
     * INNERTEXT returns
     * {@link ParagraphsExplorer.AncestorState#INNERTEXT_ONLY} - if one of the
     * visited ancestors is
     * isBlockTag(Node) returns
     * {@link ParagraphsExplorer.AncestorState#BLOCKLEVEL} - otherwise returns
     * {@link ParagraphsExplorer.AncestorState#UNKNOW}
     *
     * @param lastNode    last node
     * @param currentNode current node
     * @return state
     */
    public static AncestorState getAncestorState(Node lastNode, Node currentNode)
    {
        if (lastNode == null || currentNode == null) {
            throw new InvalidParameterException();
        }

        Node ancestor = NodeHelper.nearestCommonAncestor(lastNode, currentNode);
        AncestorState as1 = getAncestorStateOfBranch(ancestor, lastNode);
        if (as1 == AncestorState.BLOCKLEVEL) {
            return AncestorState.BLOCKLEVEL;
        }
        AncestorState as2 = getAncestorStateOfBranch(ancestor, currentNode);
        if (as2 == AncestorState.BLOCKLEVEL) {
            return AncestorState.BLOCKLEVEL;
        }
        if (as1 == AncestorState.INNERTEXT_ONLY && as2 == AncestorState.INNERTEXT_ONLY) {
            return AncestorState.INNERTEXT_ONLY;
        }
        return AncestorState.UNKNOW;
    }

    private void insertAsNewParagraph(Node node)
    {
        Paragraph p = new Paragraph(node, inHeading);
        // if (!p.getRawText().isEmpty()) {
        paragraphs.add(p);
        // }
    }

    private void appendToLastParagraph(Node node)
    {
        //        if(!node.nodeName().equalsIgnoreCase("br")){
        if (node instanceof TextNode) {
            Paragraph p = paragraphs.getLast();
            //TextNode.text() can be used if we don't want to preserve whitespace
            String text = ((TextNode) node).getWholeText();
            // FIXME: We don't necessarily want spaces between <span>, <sup>, etc elements.
            p.setRawText(p.getRawText() + " " + text);
            if (NodeHelper.isLink(node)) {
                p.charsCountInLinks += text.length();
            }
            paragraphs.getLast().add(node);
        }
    }


    /**
     * Visit from node to the ancestor - if all the visited ancestors are
     * {@link NodeHelper.TagType#INNER_TEXT} returns
     * {@link ParagraphsExplorer.AncestorState#INNERTEXT_ONLY} - if one of the
     * visited ancestors is {@link NodeHelper#isBlockTag(Node)}
     * returns {@link ParagraphsExplorer.AncestorState#BLOCKLEVEL} - otherwise
     * returns {@link ParagraphsExplorer.AncestorState#UNKNOW}
     */
    private static AncestorState getAncestorStateOfBranch(Node ancestor, Node node)
    {
        if (!NodeHelper.isAncestor(ancestor, node)) {
            throw new InvalidParameterException("ancestor pre-condition violation");
        }
        if (node == ancestor) {
            if (NodeHelper.isBlockTag(node)) {
                return AncestorState.BLOCKLEVEL;
            }
            if (NodeHelper.isInlineTag(node)) {
                return AncestorState.INNERTEXT_ONLY;
            }
            return AncestorState.UNKNOW;
        }
        Node n = node.parent();
        boolean innerTextOnly = true;
        while (n != ancestor && n != null) {
            if (NodeHelper.isBlockTag(n)) {
                return AncestorState.BLOCKLEVEL;
            }
            if (!NodeHelper.isInlineTag(n)) {
                innerTextOnly = false;
            }
            n = n.parent();
        }
        return innerTextOnly ? AncestorState.INNERTEXT_ONLY : AncestorState.UNKNOW;
    }

}
