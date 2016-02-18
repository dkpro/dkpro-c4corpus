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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.jsoup.select.NodeVisitor;

import java.util.HashMap;

/**
 * @author Omnia Zayed original code author is Phu-Hiep DUONG (found on-line but
 * edited some parts)
 */
public class NodeHelper {

    /**
     * return the nearest common ancestor of node1 and node2
     *
     * @throws IllegalStateException if node1 and node2 has no common ancestor
     * to make sure that node1 and node2 should inside the same document
     */
    public static Node nearestCommonAncestor(Node node1, Node node2) {
        Node ancestor = node1;
        while (ancestor != null) {
            if (isAncestor(ancestor, node2)) {
                return ancestor;
            }
            ancestor = ancestor.parent();
        }
        throw new IllegalStateException("node1 and node2 do not have common ancestor");
    }

    /**
     * return true if node1 is ancestor of node2 or node1 == node2
     */
    public static boolean isAncestor(Node node1, Node node2) {
        if (node1 == node2) {
            return true;
        }
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
     * return true if node has ancestor satisfy f
     */
    public static boolean ancestorSatisfy(Node node, Function<Node, Boolean> f) {
        Node ancestor = node;

        while (ancestor != null) {
            if (f.apply(ancestor)) {
                return true;
            }
            ancestor = ancestor.parent();
        }

        return false;
    }

    /**
     * return all parent of a Node
     */
    public static String displayParent(Node node) {
        StringBuilder ret = new StringBuilder();
        Node ancestor = node;

        String padding = "\n";
        while (ancestor != null) {
            if (ancestor instanceof Element) {
                ret.append(padding);
                ret.append("<");
                ret.append(((Element) ancestor).tagName());
                for (Attribute attr : ancestor.attributes()) {
                    ret.append(" " + attr.getKey() + "=\"" + attr.getValue() + "\"");
                }
                ret.append(">");
            }
            padding += "  ";
            ancestor = ancestor.parent();
        }

        return ret.toString();
    }

    /**
     * return the Tag of the first heading (h1, h2..) ancestor otherwise return
     * null if no ancestor is heading
     */
    public static Tag findHeadingAncestor(Node node) {
        Node ancestor = node;

        while (ancestor != null) {
            Tag t = getHeadingTag(ancestor);
            if (t != null) {
                return t;
            }
            ancestor = ancestor.parent();
        }

        return null;
    }

    /**
     * return the unique leaf tag (deepest child tag) of the ancestor return
     * null if the ancestor has more than one leaf. for example
     * "<div><span><kaka>abc</kaka><span/></div>" will return "<kaka>abc</kaka>"
     *
     * @param ancestor
     */
    public static Node getUniqueLeafTag(Node ancestor) {
        if (ancestor == null || !(ancestor instanceof Element)) {
            return null;
        }

        if (ancestor.childNodeSize() == 0) {
            return ancestor;
        }

        if (ancestor.childNodeSize() == 1) {
            Node uniqueChild = ancestor.childNode(0);
            if (uniqueChild instanceof TextNode) {
                return ancestor;
            } else {
                return getUniqueLeafTag(uniqueChild);
            }
        }

        return null;
    }

    /**
     * return true if node has a link ancestor
     */
    public static boolean isLink(Node node) {
        Node ancestor = node;

        while (ancestor != null) {
            if (isLinkTag(ancestor)) {
                return true;
            }
            ancestor = ancestor.parent();
        }

        return false;
    }

    /**
     * getTextContent avoid NullReferenceException
     */
    public static String getTextContent(Element node) {
        return (node == null) ? null : node.text();
    }

    // see: http://stackoverflow.com/questions/7541843/how-to-search-for-comments-using-jsoup
    //TODO: could be deletd not used as it throws stackoverflow error. I replaced it with more simpler one
    public static void removeComments(Node node) {
        for (int i = 0; i < node.childNodes().size();) {
            Node child = node.childNode(i);
            if (child != null && child.nodeName().equals("#comment")) {
                child.remove();
            } else if (child != null) {
                removeComments(child);
                i++;
            }
        }
    }

    public static enum TagType {

        IGNORABLE, INNERTEXT, BLOCKLEVEL, BLOCKLEVEL_CONTENT, BLOCKLEVEL_TITLE
    }

    public static final HashMap<String, TagType> TagsType = new HashMap<String, TagType>() {
        {
            put("style", TagType.IGNORABLE);
            put("script", TagType.IGNORABLE);
            put("option", TagType.IGNORABLE);
            put("noscript", TagType.IGNORABLE);
            put("embed", TagType.IGNORABLE);
            put("applet", TagType.IGNORABLE);
            put("link", TagType.IGNORABLE);
            put("button", TagType.IGNORABLE);
            put("select", TagType.IGNORABLE);
            put("input", TagType.IGNORABLE);
            put("textarea", TagType.IGNORABLE);
            put("keygen", TagType.IGNORABLE);

            put("blockquote", TagType.BLOCKLEVEL);
            put("caption", TagType.BLOCKLEVEL);
            put("center", TagType.BLOCKLEVEL);
            put("col", TagType.BLOCKLEVEL);
            put("colgroup", TagType.BLOCKLEVEL);
            put("dd", TagType.BLOCKLEVEL);
            put("div", TagType.BLOCKLEVEL);
            put("dl", TagType.BLOCKLEVEL);
            put("dt", TagType.BLOCKLEVEL);
            put("fieldset", TagType.BLOCKLEVEL);
            put("form", TagType.BLOCKLEVEL);
            put("legend", TagType.BLOCKLEVEL);
            put("optgroup", TagType.BLOCKLEVEL);
            put("p", TagType.BLOCKLEVEL);
            put("pre", TagType.BLOCKLEVEL_CONTENT);
            put("table", TagType.BLOCKLEVEL);
            put("td", TagType.BLOCKLEVEL);
            put("tfoot", TagType.BLOCKLEVEL);
            put("th", TagType.BLOCKLEVEL);
            put("thead", TagType.BLOCKLEVEL);
            put("tr", TagType.BLOCKLEVEL);
            put("ul", TagType.BLOCKLEVEL);
            put("ol", TagType.BLOCKLEVEL);
            put("li", TagType.BLOCKLEVEL);
            put("h1", TagType.BLOCKLEVEL_TITLE);
            put("h2", TagType.BLOCKLEVEL_TITLE);
            put("h3", TagType.BLOCKLEVEL_TITLE);
            put("h4", TagType.BLOCKLEVEL_TITLE);
            put("h5", TagType.BLOCKLEVEL_TITLE);
            put("h6", TagType.BLOCKLEVEL_TITLE);
            put("code", TagType.BLOCKLEVEL_CONTENT); //main content for sure
            put("b", TagType.INNERTEXT); //count as text inside block
            put("u", TagType.INNERTEXT); //count as text inside block
            put("i", TagType.INNERTEXT);//count as text inside block
            //the <br><br> is a paragraph separator and should
            put("br", TagType.INNERTEXT); //count as text inside block
        }
    };

    /**
     * Keep only some attribute (src, href, style) remove all other
     *
     * @param attrName
     * @return
     */
    public static boolean isIgnorableAttribute(String attrName) {
        return !"src".equalsIgnoreCase(attrName)
                && !"href".equalsIgnoreCase(attrName)
                && !"style".equalsIgnoreCase(attrName)
                && !"lang".equalsIgnoreCase(attrName)
                && !"content".equalsIgnoreCase(attrName)
                && !"title".equalsIgnoreCase(attrName)
                && !"charset".equalsIgnoreCase(attrName)
                && !"http-equiv".equalsIgnoreCase(attrName)
                && !"alt".equalsIgnoreCase(attrName);
    }

    public static boolean isIgnorableTag(Node tag) {
        if (tag == null || !(tag instanceof Element)) {
            return false;
        }
        return TagsType.get(tag.nodeName()) == TagType.IGNORABLE;
    }

    public static boolean isInnerText(Node tag) {
        if (tag == null || !(tag instanceof Element)) {
            return false;
        }
        return TagsType.get(tag.nodeName()) == TagType.INNERTEXT;
    }

    public static boolean isBlockTag(Node tag) {
        if (tag == null || !(tag instanceof Element)) {
            return false;
        }
        return ((Element) tag).isBlock();
        /*TagType type = TagsType.get(tag.toLowerCase());
         return type == TagType.BLOCKLEVEL || type == TagType.BLOCKLEVEL_CONTENT || type == TagType.BLOCKLEVEL_TITLE;*/
    }

    public static boolean isInlineTag(Node tag) {
        if (tag == null || !(tag instanceof Element)) {
            return false;
        }
        return ((Element) tag).tag().isInline();
        /*TagType type = TagsType.get(tag.toLowerCase());
         return type == TagType.INNERTEXT;*/
    }

    /**
     * return the Tag of element if it is a heading h1, h2.. otherwise return
     * null
     */
    public static Tag getHeadingTag(Node elem) {
        if (elem == null || !(elem instanceof Element)) {
            return null;
        }
        Tag t = ((Element) elem).tag();
        return TagsType.get(t.getName()) == TagType.BLOCKLEVEL_TITLE ? t : null;
    }

    public static boolean isLinkTag(Node elem) {
        if (elem == null || !(elem instanceof Element)) {
            return false;
        }
        return "a".equalsIgnoreCase(elem.nodeName()) || "link".equalsIgnoreCase(elem.nodeName());
    }

    public static boolean isImgTag(Node elem) {
        if (elem == null || !(elem instanceof Element)) {
            return false;
        }
        return "img".equalsIgnoreCase(elem.nodeName());
    }

    public static boolean isEmptyElement(Node node) {
        if (node == null) {
            return false;
        }
        if (node instanceof TextNode) {
            return StringUtil.isBlank(((TextNode) node).text());
        }
        if (!(node instanceof Element)) {
            return false;
        }
        boolean isEmptyTag = ((Element) node).tag().isEmpty();
        return !isEmptyTag && hasEmptyChidren(node);
    }

    public static boolean hasEmptyChidren(Node node) {
        if (node.childNodeSize() == 0) {
            return true;
        }
        for (Node n : node.childNodes()) {
            if (!(n instanceof TextNode)) {
                return false;
            }

            if (!StringUtil.isBlank(((TextNode) n).text())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isIgnorableTagNode(Node node) {
        if (node instanceof Comment || isIgnorableTag(node)) {
            return true;
        }
        if (node.nodeName().equalsIgnoreCase("img") && Strings.isNullOrEmpty(node.attr("src"))) {
            //ignore tag img without source
            return true;
        }
//		String styleAttr = node.attr("style");
//		if (styleAttr!=null) {
//			if (styleAttr.replace(" ", "").toLowerCase().contains("display:none")) {
//				//ignore element invisible
//				return true;
//			}
//		}
        return false;
    }

    private static class TagUnwrapper implements NodeVisitor {

        private boolean modified = false;

        @Override
        public void head(Node node, int depth) {
            if (node.childNodeSize() == 1) {
                Node child = node.childNode(0);
                if (child.childNodeSize() == 1 && child.nodeName().equalsIgnoreCase(node.nodeName())) {
                    if (child.attributes().size() == 0) {
                        child.unwrap();
                        modified = true;
                    }
                }
            }
        }

        @Override
        public void tail(Node node, int depth) {

        }

        public boolean isModified() {
            return modified;
        }
    }

    private static class EmptyNodeCleaner implements NodeVisitor {

        private boolean modified = false;

        @Override
        public void head(Node node, int depth) {
            for (int i = 0; i < node.childNodes().size();) {
                Node child = node.childNode(i);

                //remove empty elements
                if (isEmptyElement(child)) {
                    child.remove();
                    modified = true;
                } else {
                    i++;
                }
            }
        }

        @Override
        public void tail(Node node, int depth) {

        }

        public boolean isModified() {
            return modified;
        }
    }

    public static void cleanEmptyElements(Node node) {
        EmptyNodeCleaner enc;
        do {
            enc = new EmptyNodeCleaner();
            node.traverse(enc);
        } while (enc.isModified());
    }

    public static void unwrapRedundancyTags(Node node) {
        TagUnwrapper tu;
        do {
            tu = new TagUnwrapper();
            node.traverse(tu);
        } while (tu.isModified());
    }

    public static String detectLanguage(Document doc) {
        Element htmlTag = doc.select("html").first();
        if (htmlTag.attributes().hasKey("lang")) {
            return htmlTag.attr("lang");
        }
        if (htmlTag.attributes().hasKey("xml:lang")) {
            return htmlTag.attr("xml:lang");
        }
        return null;
    }
}
