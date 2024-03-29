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
 * Take the native JSONObject "item" and wrap the JSON within an XML element.
 * This Crosswalk assumes the native JSONObject article details from figshare.
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
        //TODO seek a better schema element to use than XSD element itself
	sb.append("<json:element xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:json=\"http://www.w3.org/2001/XMLSchema\" "
                + "xsi:schemaLocation=\"");
        sb.append(this.getSchemaLocation());
	sb.append("\" ");
        sb.append("name=\"json\" type=\"xs:string\" ");
        sb.append(">");
        // The JSON standard (EMCA-404) allows for any UTF-8 characters to be within
        // a string, except backslash and doublequote which are to be escaped (\\ and \").
        // Typically \n \r \f \b \t and \/ are escaped also.
        // Other UTF-16 characters, for example accents and multilingual characters,
        // can be further escaped using backslash-u and 4 hex digits eg. \u0000 ,
        // but technically this is not required.
        // https://www.ecma-international.org/publications-and-standards/standards/ecma-404/
        // Note that figshare outputs hex escaped UTF-8 characters eg. \u0000.
        // This may allow better browser compatibility with JSON, as the whole
        // of JSON can then effectively be 7 bit ASCI. 
        // If this is ever required the following will work.
        //sb.append( Utils.XML_cdata_escape( Utils.StringToUTF8Escaped( jitem.toJSONString() ) ) );
        sb.append( Utils.XML_cdata_escape( jitem.toJSONString() ) );
        sb.append("</json:element>");
        LOG.log(Level.FINER, "createMetadata() metadata="+sb.toString());
	return sb.toString();
    }


}
