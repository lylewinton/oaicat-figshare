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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import ORG.oclc.oai.server.catalog.AbstractCatalog;
import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.crosswalk.CrosswalkItem;
import ORG.oclc.oai.server.verb.BadArgumentException;
import ORG.oclc.oai.server.verb.BadResumptionTokenException;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import ORG.oclc.oai.server.verb.NoMetadataFormatsException;
import ORG.oclc.oai.server.verb.NoSetHierarchyException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.datanoid.figshare.FigshareConnection;
import org.json.simple.JSONObject;

/**
 * FigshareOAICatalog is an implementation of the AbstractCatalog interface.
 * This class converts requests to queries for the figshare API and returns results.
 * 
 * CONSIDERATIONS:
 *   - Items within a project are not selectable by a searchFilter.
 *     No figshare call exists that provides project items after a given date.
 *   - Filtering based on :group: may capture more than one group.
 *     Groups ID's can be provided on publicArticlesSearch, but not yet implemented.
 * 
 * TODO: Implement filter on file (regex), output custom metadata link, or output file contents as custom metadata, or output some metadata within the file as custom metadata (eg. if XML/JSON)
 * TODO: filter out items based is_embargoed ?
 * TODO: filter out items based is_metadata_record ?
 * 
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class FigshareOAICatalog extends AbstractCatalog {
    /**
     * maximum number of entries to return for ListRecords and ListIdentifiers
     */
    protected static int maxListSize;
    private static final Logger LOG = Logger.getLogger(FigshareOAICatalog.class.getName());
    private static String searchFilter;
    private static Integer institution = null;

    /**
     * pending resumption tokens
     */
    private HashMap resumptionResults = new HashMap();
    /**
     * local override for getMillisecondsToLive()
     */
    private int local_millisecondsToLive = -2;
    
    /**
     * Construct a FigshareOAICatalog object
     *
     * @param properties a properties object containing initialization parameters
     */
    public FigshareOAICatalog(Properties properties) {
        String maxlistsize = properties.getProperty("FigshareOAICatalog.maxListSize");
        if (maxlistsize == null) {
            //throw new IllegalArgumentException("FigshareOAICatalog.maxListSize is missing from the properties file");
            LOG.log(Level.INFO, "FigshareOAICatalog.maxListSize is missing from the properties file. Set to default of 10.");
            FigshareOAICatalog.maxListSize = 10;
        } else {
            FigshareOAICatalog.maxListSize = Integer.parseInt(maxlistsize);
            if (FigshareOAICatalog.maxListSize > 20) {
                LOG.log(Level.WARNING, "Warning: maxListSize of over 20 not advisable.");
            }
        }
        
        searchFilter = properties.getProperty("FigshareOAICatalog.searchFilter");
        if (searchFilter != null) {
            if (searchFilter.trim().length() == 0)
                searchFilter = "";
        } else
            searchFilter = "";
        
        String institutionstring = properties.getProperty("FigshareOAICatalog.institution");
        if (institutionstring != null) {
            FigshareOAICatalog.institution = Integer.parseInt(institutionstring);
        }
    }
    
    
    /**
     * Turn a datetime into a java Date.
     * Takes into account the finest configured data format, ie. if includes hours:mins:seconds
     *
     * @param indate OAI-PMH formatted date either until or after date
     * @param until indicates it's an until date, be tolerant, don't fail, set date to null if problematic
     * @return a reformatted date string
     * @exception BadArgumentException the specified indate doesn't parse as a date, or granularity is too high
     */
    private Date convertToQueryDate(String indate, boolean until) throws BadArgumentException {
        if (indate==null) return null;
        Date outdate = null;
        LOG.log(Level.FINER, "convertToQueryDate() indate="+indate);
        String finedate = toFinestUntil(indate); // throws BadArgumentException is incorrect granularity
        LOG.log(Level.FINER, "convertToQueryDate() finedate="+finedate);
        String formatIn1 = "yyyy-MM-dd";
        String formatIn2 = "yyyy-MM-dd'T'HH:mm:ssX";
        SimpleDateFormat strFormatIn1 = new SimpleDateFormat(formatIn1);
        strFormatIn1.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat strFormatIn2 = new SimpleDateFormat(formatIn2);
        strFormatIn2.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (finedate.length() == formatIn1.length()) {
            LOG.log(Level.FINER, "convertToQueryDate() found length of formatIn1="+formatIn1);
            try {
                outdate = strFormatIn1.parse(finedate);
                LOG.log(Level.FINER, "convertToQueryDate() idate="+outdate.toString());
            } catch (ParseException ex) {
                LOG.log(Level.FINE, "convertToQueryDate ParseException on '"+indate+"' (finest='"+finedate+"')", ex);
                if (until) {
                    LOG.log(Level.FINE, "convertToQueryDate ParseException bad until assumed.");
                    outdate = null;
                } else
                    throw new BadArgumentException();
            }
        } else if (finedate.length() == strFormatIn2.format(new Date()).length()) {
            LOG.log(Level.FINER, "convertToQueryDate() found length of formatIn2="+formatIn2);
            try {
                outdate = strFormatIn2.parse(finedate);
                LOG.log(Level.FINER, "convertToQueryDate() idate="+outdate.toString());
            } catch (ParseException ex) {
                LOG.log(Level.FINE, "convertToQueryDate ParseException on '"+indate+"' (finest='"+finedate+"')", ex);
                if (until) {
                    LOG.log(Level.FINE, "convertToQueryDate ParseException bad until assumed.");
                    outdate = null;
                } else
                    throw new BadArgumentException();
            }
        } else {
            LOG.log(Level.FINE, "convertToQueryDate cannot match expected format length on '"+indate+"' (finest='"+finedate+"')");
            throw new BadArgumentException();
        }
        return outdate;
    }
    
    
    /**
     * Retrieve a list of schemaLocation values associated with the specified
     * identifier.
     *
     * @param identifier the OAI identifier
     * @return a Vector containing schemaLocation Strings
     * @exception IdDoesNotExistException the specified identifier can't be found
     * @exception NoMetadataFormatsException the specified identifier was found
     * but the item is flagged as deleted and thus no schemaLocations (i.e.
     * metadataFormats) can be produced.
     */
    @Override
    public Vector getSchemaLocations(String identifier)
        throws IdDoesNotExistException, NoMetadataFormatsException, OAIInternalServerError {
        LOG.log(Level.FINE, "getSchemaLocations() for identifier="+identifier);
        String localIdentifier = getRecordFactory().fromOAIIdentifier(identifier);
        LOG.log(Level.FINE, "getSchemaLocations() for localIdentifier="+localIdentifier);
        FigshareConnection connection = new FigshareConnection();
        connection.setRetryCount(2);
        JSONObject nativeItem = null;
        int result = connection.pulbicArticleDetails(Long.parseLong(localIdentifier));
        if (result == 0) {
            nativeItem = connection.responseJSON;
        } else if (result == 1) {
            throw new IdDoesNotExistException(identifier);
        } else {
            LOG.log(Level.SEVERE, "getRecord() pulbicArticleDetails ERROR: "+connection.errorMessage);
            throw new OAIInternalServerError("figshare pulbicArticleDetails ERROR: "+connection.errorMessage);
        }
        if (nativeItem == null)
            throw new IdDoesNotExistException(identifier);

        // TODO Later check if this item meets criteria for each crosswalk.
        // Currently dummy code, this just loops over and returns all crosswalks.
        Iterator iterator = this.getRecordFactory().getCrosswalks().iterator();
        Vector ret = new Vector();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            String itemPrefix = (String) entry.getKey();
            CrosswalkItem crosswalkItem = (CrosswalkItem)entry.getValue();
            Crosswalk crosswalk = crosswalkItem.getCrosswalk();
            LOG.log(Level.FINE, "getSchemaLocations() crosswalk iterator itemPrefix="+itemPrefix+" : "+crosswalk.getSchemaLocation());
            ret.add( crosswalk.getSchemaLocation() );
        }
        return ret;
    }

    /**
     * Retrieve a list of identifiers that satisfy the specified criteria.
     * Note that it may return a Map with an iterator of zero records, as
     * figshare has no way of determining if the last record has been listed.
     *
     * @param from beginning date using the proper granularity
     * @param until ending date using the proper granularity
     * @param set the set name or null if no such limit is requested
     * @param metadataPrefix the OAI metadataPrefix or null if no such limit is requested
     * @return a Map object containing entries for "headers" and "identifiers" Iterators
     * (both containing Strings) as well as an optional "resumptionMap" Map.
     * It may seem strange for the map to include both "headers" and "identifiers"
     * since the identifiers can be obtained from the headers. This may be true, but
     * AbstractCatalog.listRecords() can operate quicker if it doesn't
     * need to parse identifiers from the XML headers itself. Better
     * still, do like I do below and override AbstractCatalog.listRecords().
     * AbstractCatalog.listRecords() is relatively inefficient because given the list
     * of identifiers, it must call getRecord() individually for each as it constructs
     * its response. It's much more efficient to construct the entire response in one fell
     * swoop by overriding listRecords() as I've done here.
     */
    @Override
    public Map listIdentifiers(String from, String until, String set, String metadataPrefix)
            throws BadArgumentException, OAIInternalServerError {
        LOG.log(Level.FINE, "listIdentifiers() for from="+from+" until="+until);
        purge(); // clean out old resumptionTokens
        String filter = searchFilter;
        HashMap inputs = null;
        if (institution != null) {
            inputs = new HashMap();
            inputs.put("institution", institution);
        }
        Map items = findIdentifiers(filter, 1, inputs, metadataPrefix, from, until);
        return finishListIdentifiers(items);
    }

    /**
     * Retrieve the next set of identifiers associated with the resumptionToken
     * Note that it may return a Map with an iterator of zero records, as
     * figshare has no way of determining if the last record has been listed.
     *
     * @param resumptionToken implementation-dependent format taken from the
     * previous listIdentifiers() Map result.
     * @return a Map object containing entries for "headers" and "identifiers" Iterators
     * (both containing Strings) as well as an optional "resumptionMap" Map.
     * @exception BadResumptionTokenException the value of the resumptionToken
     * is invalid or expired.
     */
    @Override
    public Map listIdentifiers(String resumptionToken)
        throws BadResumptionTokenException, OAIInternalServerError {
        LOG.log(Level.FINE, "listIdentifiers() for resumptionToken="+resumptionToken);
        Map items = findIdentifiers(resumptionToken);
        Map rmap = finishListIdentifiers(items);
        return rmap;
    }

    /**
     * Convert a list of JSONObjects into an identifier list.
     *
     * @param findIdentifiersMap Output of findIdentifiers.
     * @return a Map object as expected for other listIdentifiers() implementations.
     */
    private Map finishListIdentifiers(Map findIdentifiersMap) throws OAIInternalServerError {
        Map listIdentifiersMap = new HashMap();
        ArrayList headers = new ArrayList();
        ArrayList identifiers = new ArrayList();
        ArrayList items = (ArrayList) findIdentifiersMap.get("items");
        LOG.log(Level.FINE, "listIdentifiers(map) got items count="+items.size());
        for (Object item: items) {
            JSONObject jitem = (JSONObject)item;
            String[] header = getRecordFactory().createHeader(jitem);
            headers.add(header[0]);
            identifiers.add(header[1]);
            LOG.log(Level.FINER, "listIdentifiers(map) added header[1]="+header[1]);
            LOG.log(Level.FINER, "listIdentifiers(map) added header[0]="+header[0]);
        }
        listIdentifiersMap.put("headers", headers.iterator());
        listIdentifiersMap.put("identifiers", identifiers.iterator());
        String resumptionId = (String) findIdentifiersMap.get("resumptionId");
        if ( (resumptionId != null) && (resumptionId.length()>0) )
            listIdentifiersMap.put("resumptionMap", getResumptionMap(resumptionId));
        return listIdentifiersMap;
    }

    /**
     * Retrieve a list of identifiers given figshare params, as JSONObjects.
     * Note that it may return a Map with an iterator of zero records, as
     * figshare has no way of determining if the last record has been listed.
     *
     * @param filter figshare search filter string.
     * @param page starting page for search results, from 1.
     * @param inputs figshare search input params.
     * @return a Map including "items"(JSONObject) "ids"(Long) "resumptionId"(String)
     */
    private Map findIdentifiers(String filter, int page, Map inputs, String metadataPrefix, String from, String until)
            throws BadArgumentException, OAIInternalServerError {
        Map findIdentifiersMap = new HashMap();
        ArrayList items = new ArrayList();
        ArrayList ids = new ArrayList();
        LOG.log(Level.FINE, "findIdentifiers() page="+page+" filter="+filter);
        FigshareConnection connection = new FigshareConnection();
        connection.setRetryCount(2);
        int result = connection.publicArticlesSearch(filter, page, maxListSize, inputs,
                convertToQueryDate(from,false),
                convertToQueryDate(until,true) );
        LOG.log(Level.FINE, "findIdentifiers() figshare publicArticlesSearch return="+result);
        if (result == 0) {
            LOG.log(Level.FINE, "findIdentifiers() publicArticlesSearch count="+connection.responseArrayJSON.size());
            for (Object item: connection.responseArrayJSON) {
                JSONObject jitem = (JSONObject)item;
                Long id = (Long)jitem.get("id");
                items.add(jitem);
                ids.add(id);
                //LOG.log(Level.FINE, "findIdentifiers() ***DUMP JSON***\n"+jitem.toJSONString());
            }
            findIdentifiersMap.put("items", items);
            findIdentifiersMap.put("ids", ids);
            if (connection.responseArrayJSON.size() == maxListSize) {
                // RESUMPTION TOKEN NEEDED
                String resumptionId = getResumptionId();
                LOG.log(Level.FINE, "findIdentifiers() publicArticlesSearch resumptionId="+resumptionId);
                Map resumptionData = new HashMap();
                resumptionData.put("filter", filter);
                resumptionData.put("page", new Integer(page+1));
                resumptionData.put("inputs", inputs);
                resumptionData.put("mdprefix", metadataPrefix);
                if (from!=null)
                resumptionData.put("from", from);
                if (until!=null)
                resumptionData.put("until", until);
                resumptionResults.put(resumptionId, resumptionData);
                findIdentifiersMap.put("resumptionId", resumptionId);
            }
        } else {
            LOG.log(Level.SEVERE, "findIdentifiersMap publicArticlesSearch ERROR: "+connection.errorMessage);
            throw new OAIInternalServerError("figshare publicArticlesSearch ERROR: "+connection.errorMessage);
        }
        return findIdentifiersMap;
    }

    /**
     * Retrieve a list of identifiers given resumptionToken.
     * See above.
     *
     * @param resumptionToken OAI resumption token.
     * @return a Map including "items"(JSONObject) "ids"(Long) "resumptionId"(String)
     */
    private Map findIdentifiers(String resumptionToken)
            throws BadResumptionTokenException, OAIInternalServerError {
        LOG.log(Level.FINE, "findIdentifiers() for resumptionToken="+resumptionToken);
        purge(); // clean out old resumptionTokens
        Map resumptionData = (HashMap) resumptionResults.get(resumptionToken);
        if (resumptionData == null) {
            LOG.log(Level.SEVERE, "findIdentifiers() BadResumptionTokenException resumptionToken="+resumptionToken);
            throw new BadResumptionTokenException();
        }
        String filter = (String) resumptionData.get("filter");
        Integer page = (Integer) resumptionData.get("page");
        Map inputs = (Map) resumptionData.get("inputs");
        String metadataPrefix = (String) resumptionData.get("mdprefix");
        String from = (String) resumptionData.get("from");
        String until = (String) resumptionData.get("until");
        Map items = null;
        try {
            items = findIdentifiers(filter, page, inputs, metadataPrefix, from, until);
        } catch (BadArgumentException ex) {
            LOG.log(Level.SEVERE, "findIdentifiers() Unexpected failure, succeeded initially, but not on resumptionToken="+resumptionToken, ex);
            return null;
        }
        resumptionResults.remove(resumptionToken);
        return items;
    }
    
    
    /**
     * Retrieve the specified metadata for the specified identifier.
     *
     * @param identifier the OAI identifier
     * @param metadataPrefix the OAI metadataPrefix
     * @return the <record/> portion of the XML response.
     * @exception CannotDisseminateFormatException the metadataPrefix is not
     * supported by the item.
     * @exception IdDoesNotExistException the identifier wasn't found
     */
    @Override
    public String getRecord(String identifier, String metadataPrefix)
        throws CannotDisseminateFormatException,
               IdDoesNotExistException, OAIInternalServerError {
        LOG.log(Level.FINE, "getRecord() for identifier="+identifier);
        String localIdentifier = getRecordFactory().fromOAIIdentifier(identifier);
        LOG.log(Level.FINE, "getRecord() for localIdentifier="+localIdentifier);
        FigshareConnection connection = new FigshareConnection();
        connection.setRetryCount(2);
        JSONObject nativeItem = null;
        int result = connection.pulbicArticleDetails(Long.parseLong(localIdentifier));
        LOG.log(Level.FINE, "getRecord() figshare pulbicArticleDetails return="+result);
        if (result == 0) {
            nativeItem = connection.responseJSON;
        } else if (result == 2) {
            LOG.log(Level.SEVERE, "getRecord() pulbicArticleDetails IdDoesNotExistException: "+connection.errorMessage);
            throw new IdDoesNotExistException(identifier);
        } else {
            LOG.log(Level.SEVERE, "getRecord() pulbicArticleDetails ERROR: "+connection.errorMessage);
            throw new OAIInternalServerError("figshare pulbicArticleDetails ERROR: "+connection.errorMessage);
        }
        if (nativeItem == null)
            throw new IdDoesNotExistException(identifier);
        return constructRecord(nativeItem, metadataPrefix);
    }

    /**
     * Retrieve a list of records that satisfy the specified criteria.
     * Note that it may return a Map with an iterator of zero records, as
     * figshare has no way of determining if the last record has been listed.
     *
     * @param from beginning date using the proper granularity
     * @param until ending date using the proper granularity
     * @param set the set name or null if no such limit is requested
     * @param metadataPrefix the OAI metadataPrefix or null if no such limit is requested
     * @return a Map object containing entries for a "records" Iterator object
     * (containing XML <record/> Strings) and an optional "resumptionMap" Map. 
     * "records_ids" added includes the IDs for each record, additional to OAICAT requirements,
     * but used in FigshareOAIMain.
     * @exception CannotDisseminateFormatException the metadataPrefix isn't
     * supported by the item.
     * @exception BadArgumentException the data format is incorrect
     * @exception OAIInternalServerError the figshare server returned an error
     */
    @Override
    public Map listRecords(String from, String until, String set, String metadataPrefix)
        throws BadArgumentException, CannotDisseminateFormatException, OAIInternalServerError {
        LOG.log(Level.FINE, "listRecords() for from="+from+" until="+until);
        purge(); // clean out old resumptionTokens
        Map listRecordsMap = new HashMap();
        ArrayList records = new ArrayList();
        ArrayList records_ids = new ArrayList();

        String filter = searchFilter;
        HashMap inputs = null;
        if (institution != null) {
            inputs = new HashMap();
            inputs.put("institution", institution);
        }

        Map items = findIdentifiers(filter, 1, inputs, metadataPrefix, from, until);
        ArrayList jitems = (ArrayList) items.get("items");
        String oaiid = "";
        for (Object jitem: jitems) {
            try {
                oaiid = getRecordFactory().getOAIIdentifier(jitem);
                String record = getRecord( oaiid, metadataPrefix );
                LOG.log(Level.FINER, "listRecords() adding record="+record);
                records.add(record);
                records_ids.add(oaiid);
            } catch (IdDoesNotExistException ex) {
                // it is possible that the item has just been unpublished 
                LOG.log(Level.WARNING, "listRecords() cannot find record "+oaiid+" Exception",ex);
                //throw new OAIInternalServerError("listRecords() cannot find record Exception: "+ex.toString());
            }
        }
        
        String resumptionId = (String) items.get("resumptionId");
        if ( (resumptionId != null) && (resumptionId.length()>0) )
            listRecordsMap.put("resumptionMap", getResumptionMap(resumptionId));

        listRecordsMap.put("records", records.iterator());
        listRecordsMap.put("records_ids", records_ids.iterator());
        return listRecordsMap;
    }

    /**
     * Retrieve the next set of records associated with the resumptionToken.
     * Note that it may return a Map with an iterator of zero records, as
     * figshare has no way of determining if the last record has been listed.
     *
     * @param resumptionToken implementation-dependent format taken from the
     * previous listRecords() Map result.
     * @return a Map object containing entries for a "records" Iterator object
     * (containing XML <record/> Strings) and an optional "resumptionMap" Map.
     * "records_ids" added includes the IDs for each record, additional to OAICAT requirements,
     * but used in FigshareOAIMain.
     * @exception BadResumptionTokenException the value of the resumptionToken argument
     * is invalid or expired.
     */
    @Override
    public Map listRecords(String resumptionToken)
        throws BadResumptionTokenException, OAIInternalServerError {
        LOG.log(Level.FINE, "listRecords() for resumptionToken="+resumptionToken);
        Map listRecordsMap = new HashMap();
        ArrayList records = new ArrayList();
        ArrayList records_ids = new ArrayList();
        
        // Obtain some resumption details, should include last page+1
        Map resumptionData = (HashMap) resumptionResults.get(resumptionToken);
        if (resumptionData == null) {
            throw new BadResumptionTokenException();
        }
        String metadataPrefix = (String) resumptionData.get("mdprefix");
        
        // Find next page of items.
        Map items = findIdentifiers(resumptionToken);
        ArrayList jitems = (ArrayList) items.get("items");
        String oaiid = "";
        for (Object jitem: jitems) {
            try {
                oaiid = getRecordFactory().getOAIIdentifier(jitem);
                String record = getRecord( oaiid, metadataPrefix );
                LOG.log(Level.FINER, "listRecords() adding record="+record);
                records.add(record);
                records_ids.add(oaiid);
            } catch (IdDoesNotExistException ex) {
                // it is possible that the item has just been unpublished 
                LOG.log(Level.WARNING, "listRecords() cannot find record "+oaiid+" Exception",ex);
                //throw new OAIInternalServerError("listRecords() cannot find record Exception: "+ex.toString());
            } catch (CannotDisseminateFormatException ex) {
                LOG.log(Level.SEVERE, "listRecords() unexpected CannotDisseminateFormatException",ex);
                throw new OAIInternalServerError("listRecords() unexpected CannotDisseminateFormatException"+ex.toString());
            }
        }
        
        String resumptionId = (String) items.get("resumptionId");
        if (resumptionId != null)
            listRecordsMap.put("resumptionMap", getResumptionMap(resumptionId));

        listRecordsMap.put("records", records.iterator());
        listRecordsMap.put("records_ids", records_ids.iterator());
        return listRecordsMap;
    }

    /**
     * Utility method to construct a Record object for a specified
     * metadataFormat from a native record
     *
     * @param nativeItem native item from the database
     * @param metadataPrefix the desired metadataPrefix for performing the crosswalk
     * @return the <record/> String
     * @exception CannotDisseminateFormatException the record is not available
     * for the specified metadataPrefix.
     */
    private String constructRecord(Object nativeItem, String metadataPrefix)
        throws CannotDisseminateFormatException {
        String schemaURL = null;

        if (metadataPrefix != null) {
            LOG.log(Level.FINER, "constructRecord() getting schemaURL for metadataPrefix="+metadataPrefix);
            if ((schemaURL = getCrosswalks().getSchemaURL(metadataPrefix)) == null)
                throw new CannotDisseminateFormatException(metadataPrefix);
        }
        return getRecordFactory().create(nativeItem, schemaURL, metadataPrefix);
    }

    /**
     * Not implemented.
     * Retrieve a list of sets that satisfy the specified criteria.
     *
     * @return a Map object containing "sets" Iterator object (contains
     * <setSpec/> XML Strings) as well as an optional resumptionMap Map.
     * @exception OAIBadRequestException signals an http status code 400 problem
     */
    @Override
    public Map listSets() throws NoSetHierarchyException {
	throw new NoSetHierarchyException();

    }

    /**
     * Not implemented.
     * Retrieve the next set of sets associated with the resumptionToken.
     *
     * @param resumptionToken implementation-dependent format taken from the
     * previous listSets() Map result.
     * @return a Map object containing "sets" Iterator object (contains
     * <setSpec/> XML Strings) as well as an optional resumptionMap Map.
     * @exception BadResumptionTokenException the value of the resumptionToken
     * is invalid or expired.
     */
    @Override
    public Map listSets(String resumptionToken)
      throws BadResumptionTokenException {
	throw new BadResumptionTokenException();
    }

    /**
     * get the optional millisecondsToLive property (<0 indicates no limit)
     **/
    @Override
    public int getMillisecondsToLive() {
        if (local_millisecondsToLive == -2) {
            return super.getMillisecondsToLive();
        }
        return local_millisecondsToLive;
    }

    /**
     * set the optional millisecondsToLive property (<0 indicates no limit)
     **/
    public void setMillisecondsToLive(int value) {
        local_millisecondsToLive = value;
    }

    /**
     * close the repository
     */
    @Override
    public void close() { }
    
    /**
     * Purge tokens that are older than the configured time-to-live.
     */
    private void purge() {
        if (getMillisecondsToLive() < 0) return; // no time limit on tokens
        ArrayList old = new ArrayList();
        Date now = new Date();
        Iterator keySet = resumptionResults.keySet().iterator();
        while (keySet.hasNext()) {
            String key = (String)keySet.next();
            String dateprefix = key.substring(0,key.indexOf("-"));
            Date then = new Date(Long.parseLong(dateprefix) + getMillisecondsToLive());
            LOG.log(Level.FINER, "getMillisecondsToLive="+getMillisecondsToLive());
            LOG.log(Level.FINER, "purge-check ID="+key+"\nthen="+dateprefix+" now="+now.getTime()+" expires="+then.getTime());
            if (now.after(then)) {
                old.add(key);
                LOG.log(Level.FINER, "purge!");
            }
        }
        Iterator iterator = old.iterator();
        while (iterator.hasNext()) {
            String key = (String)iterator.next();
            resumptionResults.remove(key);
        }
    }
    
    /**
     * Use the current date as the basis for the resumptiontoken
     *
     * @return a String version of the current time
     */
    private synchronized static String getResumptionId() {
        Date now = new Date();
        String id = Long.toString(now.getTime()) + "-" + UUID.randomUUID();
        return id;
    }
}
