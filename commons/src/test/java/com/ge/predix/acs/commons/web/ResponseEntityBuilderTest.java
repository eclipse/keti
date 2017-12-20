/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package com.ge.predix.acs.commons.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings({ "javadoc", "nls" })
public class ResponseEntityBuilderTest {

    @Test
    public void testCreated() {
        ResponseEntity<Object> created = ResponseEntityBuilder.created();
        Assert.assertNotNull(created);
        Assert.assertNull(created.getBody());
        Assert.assertEquals(created.getStatusCode(), HttpStatus.CREATED);
    }

    @Test
    public void testCreatedWithLocation() {
        ResponseEntity<Object> created = ResponseEntityBuilder.created("/report/1", Boolean.FALSE);

        Assert.assertNotNull(created);
        Assert.assertNull(created.getBody());
        Assert.assertEquals(created.getStatusCode(), HttpStatus.CREATED);

        Assert.assertNotNull(created.getHeaders());
        URI location = created.getHeaders().getLocation();
        Assert.assertEquals(location.getPath(), "/report/1");
    }

    @Test
    public void testUpdatedWithLocation() {
        ResponseEntity<Object> created = ResponseEntityBuilder.created("/report/1", Boolean.TRUE);

        Assert.assertNotNull(created);
        Assert.assertNull(created.getBody());
        Assert.assertEquals(created.getStatusCode(), HttpStatus.NO_CONTENT);

        Assert.assertNotNull(created.getHeaders());
        URI location = created.getHeaders().getLocation();
        Assert.assertEquals(location.getPath(), "/report/1");
    }

    @Test
    public void testOk() {
        ResponseEntity<Object> ok = ResponseEntityBuilder.ok();
        Assert.assertNotNull(ok);
        Assert.assertNull(ok.getBody());
        Assert.assertEquals(ok.getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void testOkWithContent() {
        String content = "PredixRocks";
        ResponseEntity<String> ok = ResponseEntityBuilder.ok(content);
        Assert.assertNotNull(ok);
        Assert.assertEquals(ok.getStatusCode(), HttpStatus.OK);
        Assert.assertEquals(ok.getBody(), "PredixRocks");

    }

    @Test
    public void testDeleted() {
        ResponseEntity<Void> deleted = ResponseEntityBuilder.noContent();
        Assert.assertNotNull(deleted);
        Assert.assertNull(deleted.getBody());
        Assert.assertEquals(deleted.getStatusCode(), HttpStatus.NO_CONTENT);
    }

}
