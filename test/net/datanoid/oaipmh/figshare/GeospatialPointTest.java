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
public class GeospatialPointTest {
    
    public GeospatialPointTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
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
     * Test of parse method, of class GeospatialPoint.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("#### parse");
        String location = "PHL";
        GeospatialPoint result = GeospatialPoint.parse(location);
        System.out.println("point returned: "+result.toString());
        assertEquals("Philippines", result.location);
    }

    /**
     * Test of iso3166AlphaToCountry method, of class GeospatialPoint.
     */
    @Test
    public void testIso3166AlphaToCountry() throws Exception {
        System.out.println("#### iso3166AlphaToCountry");
        String code = "AU";
        String expResult = "Australia";
        String result = GeospatialPoint.iso3166AlphaToCountry(code);
        assertEquals(expResult, result);
        code = "POL";
        expResult = "Poland";
        result = GeospatialPoint.iso3166AlphaToCountry(code);
        assertEquals(expResult, result);

    }

    /**
     * Test of countryToGeospatialPoint method, of class GeospatialPoint.
     */
    @Test
    public void testCountryToGeospatialPoint() throws Exception {
        System.out.println("#### countryToGeospatialPoint");
        String country = "Poland";
        GeospatialPoint result = GeospatialPoint.countryToGeospatialPoint(country);
        System.out.println("point returned: "+result.toString());
        assertEquals(new Double(20.0), new Double(result.longitude));
        assertEquals(new Double(52.0), new Double(result.latitude));
    }
    
}
