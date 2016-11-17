package com.vtjiles.inky;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Inky {
    public static Integer COLUMN_COUNT = 12;

    public static String releaseTheKraken(String markup) {
        // (to-be) supported tags
        List<String> tags = Arrays.asList("button", "row", "columns", "container", "callout", "inky", "block-grid", "menu", "menuItem", "center", "spacer", "wrapper");

        // parse our markup
        Document document = Jsoup.parse(markup);

        // clone the body so we can mess with it
        Element body = document.body().clone();

        // loop each supported tag and look for an implementation.
        for (String tag : tags) {
            switch(tag.toUpperCase()) {
                case "BUTTON":
                    Elements buttons = body.select("button");
                    while(buttons.size() > 0) {
                        Element element = buttons.get(0);
                        
                        String target = "";
                        if(element.hasAttr("target")) {
                            target = " target='" + element.attr("target") + "'";
                        }
                        
                        String inner = element.html();
                        if(element.hasAttr("href")) {
                            inner = "<a href=\"" + element.attr("href") + "\"" + target + ">" + inner + "</a>";
                        }
                        
                        String expander = "";
                        if(element.hasClass("expand") || element.hasClass("expanded")) {
                            inner = "<center>" + inner + "</center>";
                            expander = "\n<td class=\"expander\"></td>";
                        }

                        List<String> classes = getSourceClasses(element, "button");

                        Element table = getNode("<table><tr><td><table><tr><td inky-content></td></tr></table></td>" + expander + "</tr></table>");
                        table.attr("class", String.join(" ", classes));
                        setContent(inner, table);
                        element.replaceWith(table);

                        // re-sync
                        buttons = body.select("button");
                    }

                    break;

                case "CALLOUT" :
                    Elements callouts = body.select("callout");
                    while(callouts.size() > 0) {
                        Element element = callouts.first();

                        List<String> classes = getSourceClasses(element, "callout-inner");

                        Element table = getNode("<table class=\"callout\"><tr><th inky-content class=\"" + String.join(" ", classes) + "\"></th><th class=\"expander\"></th></tr></table>");
                        addAttributes(element, table);

                        // set inner html
                        setContent(element, table);

                        // replace element
                        element.replaceWith(table);

                        // re-sync
                        callouts = body.select("callout");
                    }

                    break;

                case "CENTER" :
                    body.select("center").forEach(element -> {
                        element.attr("data-parsed", "");

                        element.children().forEach(child -> {
                            child.attr("align", "center").addClass("float-center");
                            child.select("item, .menu-item").addClass("float-center");
                        });
                    });
                    break;

                case "COLUMNS" :
                    Elements columns = body.select("columns");
                    while(columns.size() > 0) {
                        Element element = columns.first();

                        Integer colCount = 1;
                        for (Element child : element.siblingElements()) {
                            if(child.tagName().toUpperCase().equals("COLUMNS")) {
                                colCount++;
                            }
                        }

                        Integer smallSize = getAttributeInt(element, "small", COLUMN_COUNT);
                        Integer largeSize = getAttributeInt(element, "large", null);
                        if(largeSize == null) {
                            largeSize = getAttributeInt(element, "small", null);
                        }
                        if(largeSize == null) {
                            largeSize = (int) Math.floor(COLUMN_COUNT / (1.0 * colCount));
                        }

                        boolean noExpander = element.hasAttr("no-expander");
                        if(noExpander) {
                            noExpander = !element.attr("no-expander").equals("false");
                        }

                        List<String> classes = getSourceClasses(element);
                        classes.add("small-" + smallSize);
                        classes.add("large-" + largeSize);
                        classes.add("columns");

                        if(element.previousElementSibling() == null) {
                            classes.add("first");
                        }
                        if(element.nextElementSibling() == null) {
                            classes.add("last");
                        }

                        // If the column contains a nested row, the .expander class should not be used
                        String expander = "";
                        if(largeSize.equals(COLUMN_COUNT) && element.select(".row, row").size() == 0 && !noExpander) {
                            expander = "\n<th class=\"expander\"></th>";
                        }

                        Element th = getNode("<th><table><tr><th inky-content></th>" + expander + "</tr></table></th>");
                        addAttributes(element, th);
                        th.attr("class", String.join(" ", classes));

                        setContent(element, th);
                        element.replaceWith(th);

                        // re-sync
                        columns = body.select("columns");
                    }
                    break;

                case "CONTAINER" :
                    Elements containers = body.select("container");

                    while(containers.size() > 0) {
                        Element element = containers.first();

                        List<String> classes = getSourceClasses(element, "container");

                        Element table = getNode("<table><tbody><tr><td inky-content></td></tr></tbody></table>");
                        addAttributes(element, table);
                        table.attr("align", "center").attr("class", String.join(" ", classes));

                        setContent(element, table);
                        element.replaceWith(table);

                        // re-sync
                        containers = body.select("container");
                    }
                    break;

                case "ROW" :
                    Elements rows = body.select("row");
                    while(rows.size() > 0) {
                        Element element = rows.first();

                        List<String> classes = getSourceClasses(element, "row");

                        Element table = getNode("<table><tbody><tr inky-content></tr></tbody></table>");
                        addAttributes(element, table);
                        table.attr("class", String.join(" ", classes));
                        setContent(element, table);
                        element.replaceWith(table);

                        // re-sync
                        rows = body.select("row");
                    }

                    break;

                case "SPACER":
                    Elements spacers = body.select("spacer");
                    while(spacers.size() > 0) {
                        Element element = spacers.get(0);
                        List<String> classes = getSourceClasses(element, "spacer");

                        String template = "<table class=\"%1$s\"><tbody><tr><td height=\"%2$spx\" style=\"font-size:%2$spx;line-height:%2$spx;\">&#xA0;</td></tr></tbody></table>";

                        String html = "";
                        // set size
                        Integer size = 16;
                        if(element.hasAttr("size-sm") || element.hasAttr("size-lg")) {
                            if(element.hasAttr("size-sm")) {
                                size = getAttributeInt(element, "size-sm", size);

                                List<String> smClasses = clone(classes);
                                smClasses.add("hide-for-large");
                                html += String.format(template, String.join(" ", smClasses), size);
                            }
                            else if(element.hasAttr("size-lg")) {
                                size = getAttributeInt(element, "size-lg", size);

                                List<String> lgClasses = clone(classes);
                                lgClasses.add("show-for-large");
                                html += String.format(template, String.join(" ", lgClasses), size);
                            }
                        }
                        else {
                            size = getAttributeInt(element, "size", size);
                            html += String.format(template, String.join(" ", classes), size);
                        }

                        String content = "<div>" + html + "</div>";

                        // replace
                        Element div = getNode(content);
                        Element lastEl = div.child(0);
                        element.replaceWith(lastEl);

                        Elements children = div.children();
                        int childCount = children.size();

                        if(childCount > 0) {
                            for (Element child: div.children()) {
                                lastEl.after(child);
                                lastEl = child;
                            }
                        }

                        // re-sync
                        spacers = body.select("spacer");
                    }
                    break;

                case "WRAPPER":
                    Elements wrappers = body.select("wrapper");
                    while(wrappers.size() > 0) {
                        Element element = wrappers.first();

                        List<String> classes = getSourceClasses(element, "wrapper");

                        Element table = getNode("<table><tr><td class=\"wrapper-inner\" inky-content></td></tr></table>");
                        addAttributes(element, table);
                        table.attr("align", "center").attr("class", String.join(" ", classes));
                        setContent(element, table);
                        element.replaceWith(table);

                        // re-sync
                        wrappers = body.select("wrapper");
                    }
                    break;

                default: break;
            }
        }

        document.body().replaceWith(body);

        return document.outerHtml();
    }

    private static Element getNode(String html) {
        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
        return doc.child(0);
    }

    private static void addAttributes(Element source, Element target) {
        List<String> ignore = Arrays.asList("class", "id", "href", "size", "large", "no-expander", "small", "target");

        for (Attribute attribute : source.attributes()) {
            if(!ignore.contains(attribute.getKey())) {
                target.attr(attribute.getKey(), attribute.getValue());
            }
        }
    }

    private static void setContent(Element source, Element target) {
        setContent(source.html(), target);
    }

    private static void setContent(String html, Element target) {
        Element content = target.select("[inky-content]").first();
        content.empty();


        Element node = getNode("<div>" + html + "</div>");
        node.children().forEach(content::appendChild);
        content.removeAttr("inky-content");
    }

    private static List<String> getSourceClasses(Element element) {
        return getSourceClasses(element, null);
    }

    private static List<String> getSourceClasses(Element element, String seedClass) {
        List<String> classes = new ArrayList<>();
        if(seedClass != null && !seedClass.isEmpty()) {
            classes.add(seedClass);
        }

        if(element.hasAttr("class")) {
            String[] elClasses = element.attr("class").split(" ");
            for(String elClass : elClasses) {
                classes.add(elClass);
            }
        }

        return classes;
    }

    private static Integer getAttributeInt(Element el, String attr, Integer defaultValue) {
        if(el.hasAttr(attr)) {
            String val = el.attr(attr);
            try {
                return Integer.valueOf(val);
            }
            catch(Exception ex) {
                // return default
            }
        }

        return defaultValue;
    }

    private static List<String> clone(List<String> orig) {
        List<String> clone = new ArrayList<>();
        clone.addAll(orig);
        return clone;
    }
}
