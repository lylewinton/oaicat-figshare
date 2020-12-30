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
package net.datanoid.figshare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Class to manage figshare API calls.
 * 
 * Pattern:
 *   For private calls set your Personal Token using setAuthToken() first.
 *   Call a private/public method which returns 0 if completed without error.
 *   If an error you can check the returned statusCode or errorMessage.
 *   If not an error, the results will be, depending on the call, either:
 *      responseJSON - a single JSONObject
 *      responseArrayJSON - an array of objects (a JSONArray)
 *   (If all else fails "response" holds the full text response.)
 * 
 *   eg. 
 *      FigshareConnection connection = FigshareConnection.getInstance();
 *      int ret = connection.privateArticlesSearch(":institution: melbourne", 1, 10);
 *      if (ret == 1)
 *          System.err.println("ERROR: "+connection.errorMessage);
 *      else
 *          System.err.println("Found count="+connection.responseArrayJSON.size());
 * 
 * TODO /account/projects/{project_id}/articles
 * TODO /account/projects/search
 * TODO /account/projects
 * TODO /account/projects/{project_id}/collaborators
 * UNSURE
 * TODO /account/institution/articles (admin, limited)
 * TODO /account/articles/{article_id} (if admin, can see???)
 * LATER
 * TODO /account/institution/accounts/search (admin)
 * TODO /account/articles/search :project: project_name (which impersonated can edit)
 * LISTS for entry
 * TODO /account/authors/search
 * TODO /account/authors/{author_id}
 * TODO /account/categories
 * TODO /account/institution (just get the current institution value)
 * TODO /account/licenses
 * TODO /account/funding/search
 * 
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class FigshareConnection {

    private static final Logger LOG = Logger.getLogger(FigshareConnection.class.getName());
    private String authorization = null;
    private int readTimeout = 30000;
    private int retryCount = 0;
    private String apiURI = "https://api.figshare.com/v2";
    private static String apiURIsecure = "https://api.figshare.com/v2";
    private static String apiURIinsecure = "http://api.figshare.com/v2";

    /**
     * Returns the last call return value, 0 is success.
     */
    public int lastError = 0;

    /**
     * Returns the HTTP returned status code of the last call.
     * See manual for action context. https://docs.figshare.com/
     * 0 = call failed ;
     * 200 = OK for some actions ;
     * 201-209 = OK for some actions ;
     * 400 = bad request ;
     * 422 = invalid data structure sent / incorrect params ;
     * 401 = authorisation failed ;
     * 403 = no permission / forbidden ; 
     * 404 = no permission or resource doesn't exist ; 
     * 500 = internal server error ;
     */
    public int statusCode = 0;
    public String statusMessage = null;
    public String errorMessage = null;
    public String response = null;
    public JSONObject responseJSON = null;
    public JSONArray responseArrayJSON = null;
    
    public FigshareConnection() {
    }
    
    public void setAuthToken(String authToken) {
        if ( (authToken == null) || (authToken.length()==0) )
            this.authorization = null;
        else
            this.authorization = "token "+authToken;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    /**
     * Ensure connections are HTTPS.
     */
    public void setSecureConnection() {
        apiURI = apiURIsecure;
    }
    
    /**
     * Ensure connections are HTTP not HTTPS.
     * There's some evidence that Java's automatic keep-alive connection pooling
     * doesn't would for HTTPS.
     */
    public void setInsecureConnection() {
        apiURI = apiURIinsecure;
    }
    
    /**
     * Set a retry count
     * @param count number of retries, zero is default.
     */
    public void setRetryCount(int count) {
        retryCount = count;
    }
    
    /**
     * Logged in get article details.
     * @param articleID Article ID.
     * @return 0 for success, -1 HTTP error, 1 figshare error, 2 item not found.
     */
    public int privateArticleDetails(Long articleID) {
        int ret = this.call("GET","/account/articles/"+articleID, null);
        if ( (ret==0) && (statusCode == 404) )
            return 2;
        if ( (ret==0) && (statusCode != 200) )
            return 1;
        return ret;
    }

    /**
     * Get public article details.
     * @param articleID Article ID.
     * @return 0 for success, -1 HTTP error, 1 figshare error, 2 item not found.
     */
    public int pulbicArticleDetails(Long articleID) {
        int ret = this.call("GET","/articles/"+articleID, null);
        if ( (ret==0) && (statusCode == 404) )
            return 2;
        if ( (ret==0) && (statusCode != 200) )
            return 1;
        return ret;
    }

    /**
     * Logged in list articles in a given project.
     * @param projectID Project ID.
     * @param page Requested page starting with 1.
     * @param page_size Number of articles returned each page.
     * @return 0 for success, -1 HTTP error, 1 figshare error, 2 project not found.
     */
    public int privateProjectArticles(Long projectID, int page, int page_size) {
        String q = "?page="+page+"&page_size="+page_size;
        int ret = this.call("GET","/account/projects/"+projectID.toString()+"/articles"+q, null);
        if ( (ret==0) && (statusCode == 404) )
            return 2;
        if ( (ret==0) && (statusCode != 200) )
            return 1;
        return ret;
    }

    /**
     * Logged in get project article details.
     * @param projectID Project ID.
     * @param articleID Article ID.
     * @return 0 for success, -1 HTTP error, 1 figshare error, 2 item not found.
     */
    public int privateProjectArticleDetails(Long projectID, Long articleID) {
        int ret = this.call("GET","/account/projects/"+projectID+"/articles/"+articleID, null);
        if ( (ret==0) && (statusCode == 404) )
            return 2;
        if ( (ret==0) && (statusCode != 200) )
            return 1;
        return ret;
    }

    /**
     * Logged in search for projects via structured/text query string.
     * @param query Structured query string normally used via the web (https://docs.figshare.com/#search_how_to_find_data_on_figshare).
     * @param page Requested page starting with 1.
     * @param page_size Number of articles returned each page.
     * @return 0 for success, -1 HTTP error, 1 figshare error.
     */
    public int privateProjectSearch(String query, int page, int page_size) {
        JSONObject data = new JSONObject();
        data.put("search_for", query);
        data.put("page", new Integer(page));
        data.put("page_size", new Integer(page_size));
        data.put("order", "modified_date"); // published_date and modified_date
        data.put("order_direction", "desc");
        int ret = this.call("POST","/account/projects/search", data);
        if ( (ret==0) && (statusCode != 200) )
            return 1;
        return ret;
    }

    /**
     * Logged in search for articles via structured query string.
     * @param query Structured query string normally used via the web (https://docs.figshare.com/#search_how_to_find_data_on_figshare).
     * @param page Requested page starting with 1.
     * @param page_size Number of articles returned each page.
     * @param map Any additional params to send to figshare, or null if none. Can be used to override order/order_direction.
     * @return 0 for success, -1 HTTP error, 1 figshare error.
     */
    public int privateArticlesSearch(String query, int page, int page_size, Map inputs) {
        JSONObject data = new JSONObject();
        data.put("search_for", query);
        data.put("page", new Integer(page));
        data.put("page_size", new Integer(page_size));
        data.put("order", "modified_date"); // published_date and modified_date
        data.put("order_direction", "desc");
        if (inputs != null) {
            inputs.forEach((k,v)-> data.put(k,v) );
        }
        int ret = this.call("POST","/account/articles/search", data);
        if ( (ret==0) && (statusCode != 200) )
            return 1;
        return ret;
    }
    /**
     * Logged in search for articles via structured query string.
     */
    public int privateArticlesSearch(String query, int page, int page_size) {
        return privateArticlesSearch(query,page,page_size,null);
    }

    /**
     * Public search for articles via structured query string.
     * @param query Structured query string normally used via the web (https://docs.figshare.com/#search_how_to_find_data_on_figshare).
     * @param page Requested page starting with 1.
     * @param page_size Number of articles returned each page.
     * @param map Any additional params to send to figshare, or null if none. Can be used to override order/order_direction.
     * @return 0 for success, -1 HTTP error, 1 figshare error.
     */
    public int publicArticlesSearch(String query, int page, int page_size, Map inputs) {
        JSONObject data = new JSONObject();
        data.put("search_for", query);
        data.put("page", new Integer(page));
        data.put("page_size", new Integer(page_size));
        data.put("order", "modified_date");
        data.put("order_direction", "desc");
        if (inputs != null) {
            inputs.forEach((k,v)-> data.put(k,v) );
        }
        int ret = this.call("POST","/articles/search", data);
        if ( (ret==0) && (statusCode != 200) )
            return 1;
        return ret;
    }

    /**
     * Public search for articles via structured query string.
     */
    public int publicArticlesSearch(String query, int page, int page_size) {
        return publicArticlesSearch(query,page,page_size,null);
    }

    /**
     * Generic HTTP method call with JSON input data, and expected JSON output.
     * Whatever is needed for https://docs.figshare.com/
     * @param method POST, GET, PUT
     * @param path Path of the API call after https://api.figshare.com/v2
     * @param data Data sent as part of the API call.
     * @return
     */
    public int call(String method, String path, JSONObject data) {
        // TODO check if path is a full URL
        // TODO implement Impersonation see figshare docs

        URL url;
        HttpURLConnection urlConnection = null;
        responseJSON = new JSONObject();
        responseArrayJSON = new JSONArray();
        int countdown = 1 + retryCount;
        for (int thistry=countdown; thistry>0; thistry--) {
            boolean retryable = false; // only some errors are worth a retry
            statusCode = 0;
            statusMessage = null;
            errorMessage = null;
            lastError = -1;

            try {
                url = new URL(apiURI + path);
                LOG.log(Level.FINE, "call() connecting to "+url.toString());

                urlConnection = (HttpURLConnection) url.openConnection(); // tends to succeed, so no real connection happening here
                LOG.log(Level.FINER, "call() connected...");
                // NB: keepalive connection pooling is automatic,
                //  if getInputStream() and getErrorStream() are closed.
                //  Ref: https://docs.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html
                // NB2: some evidence this doesn't would for HTTPS
                if (authorization != null)
                    urlConnection.setRequestProperty ("Authorization", authorization);
                urlConnection.setRequestMethod(method);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setUseCaches(false);
                urlConnection.setDoInput(true);

                // send JSON data
                if (data != null) {
                    urlConnection.setDoOutput(true);
                    LOG.log(Level.FINER, "call() sending data...");
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
                    bw.write(data.toString());
                    bw.flush();
                    bw.close();
                } else {
                    urlConnection.setDoOutput(false);
                }

                // now handle response, possibly normal page, possibly error page, possibly JSON
                BufferedReader br = null;
                try {
                    LOG.log(Level.FINER, "call() getting InputStream response...");
                    br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    response = br.lines().collect(Collectors.joining());
                } catch (IOException e) {
                    LOG.log(Level.FINER, "call() IOException, getting ErrorStream response...");
                    InputStream is = urlConnection.getErrorStream();
                    if (is == null)
                        throw e; // no error response, likely a serious problem
                    br = new BufferedReader(new InputStreamReader(is));
                    response = br.lines().collect(Collectors.joining());
                    errorMessage = response;
                    LOG.log(Level.FINER, "call() errorMessage="+response);
                } finally {
                    if (br!=null) br.close();
                }
                try {
                    LOG.log(Level.FINER, "call() attempting to parse JSON...");
                    JSONParser parser = new JSONParser();
                    Object obj = parser.parse(response);
                    // TODO check if JSONArray or JSONObject?
                    if (obj instanceof JSONObject) {
                        LOG.log(Level.FINER, "call() found JSONObject");
                        responseJSON = (JSONObject)obj;
                    }
                    if (obj instanceof JSONArray) {
                        LOG.log(Level.FINER, "call() found JSONArray");
                        responseArrayJSON = (JSONArray)obj;
                    }
                }catch(ParseException pe) {
                    LOG.log(Level.FINE, "call() ParseException: "+pe.toString());
                    LOG.log(Level.FINE, "... on response="+response);
                    if (errorMessage == null)
                        errorMessage = pe.getMessage();
                    else 
                        errorMessage = pe.getMessage() + "\nMessage: " + errorMessage;
                }

                // HTTP Response Headers aren't useful
                /*
                Map map = urlConnection.getHeaderFields();
                map.forEach((k,v)->System.out.println("Header : " + k + " Value : " + v));
                */

                lastError = 0;

            } catch (IOException e) {
                LOG.log(Level.FINE, "call() IOException: "+e.getMessage());
                errorMessage = e.getMessage();
                lastError = -1;
                retryable = true;
            } catch (Exception e) {
                LOG.log(Level.FINE, "call() "+e.getClass().getName()+": "+e.getMessage());
                errorMessage = e.getMessage();
                lastError = -1;
            } finally {
                LOG.log(Level.FINER, "call() attempting to get response code and message...");
                if (urlConnection != null) {
                    try {
                    statusCode = urlConnection.getResponseCode();
                    } catch (IOException e) {
                        LOG.log(Level.FINER, "call() response code IOException:"+e.getMessage());
                        retryable = true;
                    }
                    try {
                    statusMessage = urlConnection.getResponseMessage();
                    } catch (IOException e) {
                        LOG.log(Level.FINER, "call() response message IOException:"+e.getMessage());
                        retryable = true;
                    }
                    LOG.log(Level.FINER, "call() response code="+statusCode+" message="+statusMessage);
                    //urlConnection.disconnect();  // do not disconnect for keep-alive
                }
            }
            
            if (!retryable) break;
            if (thistry>1) {
                LOG.log(Level.FINE, "call() retrying, sleeping, countdown="+thistry);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
            }
        }
        LOG.log(Level.FINE, "call() return="+lastError);
        return lastError;
    }
}

