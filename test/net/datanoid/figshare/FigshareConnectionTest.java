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
package net.datanoid.figshare;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class FigshareConnectionTest {

    // DEFAULT TEST SETTINGS
    static String testArticleSearch = ":institution: melbourne AND :group: Zoology";
    static Long testArticleID = new Long(4689088); // article test https://melbourne.figshare.com/articles/presentation/A_simple_virtual_organisation_model_and_practical_implementation/4689088
    static Long testArticleID_utf8 = new Long(14428889); // unicode test https://figshare.com/articles/book/______Medea_and_Hippolytus/14428889
    // DEFAULT TEST SETTINGS, THESE REQUIRE CHANGING FOR THE TESTER'S USER ACCOUNT
    static String authToken = ""; // create and enter an App token for your account
    static Long testArticleID_private = new Long(4689088); // choose a private article from your account
    static String testPrivateSearch = ":project: Tinker Digital HASS Collection"; // choose something sensible for your private account, must return 1 or more articles
    static String testProjectSearch = "Tinker"; // choose something sensible for your account, must return 1 or more projects
    
    // THESE ARE NOT SETTINGS, WILL BE FILLED DURING TESTING
    static Long testProjectID = null;
    static Long testProjectArticleID = null;
    static FigshareConnection connection = null;
    
    public FigshareConnectionTest() {
        if (connection == null)
            connection = new FigshareConnection();
    }
    
    @BeforeClass
    public static void setUpClass() {
        connection = new FigshareConnection();
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of setAuthToken method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testSetAuthToken() {
    }

    /**
     * Test of setReadTimeout method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testSetReadTimeout() {
    }

    /**
     * Test of setSecureConnection method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testSetSecureConnection() {
    }

    /**
     * Test of setInsecureConnection method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testSetInsecureConnection() {
    }

    /**
     * Test of setRetryCount method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testSetRetryCount() {
    }

    /**
     * Test of privateArticleDetails method, of class FigshareConnection.
     */
    //@Ignore
    @Test
    public void testPrivateArticleDetails() {
        if ((authToken==null) || (authToken.length()==0)) {
            System.out.println("#### PrivateArticleDetails WARNING no authToken");
            return;
        }
        // test article details for an article
        System.out.println("#### PrivateArticleDetails start for id="+testArticleID_private);
        connection.setAuthToken(authToken);
        int result = connection.privateArticleDetails(testArticleID_private);
        connection.setAuthToken(null);
        System.out.println("## RETURN DETAILS");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        System.out.println("response:\n"+connection.response);
        System.out.println("responseJSON:\n"+connection.responseJSON);
        System.out.println("responseArrayJSON:\n"+connection.responseArrayJSON);
        assertEquals("Private article details with auth should succeed.", 0, result);
        String url = (String)connection.responseJSON.get("url");
        System.out.println("url = "+url);
        System.out.println("#### PrivateArticleDetails end");
    }

    /**
     * Test of pulbicArticleDetails method, of class FigshareConnection.
     */
    @Test
    public void testPulbicArticleDetails() {
        // test article details for an article
        System.out.println("#### PublicArticleDetails start for id="+testArticleID);
        int result = connection.pulbicArticleDetails(testArticleID);
        System.out.println("## RETURN DETAILS");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        System.out.println("response:\n"+connection.response);
        System.out.println("responseJSON:\n"+connection.responseJSON);
        System.out.println("responseArrayJSON:\n"+connection.responseArrayJSON);
        assertEquals("Public article details should succeed.", 0, result);
        //String url = (String)connection.responseJSON.get("url");
        //System.out.println("url = "+url);
        try {
           Map map = (Map)connection.responseJSON;
           map.forEach((k,v)->System.out.println("Key : " + k + " Value : " + v));
        } catch(Exception e) {
           System.out.println(e);
        }
        
        // test article details for an article with UTF-8
        System.out.println("#### PublicArticleDetails start for id="+testArticleID_utf8+" (UTF8 test)");
        result = connection.pulbicArticleDetails(testArticleID_utf8);
        System.out.println("## RETURN DETAILS");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        assertEquals("Public article details should succeed.", 0, result);
        //String url = (String)connection.responseJSON.get("url");
        //System.out.println("url = "+url);
        String titlecomp = "Μήδεια και Ιππόλυτος = Medea and Hippolytus";
        byte[] bytes = titlecomp.getBytes(StandardCharsets.UTF_8);
        //String titleutf8 = new String(bytes,StandardCharsets.UTF_8);
        String titleget = (String)connection.responseJSON.get("title");
        System.out.println("title should be: "+titlecomp);
        //System.out.println("title in UTF-8: "+titleutf8);
        System.out.println("title got: "+titleget);
        assertEquals("Title should match in Unicode.", titlecomp, titleget);
        
        System.out.println("#### PublicArticleDetails end");
        
    }

    /**
     * Test of privateProjectArticles method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testPrivateProjectArticles() {
        // NOTE: tested in testPrivateProjectSearch
    }

    /**
     * Test of privateProjectArticleDetails method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testPrivateProjectArticleDetails() {
        // NOTE: tested in testPrivateProjectSearch
    }

    /**
     * Test of privateProjectSearch method, of class FigshareConnection.
     */
    //@Ignore
    @Test
    public void testPrivateProjectSearch() {
        if ((authToken==null) || (authToken.length()==0)) {
            System.out.println("#### PrivateProjectsSearch WARNING no authToken");
            return;
        }
        System.out.println("#### PrivateProjectsSearch start");
        
        // test private search with auth token
        connection.setAuthToken(authToken);
        int result = connection.privateProjectSearch(testProjectSearch, 1, 10);
        connection.setAuthToken(null);
        System.out.println("## RETURN DETAILS");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        System.out.println("response:\n"+connection.response);
        System.out.println("responseJSON:\n"+connection.responseJSON);
        System.out.println("responseArrayJSON:\n"+connection.responseArrayJSON);
        assertEquals("Private project search with auth should succeed.", 0, result);
        
        // Check returned a value Array or JSONObject's
        if (connection.responseArrayJSON == null)
            fail("Expected response should be a JSONArray, none returned.");
        System.out.println("Item count = "+connection.responseArrayJSON.size());
        if (connection.responseArrayJSON.size()<=0)
            fail("Expected response should have at least 1 item.");
        System.out.println("## ITEMS...");
        for (Object item: connection.responseArrayJSON) {
            if (!(item instanceof JSONObject))
                fail("Expected array item of type JSONObject, instead got: "+item.getClass().getName());
            JSONObject jitem = (JSONObject)item;
            Long id = (Long)jitem.get("id");
            System.out.println("Item ID = "+id);
            if (testProjectID == null) {
                testProjectID = (Long) jitem.get("id");
                System.out.println("using first Project ID for testing '"+testProjectID+"'");
            }
            try {
               Map map = (Map)jitem;       
               map.forEach((k,v)->System.out.println("Key : " + k + " Value : " + v));
            } catch(Exception e) {
               System.out.println(e);
            }
        }
        
        System.out.println("#### PrivateProjectsSearch end");

        System.out.println("#### PrivateProjectsArticles start for "+testProjectID);       
        // test private search with auth token
        connection.setAuthToken(authToken);
        result = connection.privateProjectArticles(testProjectID, 1, 10);
        connection.setAuthToken(null);
        System.out.println("## RETURN DETAILS");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        System.out.println("response:\n"+connection.response);
        System.out.println("responseJSON:\n"+connection.responseJSON);
        System.out.println("responseArrayJSON:\n"+connection.responseArrayJSON);
        assertEquals("Private project articles list with auth should succeed.", 0, result);
        
        // Check returned a value Array or JSONObject's
        if (connection.responseArrayJSON == null)
            fail("Expected response should be a JSONArray, none returned.");
        System.out.println("Item count = "+connection.responseArrayJSON.size());
        System.out.println("## ITEMS...");
        for (Object item: connection.responseArrayJSON) {
            if (!(item instanceof JSONObject))
                fail("Expected array item of type JSONObject, instead got: "+item.getClass().getName());
            JSONObject jitem = (JSONObject)item;
            Long id = (Long)jitem.get("id");
            System.out.println("Item ID = "+id);
            if (testProjectArticleID == null) {
                testProjectArticleID = (Long) jitem.get("id");
                System.out.println("using first Project Article ID for testing '"+testProjectArticleID+"'");
            }
            try {
               Map map = (Map)jitem;       
               map.forEach((k,v)->System.out.println("Key : " + k + " Value : " + v));
            } catch(Exception e) {
               System.out.println(e);
            }
        }
        System.out.println("#### PrivateProjectsArticles end");

        // test article details for a project article
        System.out.println("#### PrivateProjectArticleDetails start for pid="+testProjectID+", id="+testProjectArticleID);       
        connection.setAuthToken(authToken);
        result = connection.privateProjectArticleDetails(testProjectID, testProjectArticleID);
        connection.setAuthToken(null);
        System.out.println("## RETURN DETAILS");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        System.out.println("response:\n"+connection.response);
        System.out.println("responseJSON:\n"+connection.responseJSON);
        System.out.println("responseArrayJSON:\n"+connection.responseArrayJSON);
        assertEquals("Private project article details with auth should succeed.", 0, result);
        try {
            // test attribute
            String url = (String)connection.responseJSON.get("url");
            System.out.println("url = "+url);
            // test array
            JSONArray tags = (JSONArray)connection.responseJSON.get("tags");
            String tag1 = (String)tags.get(0);
            System.out.println("tags[0] = "+tag1);
            // test object
            JSONObject license = (JSONObject)connection.responseJSON.get("license");
            String lname = (String)license.get("name");
            System.out.println("license.name = "+lname);
            // test array of objects
            JSONArray cats = (JSONArray)connection.responseJSON.get("categories");
            JSONObject cat1 = (JSONObject)cats.get(0);
            System.out.println("categories[0].title = "+(String)cat1.get("title"));
        } catch(Exception e) {
            System.out.println(e);
            fail("Expected details of article JSONObject not found.");
        }
        System.out.println("#### PrivateProjectArticleDetails end");
        
    }

    /**
     * Test of privateArticlesSearch method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testPrivateArticlesSearch_4args() {
    }

    /**
     * Test of privateArticlesSearch method, of class FigshareConnection.
     */
    //@Ignore
    @Test
    public void testPrivateArticlesSearch_3args() {
        if ((authToken==null) || (authToken.length()==0)) {
            System.out.println("#### PrivateArticlesSearch WARNING no authToken");
            return;
        }
        System.out.println("#### PrivateArticlesSearch start");
        
        // test private search no auth
        int result = connection.privateArticlesSearch(testPrivateSearch, 1, 10);
        System.out.println("## RETURN DETAILS (test private without auth, error is expected");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        assertEquals("Private search without auth should fail. ", 1, result);
        assertEquals("Private search without auth should fail with a 403. ", 403, connection.statusCode);
    
        // test private search with auth token
        connection.setAuthToken(authToken);
        result = connection.privateArticlesSearch(testPrivateSearch, 1, 10);
        connection.setAuthToken(null);
        System.out.println("## RETURN DETAILS");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        System.out.println("response:\n"+connection.response);
        System.out.println("responseJSON:\n"+connection.responseJSON);
        System.out.println("responseArrayJSON:\n"+connection.responseArrayJSON);
        assertEquals("Private search with auth should succeed.", 0, result);
        
        // Check returned a value Array or JSONObject's
        if (connection.responseArrayJSON == null)
            fail("Expected response should be a JSONArray, none returned.");
        System.out.println("Item count = "+connection.responseArrayJSON.size());
        if (connection.responseArrayJSON.size()<=0)
            fail("Expected response should have at least 1 item.");
        System.out.println("## ITEMS...");
        for (Object item: connection.responseArrayJSON) {
            if (!(item instanceof JSONObject))
                fail("Expected array item of type JSONObject, instead got: "+item.getClass().getName());
            JSONObject jitem = (JSONObject)item;
            Long id = (Long)jitem.get("id");
            System.out.println("Item ID = "+id);
            try {
               Map map = (Map)jitem;       
               map.forEach((k,v)->System.out.println("Key : " + k + " Value : " + v));
            } catch(Exception e) {
               System.out.println(e);
            }
        }
        
        System.out.println("#### PrivateArticlesSearch end");
        
    }

    /**
     * Test of publicArticlesSearch method, of class FigshareConnection.
     */
    @Test
    public void testPublicArticlesSearch_6args() {
        System.out.println("#### PublicArticlesSearch (with dates) start");
        
        // NOTE: A bit dodgy, but expecting 1 article at 2019-07-14T18:23:39Z
        // check https://api.figshare.com/v2/articles/8869124
        Calendar fromdatec = Calendar.getInstance();
        fromdatec.setTimeZone(TimeZone.getTimeZone("UTC"));
        fromdatec.set(2019, 6, 14, 18, 23, 39);
        Date fromdate = fromdatec.getTime();
        Calendar todatec = Calendar.getInstance();
        todatec.setTimeZone(TimeZone.getTimeZone("UTC"));
        todatec.set(2019, 6, 14, 18, 23, 40);
        Date todate = todatec.getTime();
        
        connection.setAuthToken(null);
        int expResult = 0;
        int result = connection.publicArticlesSearch(testArticleSearch, 1, 10, null, fromdate, todate);
        System.out.println("## RETURN DETAILS");
        System.out.println("statusCode = "+connection.statusCode);
        System.out.println("statusMessage = "+connection.statusMessage);
        System.out.println("errorMessage = "+connection.errorMessage);
        System.out.println("response:\n"+connection.response);
        System.out.println("responseJSON:\n"+connection.responseJSON);
        System.out.println("responseArrayJSON:\n"+connection.responseArrayJSON);
        assertEquals(expResult, result);
        
        // Check returned a value Array or JSONObject's
        if (connection.responseArrayJSON == null)
            fail("Expected response should be a JSONArray, none returned.");
        System.out.println("Item count = "+connection.responseArrayJSON.size());
        if (connection.responseArrayJSON.size()<=0)
            fail("Expected response should have at least 1 item.");
        System.out.println("## ITEMS...");
        for (Object item: connection.responseArrayJSON) {
            if (!(item instanceof JSONObject))
                fail("Expected array item of type JSONObject, instead got: "+item.getClass().getName());
            JSONObject jitem = (JSONObject)item;
            Long id = (Long)jitem.get("id");
            System.out.println("Item ID = "+id);
            try {
               Map map = (Map)jitem;
               map.forEach((k,v)->System.out.println("Key : " + k + " Value : " + v));
            } catch(Exception e) {
               System.out.println(e);
            }
        }
        System.out.println("#### PublicArticlesSearch (with dates) end");
        
    }

    /**
     * Test of publicArticlesSearch method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testPublicArticlesSearch_4args() {
    }

    /**
     * Test of publicArticlesSearch method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testPublicArticlesSearch_3args() {
    }

    /**
     * Test of call method, of class FigshareConnection.
     */
    @Ignore
    @Test
    public void testCall() {
    }

    /**
     * Test of datetimeToFigshareDatetime method, of class FigshareConnection.
     */
    @Test
    public void testDatetimeToFigshareDatetime() {
        System.out.println("#### datetimeToFigshareDatetime start");
        
        Calendar indatec = Calendar.getInstance();
        indatec.setTimeZone(TimeZone.getTimeZone("UTC"));
        indatec.set(2020, 10, 15, 04, 00, 01);
        Date indate = indatec.getTime();
        boolean searchString = false;
        String expResult = "2020-11-15T04:00:01Z";
        String result = FigshareConnection.datetimeToFigshareDatetime(indate, searchString);
        //System.out.println(result);
        assertEquals(expResult, result);

        searchString = true;
        expResult = "15/11/2020T04:00:01Z";
        result = FigshareConnection.datetimeToFigshareDatetime(indate, searchString);
        //System.out.println(result);
        assertEquals(expResult, result);

        indatec.set(2999, 11, 31, 23, 59, 59);
        indate = indatec.getTime();
        searchString = true;
        expResult = "31/12/2999T23:59:59Z";
        result = FigshareConnection.datetimeToFigshareDatetime(indate, searchString);
        //System.out.println(result);
        assertEquals(expResult, result);
        
        indatec.set(9999, 0, 1, 0, 0, 0);
        indate = indatec.getTime();
        searchString = true;
        expResult = "31/12/2999T23:59:59Z";
        result = FigshareConnection.datetimeToFigshareDatetime(indate, searchString);
        //System.out.println(result+ " " +indate.toString());
        assertEquals(expResult, result);
        
        SimpleDateFormat strFormatIn1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        strFormatIn1.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            indate = strFormatIn1.parse("2020-10-15T10:32:45+10");
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        searchString = true;
        expResult = "15/10/2020T00:32:45Z";
        result = FigshareConnection.datetimeToFigshareDatetime(indate, searchString);
        //System.out.println(result+ " " +indate.toString());
        assertEquals(expResult, result);
        
        System.out.println("#### datetimeToFigshareDatetime end");
    }
    
}
