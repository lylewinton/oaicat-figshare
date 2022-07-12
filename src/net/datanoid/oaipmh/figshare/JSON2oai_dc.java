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

import java.util.Properties;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.crosswalk.Crosswalk;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Convert native JSONObject "item" to oai_dc.
 * This factory assumes the native JSONObject article details from figshare.
 * The "crosswalk", involves pulling out the items required to create DC.
 * 
 * References:
 *  https://www.openarchives.org/documents/
 * 
 *  Qualifiers, Refinements, DCMI Terms - 
 *  https://www.dublincore.org/specifications/dublin-core/dcmi-terms/
 *  https://www.dublincore.org/specifications/dublin-core/dcmes-qualifiers/#date
 * 
 *  Refinements and Encoding schemes, best practice implementation - 
 *  http://www.ukoln.ac.uk/metadata/dcmi/dc-xml-guidelines/
 * 
 *  Identifiers -
 *  http://www.ukoln.ac.uk/metadata/dcmi-ieee/identifiers/
 * 
 *  Type vocab - 
 *  https://www.dublincore.org/specifications/dublin-core/dcmi-type-vocabulary/
 * 
 *  Refinement linking -
 *  https://www.dublincore.org/specifications/dublin-core/dc-elem-refine/
 *  https://www.dublincore.org/specifications/dublin-core/dc-xml-guidelines/
 *  https://www.dublincore.org/specifications/dublin-core/dc-rdf/
 *  http://www.ukoln.ac.uk/metadata/resources/dc/datamodel/WD-dc-rdf/
 * 
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class JSON2oai_dc extends Crosswalk {
    
    private static final Logger LOG = Logger.getLogger(JSON2oai_dc.class.getName());
    private static ArrayList<String> customFieldsRegex = null;
    private static ArrayList<String> customFieldsFormat = null;
    private static String filesFormat = null;
    private static String dcElementAddAttributes = "";
    
    /**
     * The constructor assigns the schemaLocation associated with this crosswalk. Since
     * the crosswalk is trivial in this case, no properties are utilized.
     *
     * @param properties properties that are needed to configure the crosswalk.
     */
    public JSON2oai_dc(Properties properties) {
	super("http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
        customFieldsRegex = new ArrayList<String>();
        customFieldsFormat = new ArrayList<String>();
        for (int i=1; i<100; i++) {
            String regex = properties.getProperty("JSON2oai_dc.customFields.Regex."+i);
            String format = properties.getProperty("JSON2oai_dc.customFields.Format."+i);
            if ((regex==null) && (format==null)) break;
            if ((regex!=null) && (format!=null)) {
                customFieldsRegex.add(regex);
                customFieldsFormat.add(format);
            }
        }
        dcElementAddAttributes = properties.getProperty("JSON2oai_dc.dcElementAddAttributes");
        if ( (dcElementAddAttributes==null) || (dcElementAddAttributes.trim().length()==0) )
            dcElementAddAttributes = "";
        filesFormat = properties.getProperty("JSON2oai_dc.filesFormat");
        if ( (filesFormat!=null) && (filesFormat.trim().length()==0) )
            filesFormat = null;
    }

    /**
     * Can this nativeItem be represented in DC format?
     * @param nativeItem a record in native format
     * @return true if DC format is possible, false otherwise.
     */
    public boolean isAvailableFor(Object nativeItem) {
        JSONObject jitem = (JSONObject) nativeItem;
        if (jitem.get("title") == null) return false;
        if (jitem.get("id") == null) return false;
        if (jitem.get("description") == null) return false;
        if (jitem.get("citation") == null) return false;
        if (jitem.get("defined_type_name") == null) return false;
        if (jitem.get("url_public_html") == null) return false;
        return true;
    }

    /**
     * Perform the actual crosswalk.
     *
     * @param nativeItem the native JSONObject "item".
     * @return a String containing the XML to be stored within the <metadata> element.
     * @exception CannotDisseminateFormatException nativeItem doesn't support this format.
     */
    public String createMetadata(Object nativeItem)
	throws CannotDisseminateFormatException {
        LOG.log(Level.FINER, "createMetadata() nativeItem="+nativeItem.toString());
        JSONObject jitem = (JSONObject) nativeItem;
	StringBuffer sb = new StringBuffer();
	sb.append("<oai_dc:dc xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
                + "xmlns:dcterms=\"http://purl.org/dc/terms/\" "
                + "xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" "
                + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                + "xsi:schemaLocation=\"");
        sb.append(this.getSchemaLocation());
	sb.append("\" ");
        sb.append(dcElementAddAttributes);
        sb.append(">\n");
        // Output the title
        sb.append("<dc:title>");
        sb.append( Utils.XML_cdata_escape( jitem.get("title") ) );
        sb.append("</dc:title>\n");
        LOG.log(Level.FINER, "createMetadata() early metadata="+sb.toString());
        // Output a DOI & DOI URL, and/or a Handle URI, or a figshare URI
        String uri = null;
        String doi = (String)jitem.get("doi");
        if ( (doi != null) && (doi.length()>0) ) {
            if (!doi.startsWith("http"))
                doi = "https://doi.org/"+doi;
            // NOTE: The DOI identifier is a URI, as per DC recommendations.
            // 2005 citation guidelines indicate "info:doi/10.1045/july99-caplan"
            // URL links are far more useful (opinion).
            // Does not follow figshare's OAI-PMH implementation which does neither "10.1045/july99-caplan"
            sb.append("<dc:identifier xsi:type=\"dcterms:URI\">");
            sb.append( doi );
            sb.append("</dc:identifier>\n");
            uri = doi;
        }
        String hdl = (String)jitem.get("handle");
        if ( (hdl != null) && (hdl.length()>0) ) {
            if (!hdl.startsWith("http"))
                hdl = "https://hdl.handle.net/"+hdl;
            sb.append("<dc:identifier xsi:type=\"dcterms:URI\">");
            sb.append( hdl );
            sb.append("</dc:identifier>\n");
            if (uri==null)
                uri = hdl;
        }
        String figshareurl = (String) jitem.get("url_public_html");
        if (uri==null) {
            // if no DOI or Handle, use the figshare URL as identifier
            sb.append("<dc:identifier xsi:type=\"dcterms:URI\">");
            sb.append( figshareurl );
            sb.append("</dc:identifier>\n");
        }
        // Make the output compatible to figshare's OAI-PMH output
        sb.append("<!-- figshare link -->\n<dc:relation xsi:type=\"dcterms:URI\">");
        sb.append( figshareurl );
        sb.append("</dc:relation>\n");
        // Get the most recent update datetime
        String datetime = JSONRecordFactory.calcDatestamp(nativeItem);
        if (datetime != null) {
            sb.append("<dc:date>");
            sb.append( datetime );
            sb.append("</dc:date>\n");
        }
        // Get the earliest online time timeline.firstOnline
        JSONObject jtimeline = (JSONObject) jitem.get("timeline");
        if (jtimeline != null) {
            String firstonline = (String)jtimeline.get("firstOnline");
            if ( (firstonline!=null) && (firstonline.length()>0) ) {
                // if date+time format, ensure ends with a Z
                if ( (firstonline.length()>10) && (!firstonline.endsWith("Z")) )
                    firstonline = firstonline + "Z";
                sb.append("<!-- firstOnline -->\n<dcterms:issued>");
                sb.append( firstonline );
                sb.append("</dcterms:issued>\n");
            }
        }
        // Get the available embargo_date time, if embargoed
        Boolean is_embargoed = (Boolean)jitem.get("is_embargoed");
        if (is_embargoed) {
            String emb = (String) jitem.get("embargo_date");
            if ( (emb!=null) && (emb.length()>0) ) {
                // if date+time format, ensure ends with a Z
                if ( (emb.length()>10) && (!emb.endsWith("Z")) )
                    emb = emb + "Z";
                sb.append("<!-- Embargoed until -->\n<dcterms:available>");
                sb.append( emb );
                sb.append("</dcterms:available>\n");
            } else {
                sb.append("<!-- Embargoed indefinitely, restricted access -->\n");
            }
        }
        // Get the description, which can include HTML markup
        if (jitem.get("description") != null) {
            sb.append("<dc:description>");
            sb.append(Utils.XML_cdata_escape( jitem.get("description") ) );
            sb.append("</dc:description>\n");
        }
        // Citation
        sb.append("<dcterms:bibliographicCitation>");
        sb.append(Utils.XML_cdata_escape( jitem.get("citation") ) );
        sb.append("</dcterms:bibliographicCitation>\n");
        // type - defined_type_name
        // Make output compatible with figshare's OAI-PMH, do the DC defined first, figshare defined second
        String typename = (String) jitem.get("defined_type_name");
        String dctype = null;
        switch (typename.toLowerCase()) {
            case "dataset":
                dctype="Dataset";
                break;
            case "collection":
                dctype="Collection";
                break;
            case "performance":
            case "event":
                dctype="Event";
                break;
            case "figure":
            case "composition":
                dctype="Image";
                break;
            case "media":
                dctype="Moving Image";
                break;
            case "physical object":
                dctype="Physical Object";
                break;
            case "service":
                dctype="Service";
                break;
            case "software":
                dctype="Software";
                break;
            case "poster":
            case "journal contribution":
            case "conference contribution":
            case "preprint":
            case "presentation":
            case "thesis":
            case "book":
            case "online resource":
            case "chapter":
            case "peer review":
            case "educational resource":
            case "report":
            case "standard":
            case "data management plan":
            case "workflow":
            case "monograph":
            case "model":
            case "registration":
            case "funding":
            default:
                dctype="Text";
                break;
        }
        sb.append("<dc:type xsi:type=\"dcterms:DCMIType\">");
        sb.append( dctype );
        sb.append("</dc:type>\n");
        if (!dctype.equalsIgnoreCase(typename)) {
            sb.append("<dc:type xsi:type=\"figshare:types\">");
            sb.append( typename );
            sb.append("</dc:type>\n");
        }
        // IsReferencedBy - resource_title: resource_doi: "10.5072/FK2.developmentfigshare.2000005"
        String refby_title = (String) jitem.get("resource_title");
        if ( (refby_title!=null) && (refby_title.length()>0) )  {
            sb.append("<!-- Resource Title in figshare -->\n<dcterms:isReferencedBy>");
            sb.append(Utils.XML_cdata_escape(refby_title) );
            sb.append("</dcterms:isReferencedBy>\n");
        }
        String refby_doi = (String) jitem.get("resource_doi");
        if ( (refby_doi!=null) && (refby_doi.length()>0) )  {
            if (!refby_doi.startsWith("http"))
                refby_doi = "https://doi.org/"+refby_doi;
            sb.append("<!-- Resource DOI in figshare -->\n<dcterms:isReferencedBy  xsi:type=\"dcterms:URI\">");
            sb.append( refby_doi );
            sb.append("</dcterms:isReferencedBy>\n");
        }
        // rights - license.name license.url
        JSONObject license = (JSONObject) jitem.get("license");
        if (license!=null) {
            sb.append("<dc:rights>");
            sb.append(Utils.XML_cdata_escape(license.get("name")) );
            sb.append("</dc:rights>\n");
            sb.append("<dc:rights xsi:type=\"dcterms:URI\">");
            sb.append( license.get("url") );
            sb.append("</dc:rights>\n");
        }
        // creator - authors[]{}
        JSONArray authors = (JSONArray) jitem.get("authors");
        if (authors != null)
            for (Object item: authors) {
                JSONObject author = (JSONObject)item;
                String authstr = (String)author.get("full_name");
                String orcid = (String)author.get("orcid_id");
                Long aid = (Long)author.get("id");
                // Determine a person URI (ORCID or figshare profile), so we can link elements
                String personuri = null;
                if ( (orcid!=null) && (orcid.length()>0) ) {
                    if (!orcid.startsWith("http"))
                        orcid = "https://orcid.org/"+orcid;
                    personuri = orcid;
                } else if ( (aid!=null) && (aid>0) ) {
                    Boolean active = (Boolean)author.get("is_active");
                    if (active)
                        personuri = "https://figshare.com/authors/_/"+aid;
                }
                String rdflink = "";
                if (personuri!=null)
                    rdflink = " rdf:resource=\""+personuri+"\"";
                // Make "creator" name compatible with figshare's OAI-PMH by adding figshare id
                if ( (aid!=null) && (aid>0) ) {
                    authstr = authstr + " ("+aid+")";
                }
                sb.append("<dc:creator");
                sb.append( rdflink );
                sb.append(">");
                sb.append(Utils.XML_cdata_escape(authstr) );
                sb.append("</dc:creator>\n");
                // Add the creator link
                if (personuri!=null) {
                    sb.append("<dcterms:creator refines=\"dc:creator\" xsi:type=\"dcterms:URI\"");
                    sb.append( rdflink );
                    sb.append(">");
                    sb.append( personuri );
                    sb.append("</dcterms:creator>\n");
                }
            }
        // DC.subject - categories[]{}.title
        JSONArray categories = (JSONArray) jitem.get("categories");
        if (categories != null)
            for (Object item: categories) {
                JSONObject cat = (JSONObject)item;
                String title = (String)cat.get("title");
                sb.append("<dc:subject xsi:type=\"figshare:categories\">");
                sb.append(Utils.XML_cdata_escape(title) );
                sb.append("</dc:subject>\n");
            }
        // DC.subject - tags[]
        JSONArray tags = (JSONArray) jitem.get("tags");
        if (tags != null)
            for (Object item: tags) {
                String tag = (String)item;
                sb.append("<dc:subject xsi:type=\"figshare:tags\">");
                sb.append(Utils.XML_cdata_escape(tag) );
                sb.append("</dc:subject>\n");
            }
        // DCTERMS.references - references[]
        JSONArray refs = (JSONArray) jitem.get("references");
        if (refs != null)
            for (Object item: refs) {
                String ref = (String)item;
                // Make output compatible with figshare's OAI-PMH, add figshare id
                if ( (ref!=null) && (ref.length()>0) ) {
                    if (ref.matches("10\\.\\d{4,9}/[-._;()/:a-zA-Z0-9]+"))
                        ref = "https://doi.org/"+ref;
                    sb.append("<dcterms:references");
                    if (ref.startsWith("http"))
                        sb.append(" xsi:type=\"dcterms:URI\"");
                    sb.append(">");
                    sb.append(Utils.XML_cdata_escape(ref) );
                    sb.append("</dcterms:references>\n");
                }
            }
        // Description.funding - funding_list[]{} .title .funder_name
        JSONArray funding_list = (JSONArray) jitem.get("funding_list");
        if ( (funding_list != null) && (funding_list.size()>0) ) {
            sb.append("<!-- funding_list in figshare -->\n");
            for (Object item: funding_list) {
                JSONObject fund = (JSONObject)item;
                String title = (String)fund.get("title");
                String funder = (String)fund.get("funder_name");
                String code = (String)fund.get("grant_code");
                String fundingstr = title; // user defined just has title
                if ((code!=null) && (code.length()>0))
                    fundingstr = fundingstr + " (" + code + ")";
                if ((funder!=null) && (funder.length()>0))
                    fundingstr = fundingstr + ", funded by " + funder;
                //sb.append("<dcterms:isPartOf>");
                sb.append("<dc:description.funding>");
                sb.append(Utils.XML_cdata_escape(fundingstr) );
                //sb.append("</dcterms:isPartOf>\n");
                sb.append("</dc:description.funding>\n");
            }
        }
        // filesFormat - files[]{} .name .download_url .computed_md5
        // NOTE: Left completely flexible, figshare's OAI-PMH implementation seems lacking.
        if (filesFormat!=null) {
            JSONArray files = (JSONArray) jitem.get("files");
            if ( (files != null) && (files.size()>0) ) {
                sb.append("<!-- files in figshare -->\n");
                for (Object item: files) {
                    JSONObject file = (JSONObject)item;
                    String fname = (String)file.get("name");
                    String md5 = (String)file.get("computed_md5");
                    String downl = (String)file.get("download_url");
                    if ( (downl!=null) && (downl.length()>0) ) {
                        sb.append( Utils.XML_format_name_value(filesFormat,fname,downl,md5) );
                        sb.append("\n");
                    }
                }
            }
        }
        // customFieldsFormat - custom_fields[]{} .name .value=(String/[])
        if (customFieldsRegex.size() > 0) {
            JSONArray custom_fields = (JSONArray) jitem.get("custom_fields");
            if ( (custom_fields != null) && (custom_fields.size()>0) ) {
                sb.append("<!-- custom_fields in figshare -->\n");
                for (Object item: custom_fields) {
                    JSONObject custom = (JSONObject)item;
                    String name = (String)custom.get("name");
                    for (int i=0; i<customFieldsRegex.size(); i++) {
                        if (!name.matches( customFieldsRegex.get(i) )) continue;
                        String format = customFieldsFormat.get(i);
                        Object values = custom.get("value");
                        if (!(values instanceof JSONArray)) {
                            // if a single value, add it to an array to simplify
                            JSONArray val = new JSONArray();
                            val.add(values);
                            values = val;
                        }
                        for (Object val: (JSONArray)values) {
                            String valstr = val.toString();
                            sb.append( Utils.XML_format_name_value(format,name,valstr,null) );
                            sb.append("\n");
                        }
                        // after first match don't bother with others
                        break;
                    }
                }
            }
        }
        sb.append("</oai_dc:dc>");
        LOG.log(Level.FINER, "createMetadata() metadata="+sb.toString());
	return sb.toString();
    }
}
