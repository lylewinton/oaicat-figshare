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

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class JSON2json  extends Crosswalk {

    private static final Logger LOG = Logger.getLogger(JSON2oai_dc.class.getName());
    
        /**
     * The constructor assigns the schemaLocation associated with this crosswalk. Since
     * the crosswalk is trivial in this case, no properties are utilized.
     *
     * @param properties properties that are needed to configure the crosswalk.
     */
    public JSON2json(Properties properties) {
	super("http://www.w3.org/2001/XMLSchema http://www.w3.org/2001/XMLSchema.xsd"); // dummy values
    }


    /**
     * Can this nativeItem be represented in JSON format? (always can be)
     * @param nativeItem a record in native format
     * @return true if JSON format is possible, false otherwise.
     */
    @Override
    public boolean isAvailableFor(Object nativeItem) {
        return true;
    }

    /**
     * Perform the actual crosswalk.
     *
     * @param nativeItem the native JSONObject "item".
     * @return a String containing the XML wrapper containing JSON to be stored within the <metadata> element.
     * @exception CannotDisseminateFormatException nativeItem doesn't support this format.
     */
    @Override
    public String createMetadata(Object nativeItem) throws CannotDisseminateFormatException {
        LOG.log(Level.FINER, "createMetadata() nativeItem="+nativeItem.toString());
        JSONObject jitem = (JSONObject) nativeItem;
	StringBuffer sb = new StringBuffer();
        //TODO seek better schema to use than XSD itself
	sb.append("<json:element xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:json=\"http://www.w3.org/2001/XMLSchema\" "
                + "xsi:schemaLocation=\"");
        sb.append(this.getSchemaLocation());
	sb.append("\" ");
        sb.append("name=\"json\" type=\"xs:string\" ");
        sb.append(">");
        sb.append( cdata_escape( jitem.toJSONString() ) );
        sb.append("</json:element>");
        LOG.log(Level.FINER, "createMetadata() metadata="+sb.toString());
	return sb.toString();
    }

    /**
     * Escape string as a safe XML text element, enclosed in CDATA if needed.
     * @param item object to convert to string
     * @return escaped string.
     */
    private String cdata_escape(Object item) {
        if (item==null)
            return null;
        String out = item.toString();
        // don't cdata_escape if no XML code
        if (out.indexOf("<")<0)
            return out;
        // cdata_escape any end of CDATA occurences
        out = out.replaceAll("]]>", "]]]]><![CDATA[>");
        // return as CDATA
        return "<![CDATA["+out+"]]>";
    }

}
