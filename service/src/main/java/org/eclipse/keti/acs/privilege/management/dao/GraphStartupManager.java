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

package org.eclipse.keti.acs.privilege.management.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;

@Component
@Profile("graph")
public final class GraphStartupManager {
    public static final int INITIAL_ATTRIBUTE_GRAPH_VERSION = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStartupManager.class);

    @Autowired
    @Qualifier("resourceHierarchicalRepository")
    private GraphResourceRepository resourceHierarchicalRepository;
    
    private Boolean isStartupComplete = false;


    @PostConstruct
    public void doStartup() {
        // This version vertex is common to both subject and resource repositories. So this check is sufficient to
        // trigger startups in both repos.
        if (!this.resourceHierarchicalRepository.checkVersionVertexExists(INITIAL_ATTRIBUTE_GRAPH_VERSION)) {

            // Startup needs to be performed in a separate thread to prevent cloud-foundry health check timeout,
            // which restarts the service. (Max timeout is 180 seconds which is not enough)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.info("Starting attribute startup process to graph.");
                        //Create version vertex, to record completion.
                        resourceHierarchicalRepository.createVersionVertex(INITIAL_ATTRIBUTE_GRAPH_VERSION);
                        isStartupComplete = true;
                        LOGGER.info("Graph attribute startup complete. Created version: "
                                + INITIAL_ATTRIBUTE_GRAPH_VERSION);
                    } catch (Exception e) {
                        LOGGER.error("Exception during attribute startup: ", e);
                    }
                }
            });
        } else {
            isStartupComplete = true;
            LOGGER.info("Attribute Graph startup not required.");
        }
    }

    public boolean isStartupComplete() {
        return this.isStartupComplete;
    }

}
