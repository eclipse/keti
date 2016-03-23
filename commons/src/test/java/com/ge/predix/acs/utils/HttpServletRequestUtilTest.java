/*******************************************************************************
 * Copyright 2016 General Electric Company. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.acs.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HttpServletRequestUtilTest {

    @Test
    public void getSubdomain() {
        String hostname = "groot.acs.ge.com";
        String domain = "acs.ge.com";
        String actualSubdomain = HttpServletRequestUtil.getSubdomain(hostname, domain);
        String expectedSubdomain = "groot";
        Assert.assertEquals(actualSubdomain, expectedSubdomain);
    }

    @Test
    public void getSubdomainForEmptyDomain() {
        String hostname = "groot.";
        String domain = "";
        String actualSubdomain = HttpServletRequestUtil.getSubdomain(hostname, domain);
        String expectedSubdomain = "groot";
        Assert.assertEquals(actualSubdomain, expectedSubdomain);
    }

    @Test
    public void getSubdomainDots() {
        String hostname = "i.am.groot.acs.ge.com";
        String domain = "acs.ge.com";
        String actualSubdomain = HttpServletRequestUtil.getSubdomain(hostname, domain);
        String expectedSubdomain = "i.am.groot";
        Assert.assertEquals(actualSubdomain, expectedSubdomain);
    }

    @Test
    public void getSubdomainExactMatch() {
        String hostname = "acs.ge.com";
        String domain = "acs.ge.com";
        String actualSubdomain = HttpServletRequestUtil.getSubdomain(hostname, domain);
        // If no sub-domain is provided, map to the default zone's sub-domain
        // for now. This behavior is yet to be
        // finalized. -- see US29230
        Assert.assertEquals(actualSubdomain, "");
    }

    public void getSubdomainNoMatch() {
        String hostname = "groot";
        String domain = "acs.ge.com";
        Assert.assertNull(HttpServletRequestUtil.getSubdomain(hostname, domain));
    }

    public void getSubdomainNoDots() {
        String hostname = "grootacs.ge.com";
        String domain = "acs.ge.com";
        Assert.assertNull(HttpServletRequestUtil.getSubdomain(hostname, domain));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getSubdomainNullDomain() {
        String hostname = "grootacs.ge.com";
        String domain = null;
        HttpServletRequestUtil.getSubdomain(hostname, domain);
    }

    @Test
    public void getSubdomainEmptyDomain() {
        String hostname = "grootacs.ge.com";
        String domain = "";
        Assert.assertNull(HttpServletRequestUtil.getSubdomain(hostname, domain));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getSubdomainNullHostname() {
        String hostname = null;
        String domain = "acs.ge.com";
        HttpServletRequestUtil.getSubdomain(hostname, domain);
    }

    public void getSubdomainEmptyHostname() {
        String hostname = "";
        String domain = "acs.ge.com";
        Assert.assertNull(HttpServletRequestUtil.getSubdomain(hostname, domain));
    }
}
