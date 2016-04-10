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

import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods for JSoup node handling
 * <br>
 * Based on https://github.com/duongphuhiep/justext/ by Duong Phu-Hiep
 *
 * @author Duong Phu-Hiep
 * @author Omnia Zayed
 * @author Ivan Habernal
 */
public class NodeHelper
{

    /**
     * Returns the nearest common ancestor of node1 and node2
     *
     * @param node1 node 1
     * @param node2 node 2
     * @return nearest common ancestor node
     * @throws IllegalStateException if node1 and node2 has no common ancestor
     *                               to make sure that node1 and node2 should inside the same document
     */
    public static Node nearestCommonAncestor(Node node1, Node node2)
    {
        Node ancestor = node1;
        while (ancestor != null) {
            // FIXME: Inefficient!
            if (isAncestor(ancestor, node2)) {
                return ancestor;
            }
            ancestor = ancestor.parent();
        }
        throw new IllegalStateException("node1 and node2 do not have common ancestor");
    }

    /**
     * Returns true if node1 is ancestor of node2 or node1 == node2
     *
     * @param node1 node 1
     * @param node2 node 2
     * @return boolean value
     */
    public static boolean isAncestor(Node node1, Node node2)
    {
        Node ancestor = node2;

        while (ancestor != null) {
            if (ancestor == node1) {
                return true;
            }
            ancestor = ancestor.parent();
        }

        return false;
    }

    /**
     * Returns true if node has a link ancestor
     *
     * @param node node
     * @return boolean value
     */
    public static boolean isLink(Node node)
    {
        // TODO: This is continually traversing the tree & recomputing stuff
        Node ancestor = node;

        while (ancestor != null) {
            if (isLinkTag(ancestor)) {
                return true;
            }
            ancestor = ancestor.parent();
        }

        return false;
    }

    private enum TagType
    {
        IGNORABLE, INNER_TEXT, BLOCK_LEVEL, BLOCK_LEVEL_CONTENT, BLOCK_LEVEL_TITLE
    }

    public static final Map<String, TagType> TAGS_TYPE = new HashMap<>();

    static {
        TAGS_TYPE.put("style", TagType.IGNORABLE);
        TAGS_TYPE.put("script", TagType.IGNORABLE);
        TAGS_TYPE.put("option", TagType.IGNORABLE);
        TAGS_TYPE.put("noscript", TagType.IGNORABLE);
        TAGS_TYPE.put("embed", TagType.IGNORABLE);
        TAGS_TYPE.put("applet", TagType.IGNORABLE);
        TAGS_TYPE.put("link", TagType.IGNORABLE);
        TAGS_TYPE.put("button", TagType.IGNORABLE);
        TAGS_TYPE.put("inTAGS_TYPE.put", TagType.IGNORABLE);
        TAGS_TYPE.put("textarea", TagType.IGNORABLE);
        TAGS_TYPE.put("keygen", TagType.IGNORABLE);

        TAGS_TYPE.put("select", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("blockquote", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("caption", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("center", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("col", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("colgroup", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("dd", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("div", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("dl", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("dt", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("fieldset", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("form", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("legend", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("optgroup", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("p", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("pre", TagType.BLOCK_LEVEL_CONTENT);
        TAGS_TYPE.put("table", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("td", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("tfoot", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("th", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("thead", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("tr", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("ul", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("ol", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("li", TagType.BLOCK_LEVEL);
        TAGS_TYPE.put("h1", TagType.BLOCK_LEVEL_TITLE);
        TAGS_TYPE.put("h2", TagType.BLOCK_LEVEL_TITLE);
        TAGS_TYPE.put("h3", TagType.BLOCK_LEVEL_TITLE);
        TAGS_TYPE.put("h4", TagType.BLOCK_LEVEL_TITLE);
        TAGS_TYPE.put("h5", TagType.BLOCK_LEVEL_TITLE);
        TAGS_TYPE.put("h6", TagType.BLOCK_LEVEL_TITLE);
        TAGS_TYPE.put("code", TagType.BLOCK_LEVEL_CONTENT); //main content for sure
        TAGS_TYPE.put("b", TagType.INNER_TEXT); //count as text inside block
        TAGS_TYPE.put("u", TagType.INNER_TEXT); //count as text inside block
        TAGS_TYPE.put("i", TagType.INNER_TEXT);//count as text inside block
        TAGS_TYPE.put("em", TagType.INNER_TEXT);
        TAGS_TYPE.put("strong", TagType.INNER_TEXT);
        TAGS_TYPE.put("span", TagType.INNER_TEXT);
        TAGS_TYPE.put("a", TagType.INNER_TEXT);
        //the <br><br> is a paragraph separator and should
        TAGS_TYPE.put("br", TagType.INNER_TEXT); //count as text inside block
    }

    public static boolean isInnerText(Node tag)
    {
        return tag instanceof Element && TAGS_TYPE.get(tag.nodeName()) == TagType.INNER_TEXT;
    }

    public static boolean isBlockTag(Node tag)
    {
        // FIXME: This doesn't use the tag list above
        return tag instanceof Element && ((Element) tag).isBlock();
    }

    public static boolean isInlineTag(Node tag)
    {
        return tag instanceof Element && ((Element) tag).tag().isInline();
    }

    public static boolean isLinkTag(Node elem)
    {
        return elem instanceof Element && !"".equals(((Element) elem).attr("href"));
    }

}
