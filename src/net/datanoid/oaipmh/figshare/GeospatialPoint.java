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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Lyle Winton <lyle@winton.id.au>
 */
public class GeospatialPoint {

    private static final Logger LOG = Logger.getLogger(GeospatialPoint.class.getName());
    
    /**
     * Location name associated with GeospatialPoint.
     */
    public String location;
    
    /**
     * Longitude of point (CRS is WGS84 EPSG:4326 https://epsg.io/4326).
     */
    public double longitude;

    /**
     * Latitude of point (CRS is WGS84 EPSG:4326 https://epsg.io/4326).
     */
    public double latitude;
    
    /**
     * Create point with dummy long lat.
     */
    public GeospatialPoint() {
        longitude = 9999;
        latitude = 9999;
        location = null;
    }

    /**
     * Create point with given long lat.
     * @param lon Longitude in CRS WGS84 EPSG:4326 https://epsg.io/4326
     * @param lat Latitude in CRS WGS84 EPSG:4326 https://epsg.io/4326
     */
    public GeospatialPoint(double lon, double lat) {
        longitude = lon;
        latitude = lat;
        location = null;
    }

    /**
     * Create point with given long lat.
     * @param loc Text name for the location.
     * @param lon Longitude in CRS WGS84 EPSG:4326 https://epsg.io/4326
     * @param lat Latitude in CRS WGS84 EPSG:4326 https://epsg.io/4326
     */
    public GeospatialPoint(String loc, double lon, double lat) {
        longitude = lon;
        latitude = lat;
        location = loc;
    }

    /**
     * Create geospatial point with given some location name or encoding.
     * Supports:
     *  conversion of ISO3166 country alpha codes and names;
     *  ???
     * @param location String value of longitude
     * @return GeospatialPoint of the identified location.
     * @throws ParseException If no location can be identified.
     */
    public static GeospatialPoint parse(String location) throws ParseException {
        // ISO3166 country codes
        String pattern="[a-zA-Z]{2,3}";
        if (location.matches(pattern)) {
            // guess ISO3166 2 or 3 letter country code
            try {
                String country = iso3166AlphaToCountry(location);
                return countryToGeospatialPoint(country);
            } catch (ParseException ex) {
                // try the next guess
            }
        }

        //TODO
        // DCMI-Point "name=Perth, W.A.; east=115.85717; north=-31.95301"
        pattern = "east=(-*[0-9.]+);";
        pattern = "north=(-*[0-9.]+);";
        pattern = "name=([^;]+);"; // optional
        
        // TODO
        // DCMI-Box "name=Western Australia; northlimit=-13.5; southlimit=-35.5; westlimit=112.5; eastlimit=129"
        
        // Old style written degrees,minutes,seconds
        // Old style sub-pattern "23° 26' 21''" "21°09′12″"
        String pattern_old = "(\\d+)(°|deg|degrees)\\s*(\\d+)('|′)\\s*(\\d+)(\"|″|''|′′)";
        
        // "S 21°09′12″ E 149°09′56″"
        pattern = "(N|S|E|W)\\s+"+pattern_old+"[\\s,]+(N|S|E|W)\\s+"+pattern_old;
        // "21°09′12″ N 149°09′56″ W"
        pattern = pattern_old+"\\s+(N|S|E|W)[\\s,]+"+pattern_old+"\\s+(N|S|E|W)";
        // "23° 26' 21'' N" try a single lat or long
        pattern = pattern_old+"\\s+(N|S|E|W)";
        
        
        // TODO
        // Getty TGN
        // Page Link: http://vocab.getty.edu/page/tgn/7001959
        // perhaps TGN:1234567
        // JSON http://vocab.getty.edu/tgn/7001959.json
        pattern = "http://vocab.getty.edu/tgn/(\\d{2,8})";
        pattern = "http://vocab.getty.edu/page/tgn/(\\d{2,8})";
        pattern = "TGN:(\\d{2,8})";
        /*
        {
      "Subject" : {
        "type" : "uri",
        "value" : "http://vocab.getty.edu/tgn/term/98378"
      },
      "Predicate" : {
        "type" : "uri",
        "value" : "http://vocab.getty.edu/ontology#term"
      },
      "Object" : {
        "type" : "literal",
        "value" : "Mackay"
      }
      ...
      {
      "Subject" : {
        "type" : "uri",
        "value" : "http://vocab.getty.edu/tgn/7001959-place"
      },
      "Predicate" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2003/01/geo/wgs84_pos#lat"
      },
      "Object" : {
        "datatype" : "http://www.w3.org/2001/XMLSchema#decimal",
        "type" : "literal",
        "value" : "-21.15345"
       }, {
      "Subject" : {
        "type" : "uri",
        "value" : "http://vocab.getty.edu/tgn/7001959-place"
      },
      "Predicate" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2003/01/geo/wgs84_pos#long"
      },
      "Object" : {
        "datatype" : "http://www.w3.org/2001/XMLSchema#decimal",
        "type" : "literal",
        "value" : "149.165543"
      }
        */
        
        
        // GeoNames????
        // rdf:about="https://sws.geonames.org/2159220/"  eg. Mackay
        // rdf:resource="https://sws.geonames.org/2159220/about.rdf"
        //  contains:
        //   <gn:name>Mackay</gn:name>
        //   <wgs84_pos:lat>-21.15345</wgs84_pos:lat>
        //   <wgs84_pos:long>149.16554</wgs84_pos:long>
        // rdf:resource="https://www.geonames.org/2159220/mackay.html"
        // link http://www.geonames.org/2159220/mackay.html
        pattern="https://(www|sws).geonames.org/(\\d+)/";
        String code="2159220";
        String url = "https://sws.geonames.org/"+code+"/about.rdf";

        // Last resort...
        // WGS84 EPSG:4326 https://epsg.io/4326
        // eg. Melbourne "144.962311 -37.814726", looks like East North
        pattern="((-*[0-9.]+)[\\s,]+(-*[0-9.]+)";
        
        return new GeospatialPoint();
    }

