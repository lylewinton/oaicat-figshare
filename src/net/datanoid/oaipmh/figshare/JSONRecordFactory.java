/*
 * Copyright (c) 2020, Lyle Winton <lyle@winton.id.au>
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
package net.datanoid.oaipmh.figshare;

import java.util.Iterator;
import java.util.Properties;
import ORG.oclc.oai.server.catalog.RecordFactory;
import java.util.StringTokenizer;
import org.json.simple.JSONObject;

/**
 * JSONRecordFactory converts native JSONObject items to "header" Strings.
 * This factory assumes the native JSONObject search/article details from figshare.
 * 
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class JSONRecordFactory extends RecordFactory {
    
    private String repositoryIdentifier = null;
    
    /**
     * Construct an JSONRecordFactory capable of producing the Crosswalk(s)
     * specified in the properties file.
     * @param properties Contains information to configure the factory:
     *                   specifically, the names of the crosswalk(s) supported
     * @exception IllegalArgumentException Something is wrong with the argument.
     */
    public JSONRecordFactory(Properties properties)
	throws IllegalArgumentException {
	super(properties);
	repositoryIdentifier = properties.getProperty("JSONRecordFactory.repositoryIdentifier");
	if (repositoryIdentifier == null) {
	    throw new IllegalArgumentException("JSONRecordFactory.repositoryIdentifier is missing from the properties file");
	}
    }

    /**
     * Utility method to parse the 'local identifier' from the OAI identifier
     *
     * @param identifier OAI identifier (e.g. oai:oaicat.oclc.org:ID/12345)
     * @return local identifier (e.g. ID/12345).
     */
    public String fromOAIIdentifier(String identifier) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(identifier, "/");
            tokenizer.nextToken();
            return tokenizer.nextToken();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Construct an OAI identifier from the native item
     *
     * @param nativeItem native Item object
     * @return OAI identifier
     */
    public String getOAIIdentifier(Object nativeItem) {
        JSONObject jitem = (JSONObject) nativeItem;
        Long id = (Long)jitem.get("id");
	StringBuffer sb = new StringBuffer();
	sb.append("oai:");
	sb.append(repositoryIdentifier);
	sb.append(":article/");
	sb.append(id);
	return sb.toString();
    }

    /**
     * get the datestamp from the JSON item.
     * Look for String timeline.revision or use published_date.
     *
     * @param nativeItem a native item presumably containing a datestamp somewhere
     * @return a String containing the datestamp for the item
     * @exception IllegalArgumentException Something is wrong with the argument.
     */
    public String getDatestamp(Object nativeItem)
	throws IllegalArgumentException  {
        String date = calcDatestamp(nativeItem);
        if (date == null)
            throw new IllegalArgumentException("getDatestamp() JSON cannot find timeline.revision / published_date");
        return calcDatestamp(nativeItem);
    }
    /**
     * get the datestamp from the JSON item.
     * Look for String timeline.revision or use published_date.
     *
     * @param nativeItem a native item presumably containing a datestamp somewhere
     * @return a String containing the datestamp for the item or null.
     */
    protected static String calcDatestamp(Object nativeItem) {
        JSONObject jitem = (JSONObject) nativeItem;
        JSONObject jtimeline = (JSONObject) jitem.get("timeline");
        String date = null;
        if (jtimeline != null) {
            date = (String) jtimeline.get("revision");
        }
        if (date == null)
            date = (String) jitem.get("published_date");
        if ((date == null) || (date.length()<2))
            return null;
        // if date+time format, ensure ends with a Z
        if ( (date.length()>10) && (!date.endsWith("Z")) )
            return date+"Z";
        return date;
    }

    /**
     * get the setspec from the item
     *
     * @param nativeItem a native item presumably containing a setspec somewhere
     * @return a String containing the setspec for the item
     * @exception IllegalArgumentException Something is wrong with the argument.
     */
    public Iterator getSetSpecs(Object nativeItem)
	throws IllegalArgumentException  {
	return null;
    }

    /**
     * Get the about elements from the item
     *
     * @param nativeItem a native item presumably containing about information somewhere
     * @return a Iterator of Strings containing &lt;about&gt;s for the item
     * @exception IllegalArgumentException Something is wrong with the argument.
     */
    public Iterator getAbouts(Object nativeItem) throws IllegalArgumentException {
	return null;
    }

    /**
     * Is the record deleted?
     *
     * @param nativeItem a native item presumably containing a possible delete indicator
     * @return true if record is deleted, false if not
     * @exception IllegalArgumentException Something is wrong with the argument.
     */
    public boolean isDeleted(Object nativeItem)
	throws IllegalArgumentException {
        return false;
    }

    /**
     * Allows classes that implement RecordFactory to override the default create() method.
     * This is useful, for example, if the entire &lt;record&gt; is already packaged as the native
     * record. Return null if you want the default handler to create it by calling the methods
     * above individually.
     * 
     * @param nativeItem the native record
     * @return a String containing the OAI &lt;record&gt; or null if the default method should be
     * used.
     */
    public String quickCreate(Object nativeItem, String schemaLocation, String metadataPrefix) {
	// Don't perform quick creates
	return null;
    }
}
