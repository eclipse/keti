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
 *******************************************************************************/

package com.ge.predix.acs.cloudfoundry;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.ge.predix.cloudfoundry.client.CloudFoundryConfiguration;

@Profile({ "integration" })
@Configuration
public class AcsCloudFoundryConfiguration {

    @Bean
    ConnectionContext connectionContext(
            @Value("${CF_API_HOST}") final String apiHost,
            @Value("${https.proxyHost}") final String proxyHost,
            @Value("${https.proxyPort}") final Integer proxyPort) {

        return CloudFoundryConfiguration.connectionContext(apiHost, proxyHost, proxyPort);
    }

    @Bean
    PasswordGrantTokenProvider tokenProvider(
            @Value("${cf.password}") final String password,
            @Value("${cf.username}") final String username) {

        return CloudFoundryConfiguration.tokenProvider(password, username);
    }

    @Bean
    CloudFoundryClient cloudFoundryClient(final ConnectionContext connectionContext,
            final TokenProvider tokenProvider) {

        return CloudFoundryConfiguration.cloudFoundryClient(connectionContext, tokenProvider);
    }

    @Bean
    CloudFoundryOperations cloudFoundryOperations(final CloudFoundryClient cloudFoundryClient,
            @Value("${cf.org}") final String organizationName,
            @Value("${cf.space}") final String spaceName) {

        return CloudFoundryConfiguration.cloudFoundryOperations(cloudFoundryClient, organizationName, spaceName);
    }
}