    /**
     * Parse a string value as a Longitude or Latitude.
     * @param value String value of longitude
     * @return The floating point value of the passed value.
     * @throws ParseException If no value can be identified.
     */
    public static double parseLongitude(String value) throws ParseException {
        // Old style sub-pattern "23° 26' 21''" "21°09′12″"
        String pattern_old = "(\\d+)(°|deg|degrees)\\s*(\\d+)('|′)\\s*(\\d+)(\"|″|''|′′)";
        // "S 21°09′12″ E 149°09′56″"
        String pattern = "(N|S|E|W)\\s+"+pattern_old+"[\\s,]+(N|S|E|W)\\s+"+pattern_old;
        // "21°09′12″ N 149°09′56″ W"
        pattern = pattern_old+"\\s+(N|S|E|W)[\\s,]+"+pattern_old+"\\s+(N|S|E|W)";
        // "23° 26' 21'' N" try a single lat or long
        pattern = pattern_old+"\\s+(N|S|E|W)";
        
        // "25 00 00 S degrees minutes"
        pattern = pattern_old+"(\\d+)\\s(\\d+)\\s(\\d+)\\s(N|S|E|W)";
        
        // straight number
        return Double.parseDouble(value);
    }
    
    /*
     * Configuration and storage for ISO3166.
     * Resource file obtained from:
     *  https://github.com/lukes/ISO-3166-Countries-with-Regional-Codes
     *  Licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
     * Resource file obtained from:
     *  https://github.com/eesur/country-codes-lat-long
     *  MIT License, Copyright (c) 2022 Sundar Singh
    */
    private static String iso3166resource = "resources/iso3166countries.json";
    private static String iso3166latlongresource = "resources/iso3166country-codes-lat-long-alpha3.json";
    private static HashMap alpha2ToCountry = null;
    private static HashMap alpha3ToCountry = new HashMap();
    private static HashMap countryToAlpha2 = new HashMap();
    private static HashMap countryToPoint = new HashMap();

