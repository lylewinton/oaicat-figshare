/*
 * Copyright (c) 2022, Lyle Winton <lyle@winton.id.au>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
 /* SPDX-License-Identifier: BSD-2-Clause  */
package net.datanoid.oaipmh.figshare;

import java.util.regex.Matcher;

/**
 * Static class with common utility functions.
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class Utils {
    /**
     * Escape string as a safe XML text element, enclosed in CDATA if needed.
     * @param item object to convert to string
     * @return escaped string.
     */
    protected static String XML_cdata_escape(Object item) {
        if (item==null)
            return null;
        String out = item.toString();
        // don't XML_cdata_escape if no XML code
        if (out.indexOf("<")<0)
            return out;
        // XML_cdata_escape any end of CDATA occurences
        out = out.replaceAll("]]>", "]]]]><![CDATA[>");
        // return as CDATA
        return "<![CDATA["+out+"]]>";
    }

    /**
     * Escape string as a safe XML attribute value.
     * @param item object to convert to string
     * @return escaped string.
     */
    protected static String XML_attrib_escape(Object item) {
        if (item==null)
            return null;
        String out = item.toString();
        out = out.replaceAll("&", "&amp;");
        out = out.replaceAll("<", "&lt;");
        out = out.replaceAll(">", "&gt;");
        out = out.replaceAll("\"", "&quot;");
        out = out.replaceAll("'", "&apos;");
        out = out.replaceAll("\n", "&xA;");
        out = out.replaceAll("\r", "&xD;");
        return out;
    }

    /**
     * Escape string as a safe XML element name, replacing non-alphanumeric with underscore.
     * @param item object to convert to string
     * @return escaped string.
     */
    protected static String XML_element_escape(Object item) {
        if (item==null)
            return null;
        String out = item.toString();
        return out.toLowerCase().replaceAll("[!\\W]", "_");
    }

    /**
     * Escape string as a safe XML element name, replacing non-alphanumeric with underscore.
     * @param item object to convert to string
     * @return escaped string.
     */
    protected static String XML_format_name_value(String format, String name, String value, String value2) {
        String name_element = XML_element_escape(name);
        String name_cdata = XML_cdata_escape(name);
        String name_attrib = XML_attrib_escape(name);
        String val_cdata = XML_cdata_escape(value);
        String val_attrib = XML_attrib_escape(value);
        String out = format;
        out = out.replaceAll("%NAME%",Matcher.quoteReplacement(name));
        out = out.replaceAll("%NAME_ELEMENT%",Matcher.quoteReplacement(name_element));
        out = out.replaceAll("%NAME_CDATA%",Matcher.quoteReplacement(name_cdata));
        out = out.replaceAll("%NAME_ATTRIB%",Matcher.quoteReplacement(name_attrib));
        out = out.replaceAll("%VALUE%",Matcher.quoteReplacement(value));
        out = out.replaceAll("%VALUE_CDATA%",Matcher.quoteReplacement(val_cdata));
        out = out.replaceAll("%VALUE_ATTRIB%",Matcher.quoteReplacement(val_attrib));
        if (value2!=null) {
            String val2_cdata = XML_cdata_escape(value2);
            String val2_attrib = XML_attrib_escape(value2);
            out = out.replaceAll("%VALUE2%",Matcher.quoteReplacement(value));
            out = out.replaceAll("%VALUE2_CDATA%",Matcher.quoteReplacement(val2_cdata));
            out = out.replaceAll("%VALUE2_ATTRIB%",Matcher.quoteReplacement(val2_attrib));
        }
        return out;
    }


    /**
     * Find and extract an XML element and all contents withing the string.
     * @param doc document string to search within.
     * @param xmlelement element to search for, include any namespace prefix
     * @return element and contents as string.
     */
    protected static String XML_get_element(String doc, String xmlelement) {
        int i1 = doc.indexOf("<"+xmlelement);
        int i2 = doc.lastIndexOf("</"+xmlelement);
        if ((i1>=0) && (i2>0)) {
            int i3 = doc.indexOf(">", i2);
            return doc.substring(i1, i3+1);
        }
        return null;
    }

    /**
     * Find and extract an XML element contents withing the string.
     * If element contents is fully CDATA wrapped, CDATA escaping is removed.
     * @param doc document string to search within.
     * @param xmlelement element to search for, include any namespace prefix
     * @return contents as string.
     */
    protected static String XML_get_element_contents(String doc, String xmlelement) {
        String element = XML_get_element(doc,xmlelement);
        if (element==null) return null;
        // attempt to strip off root element start and end XML
        int i1 = element.indexOf(">",1);
        int i2 = element.lastIndexOf("</"+xmlelement);
        if ((i1<0) || (i2<(i1+1))) return null;
        element = element.substring(i1+1, i2);
        // check if we've got something fully CDATA escaped, and strip out the CDATA
        // this is technically incorrect decoding, should probably find all CDATA elements
        // but because oaicat-figshare escaped this text originally, it's predictable
        if (element.startsWith("<![CDATA[") && element.endsWith("]]>")) {
            element = element.substring( "<![CDATA[".length() , element.length()-"]]>".length() );
            element = element.replaceAll("]]]]><!\\[CDATA\\[>", "]]>");
        }
        return element;
    }
    
    /**
     * Convert java String (native UTF16) to UTF-8 string with typical
     * software code escaping of non ASCII chars as hex codes.  eg. \u0000
     * @param input string to be escaped.
     * @return escaped string.
     */
    protected static String StringToUTF8Escaped(String input) {
        StringBuilder builder = new StringBuilder();
        for(char ch: input.toCharArray()) {
            if(ch >= 0x20 && ch <= 0x7E) {
                builder.append(ch);
            } else {
                builder.append(String.format("\\u%04X", (int)ch));
            }
        }
        return builder.toString();        
    }
    
}
