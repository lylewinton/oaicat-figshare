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

import ORG.oclc.oai.server.OAIHandler;
import ORG.oclc.oai.server.catalog.AbstractCatalog;
import ORG.oclc.oai.server.verb.BadArgumentException;
import ORG.oclc.oai.server.verb.BadResumptionTokenException;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command-line interface to allow harvesting of recent figshare records
 * (OAI-PMH ListRecords style) without the need for an OAI web server in between.
 * Run this class without arguments to obtain help on what arguments are required.
 * 
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class FigshareOAIMain {
    
    private static final Logger LOG = Logger.getLogger(FigshareOAICatalog.class.getName());
    private static Properties properties = new Properties();

    private static final String[] VALID_GRANULARITIES = {
        "YYYY-MM-DD",
        "YYYY-MM-DDThh:mm:ssZ"
    };
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Logger.getLogger("net.datanoid").setLevel(Level.INFO);
        for (Handler handler : Logger.getLogger("").getHandlers())
            handler.setLevel(Level.INFO);
        
        // Handle command line arguments
        ArrayList argslist = new ArrayList( Arrays.asList(args) );
        String xmlelement = "";
        boolean xmlcontent = false;
        while (argslist.size()>0) {
            String firstarg = (String) argslist.get(0);
            if (firstarg.equals("-debug")) {
                argslist.remove(0);
                Logger.getLogger("net.datanoid").setLevel(Level.FINE);
                for (Handler handler : Logger.getLogger("").getHandlers())
                    handler.setLevel(Level.FINE);
                continue;
            }
            if (firstarg.equals("-ddebug")) {
                argslist.remove(0);
                Logger.getLogger("net.datanoid").setLevel(Level.FINER);
                for (Handler handler : Logger.getLogger("").getHandlers())
                    handler.setLevel(Level.FINER);
                continue;
            }
            if (firstarg.equals("-get-xml-element") && (argslist.size()>1)) {
                argslist.remove(0);
                xmlelement = (String) argslist.get(0);
                argslist.remove(0);
                continue;
            }
            if (firstarg.equals("-get-xml-content")) {
                argslist.remove(0);
                xmlcontent = true;
                continue;
            }
            break;
        }
        if (argslist.size() != 5) {
            System.err.println("ERROR: Required arguments not found.\n"
                    + "Use:  Emulate an OAI-PMH \"ListRecords\" request outputing the returned records to separate files.\n"
                    + "Arguments:  [-debug|-ddebug] [-get-xml-element xml-element] [-get-xml-content] /path/to/oaicat-figshare.properties output-folder from-date until-date metadataPrefix\n"
                    + "   xml-element = specify name \"namespace:element\" to extract from within each record (eg. qdc:qualifieddc, oai_dc:dc or json:element)\n"
                    + "   -get-xml-content - return the contents of the element, not the including the XML element"
                    + "   output-folder = folder location to write new record files\n"
                    + "   from-date = yyyy-MM-dd  OR  yyyy-MM-ddTHH:mm:ssX  (eg. 2022-07-02T14:23:48Z best practice to use UTC timezone indicated by X=Z)\n"
                    + "   until-date = as above  OR  - dash for current time\n"
                    + "   metadataPrefix = qdc  OR  oai_dc  OR  json\n"
                    + "      (supported metadataPrefix types found in properties file as 'Crosswalks.<metadataPrefix>=...' parameters)");
            System.exit(-1);
        }
        String confFileName = (String) argslist.get(0);
        String outputFolderName = (String) argslist.get(1);
        String fromDate = (String) argslist.get(2);
        String toDate = (String) argslist.get(3);
        if (toDate.equals("-"))
            toDate = null;
        String metadataPrefix = (String) argslist.get(4);
        System.out.println("### properties-file="+confFileName);
        System.out.println("### output-folder="+outputFolderName);
        System.out.println("### xml-element="+xmlelement);
        System.out.println("### get-xml-content="+xmlcontent);
        System.out.println("### from-date="+fromDate);
        System.out.println("### until-date="+toDate);
        System.out.println("### metadataPrefix="+metadataPrefix);
        
        // Load properties and create main classes
        InputStream in = null;
        try {
            in = new FileInputStream(confFileName);
        } catch (FileNotFoundException e) {
            LOG.log(Level.SEVERE, "properties-file not found. Try fixing the path.");
            System.exit(-1);
        }
        LOG.log(Level.FINE, "properties-file was found: Loading the properties");
        properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        FigshareOAICatalog figshareOAICatalog = new FigshareOAICatalog(properties);
        figshareOAICatalog.setRecordFactory(new JSONRecordFactory(properties));
        
        // Check outputs folder exists
        Path outputFolderNamePath = Paths.get(outputFolderName);
        if ( (!Files.isDirectory(outputFolderNamePath)) || (!Files.isWritable(outputFolderNamePath)) ) {
            LOG.log(Level.SEVERE, "output-folder not found or not writeable.");
            System.exit(-1);
        }
        
        // Setup some defaults which normally would have been set by OAI-CAT and AbstractCatalogue factory
        String harvestable = properties.getProperty("AbstractCatalog.harvestable");
        if (harvestable != null && harvestable.equals("false")) {
            figshareOAICatalog.setHarvestable(false);
        }
        String secondsToLive = properties.getProperty("AbstractCatalog.secondsToLive");
        if (secondsToLive != null) {
            figshareOAICatalog.setMillisecondsToLive( Integer.parseInt(secondsToLive) * 1000 );
        }
        String granularity = properties.getProperty("AbstractCatalog.granularity");
        for (int i = 0; granularity != null && i < VALID_GRANULARITIES.length; ++i) {
            if (granularity.equalsIgnoreCase(VALID_GRANULARITIES[i])) {
                figshareOAICatalog.setSupportedGranularityOffset(i);
                break;
            }
        }
        
        // Begin retrieving the records
        int count=0;
        Instant start = Instant.now();
        Date lastretrieve = new Date();
        int exit_code=0;
        try {
            // retrieve the first lot from the commandline settings
            LOG.log(Level.FINE, "main() run initial figshareOAICatalog.listRecords()");
            System.out.println("Retrieving up to "+figshareOAICatalog.maxListSize+" records...");
            Map records = figshareOAICatalog.listRecords(fromDate, toDate, null, metadataPrefix);
            while (1==1) {
                // extract returned list
                Iterator records_it = (Iterator) records.get("records");
                Iterator recordids_it = (Iterator) records.get("records_ids");
                Map resumption_map = (Map) records.get("resumptionMap");
                if ((records_it==null) || (recordids_it==null)) {
                    LOG.log(Level.SEVERE, "Missing records iterators. Unexpected failure.");
                    exit_code = 2;
                    break;
                }
                // output each returned record to a file
                while (records_it.hasNext()) {
                    count++;
                    LOG.log(Level.FINE, "Processing record #"+count);
                    String record = (String) records_it.next();
                    String recordid = (String) recordids_it.next();
                    // sanitise a filename base on recordID
                    String fileout = recordid.replaceAll("[\\s\\.<>:\"'/\\|\\?\\*\\\\]", "_")+".xml";
                    System.out.println("### record="+count+"; id="+recordid+"; filename="+fileout);
                    // find requested element, eg. to remove the OAI wrappers and get metadata payload
                    if (xmlelement.length()>0) {
                        if (xmlcontent)
                            record = Utils.XML_get_element_contents(record,xmlelement);
                        else
                            record = Utils.XML_get_element(record,xmlelement);
                    }
                    // save to file
                    //System.out.println(record);
                    try {
                        if (record == null)
                            System.out.println("Warning: no XML element found, output file skipped");
                        else {
                            Path fileoutpath = Paths.get(outputFolderName, fileout);
                            LOG.log(Level.FINER, "Creating file: " + fileoutpath.toString());
                            BufferedWriter out = new BufferedWriter(
                                    new OutputStreamWriter(
                                            new FileOutputStream(fileoutpath.toFile()),
                                            "UTF-8")
                            );
                            if (!xmlcontent) {
                                // literal copied from OAICat
                                out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                            }
                            // The output is a valid UTF-8 encoded file.
                            // Note that figshare outputs hex escaped UTF-8 characters eg. \u0000
                            // If this is ever required the following will work.
                            //out.write(Utils.StringToUTF8Escaped(record));
                            out.write(record);
                            out.write("\n");
                            out.close();
                            LOG.log(Level.FINER, "Done writing file: " + fileoutpath.toString());
                        }
                    } catch (IOException iOException) {
                        LOG.log(Level.SEVERE, "Problem writing record to file.",iOException);
                        System.exit(-1);
                    }
                    
                }
                // Check if more to resume
                if (resumption_map==null) break;
                String resumptionToken = (String) resumption_map.get("resumptionToken");
                if ((resumptionToken==null) || (resumptionToken.length()<=0)) break;
                // add a pause between calls to be friendly to the figshare API
                System.out.println("Pausing for 10 seconds...");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ex) { }
                // retrieve the next lot of records starting from resumptionToken
                LOG.log(Level.FINE, "main() run additional figshareOAICatalog.listRecords() resumptionToken="+resumptionToken);
                lastretrieve = new Date();
                System.out.println("Retrieving up to next "+figshareOAICatalog.maxListSize+" records...");
                records = figshareOAICatalog.listRecords(resumptionToken);
            }
        } catch (BadArgumentException | CannotDisseminateFormatException ex) {
            Logger.getLogger(FigshareOAIMain.class.getName()).log(Level.SEVERE, "Server responded with an EXCEPTION", ex);
            exit_code = 2;
        } catch (BadResumptionTokenException ex) {
            Logger.getLogger(FigshareOAIMain.class.getName()).log(Level.SEVERE, "Resumption Token may have timed out. Unexpected.", ex);
            exit_code = 2;
        } catch (OAIInternalServerError ex) {
            Logger.getLogger(FigshareOAIMain.class.getName()).log(Level.SEVERE, "Server connection EXCEPTION.", ex);
            exit_code = 1;
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        System.out.println("### Harvested "+count+" records in "+ timeElapsed.toMillis()/1000.0 +" seconds");
        if (toDate==null) {
            String formatOut = "yyyy-MM-dd'T'HH:mm:ssX";
            SimpleDateFormat strFormatOut = new SimpleDateFormat(formatOut);
            strFormatOut.setTimeZone(TimeZone.getTimeZone("UTC"));
            toDate=strFormatOut.format(lastretrieve);
        }
        System.out.println("### Next from-date="+toDate);
        System.exit(exit_code);
    }
    
}