    /**
     * Return country name of a given a 2 or 3 letter country code. 
     * @param code 2 or 3 letter country code.
     * @return country name.
     * @throws ParseException if no country could be identified.
     */
    public static String iso3166AlphaToCountry(String code) throws ParseException {
        if ((alpha2ToCountry==null) || (alpha3ToCountry==null)) {
            // load country codes
            alpha2ToCountry = new HashMap();
            alpha3ToCountry = new HashMap();
            InputStreamReader reader = new InputStreamReader( GeospatialPoint.class.getResourceAsStream(iso3166resource) );
            JSONParser parser = new JSONParser();
            Object obj;
            try {
                obj = parser.parse(reader);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "ERROR: Cannot file resource "+iso3166resource, ex);
                return null;
            } catch (org.json.simple.parser.ParseException ex) {
                LOG.log(Level.SEVERE, "ERROR: Cannot JSON parse file resource "+iso3166resource, ex);
                return null;
            }
            if (!(obj instanceof JSONArray)) {
                LOG.log(Level.SEVERE, "ERROR: Resource "+iso3166resource+" did not return JSON array.");
                return null;
            }
            JSONArray jarray = (JSONArray)obj;
            for (Object item: jarray) {
                if (!(item instanceof JSONObject))
                    LOG.log(Level.WARNING, "WARNING: JSON array item is not a JSON item."+item.toString());
                JSONObject jitem = (JSONObject)item;
                String name = (String)jitem.get("name");
                String a2 = (String)jitem.get("alpha-2");
                String a3 = (String)jitem.get("alpha-3");
                alpha2ToCountry.put(a2.toUpperCase(), name);
                alpha2ToCountry.put(a3.toUpperCase(), name);
                countryToAlpha2.put(name.toLowerCase(), a2.toUpperCase());
                // Insert dummy country lon/lat flagging needs to load
                countryToPoint.put(name.toLowerCase(), new GeospatialPoint());
            }
            // load country lat long
            reader = new InputStreamReader( GeospatialPoint.class.getResourceAsStream(iso3166latlongresource) );
            try {
                obj = parser.parse(reader);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "ERROR: Cannot file resource "+iso3166latlongresource, ex);
                return null;
            } catch (org.json.simple.parser.ParseException ex) {
                LOG.log(Level.SEVERE, "ERROR: Cannot JSON parse file resource "+iso3166latlongresource, ex);
                return null;
            }
            if (!(obj instanceof JSONArray)) {
                LOG.log(Level.SEVERE, "ERROR: Resource "+iso3166latlongresource+" did not return JSON array.");
                return null;
            }
            jarray = (JSONArray)obj;
            for (Object item: jarray) {
                if (!(item instanceof JSONObject))
                    LOG.log(Level.WARNING, "WARNING: JSON array item is not a JSON item."+item.toString());
                JSONObject jitem = (JSONObject)item;
                String name = (String)jitem.get("country");
                double lat = 9999;
                double lon = 9999;                
                Object o = jitem.get("latitude");
                if (o instanceof Long)
                    lat = (Long)o * 1.0;
                else if (o instanceof Double)
                    lat = (Double)o * 1.0;
                else
                    System.out.println("LYLE:" + o.getClass().getName());
                o = jitem.get("longitude");
                if (o instanceof Long)
                    lon = (Long)o * 1.0;
                else if (o instanceof Double)
                    lon = (Double)o * 1.0;
                else
                    System.out.println("LYLE:" + o.getClass().getName());
                countryToPoint.put(name.toLowerCase(), new GeospatialPoint(name,lon,lat));
            }
        }
        if (alpha2ToCountry.get(code.toUpperCase())!=null)
            return (String)alpha2ToCountry.get(code);
        if (alpha3ToCountry.get(code.toUpperCase())!=null)
            return (String)alpha3ToCountry.get(code);
        throw new ParseException("Code '"+code+"' does not match any 2 or 3 digit ISO3166 country code.",0);
    }

    /**
     * Returns GeopatialPoint given an ISO3166 country name.
     * @param country ISO3166 country name.
     * @return GeopatialPoint point of identified country or null if no country.
     * @throws ParseException if not an identified country with lon/lat.
     */
    public static GeospatialPoint countryToGeospatialPoint(String country) throws ParseException {
        if ((alpha2ToCountry==null) || (alpha3ToCountry==null)) {
            // first call, use dummy call to trigger a load of country info
            iso3166AlphaToCountry("AU");
        }
        Object point = countryToPoint.get(country.toLowerCase());
        if (point!=null) {
            GeospatialPoint gpoint = (GeospatialPoint)point;
            if (gpoint.longitude>=9999)
                throw new ParseException("Country '"+country+"' does not have a latitude and longitude.",0);
            return gpoint;
        }
        throw new ParseException("Country '"+country+"' is not an idenfied ISO3166 country name.",0);
    }
    
    public String toString() {
        String out = "";
        if (location != null)
            out = out + "name="+location+"; ";
        if (longitude<9999)
            out = out + "east="+longitude+"; ";
        if (latitude<9999)
            out = out + "north="+latitude+"";
        return out;
    }
    
}
