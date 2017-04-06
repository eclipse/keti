package com.ge.predix.cloudfoundry.client;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;

public final class CloudFoundryConfiguration {

    private CloudFoundryConfiguration() {
        throw new AssertionError();
    }

    public static ConnectionContext connectionContext(final String apiHost, final String proxyHost,
            final Integer proxyPort) {

        DefaultConnectionContext.Builder connectionContext = DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .skipSslValidation(false);

        if (StringUtils.isNotEmpty(proxyHost)) {
            ProxyConfiguration.Builder proxyConfiguration = ProxyConfiguration.builder()
                    .host(proxyHost)
                    .port(proxyPort);

            connectionContext.proxyConfiguration(proxyConfiguration.build());
        }

        return connectionContext.build();
    }

    public static PasswordGrantTokenProvider tokenProvider(final String password, final String username) {
        return PasswordGrantTokenProvider.builder()
                .password(password)
                .username(username)
                .build();
    }

    public static CloudFoundryClient cloudFoundryClient(final ConnectionContext connectionContext,
            final TokenProvider tokenProvider) {

        return ReactorCloudFoundryClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
    }

    public static CloudFoundryOperations cloudFoundryOperations(final CloudFoundryClient cloudFoundryClient,
            final String organizationName, final String spaceName) {

        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cloudFoundryClient)
                .organization(organizationName)
                .space(spaceName)
                .build();
    }
}
