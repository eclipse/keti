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

package com.ge.predix.acs.audit;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.ge.predix.audit.sdk.AuditCallback;
import com.ge.predix.audit.sdk.AuditClient;
import com.ge.predix.audit.sdk.AuditClientType;
import com.ge.predix.audit.sdk.FailReport;
import com.ge.predix.audit.sdk.config.AuditConfiguration;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.sdk.message.AuditEvent;
import com.ge.predix.audit.sdk.validator.ValidatorReport;
import com.ge.predix.eventhub.EventHubClientException;

@Configuration
@Profile("predixAudit")
public class ACSAuditConfiguration {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ACSAuditConfiguration.class);

    private static final VcapLoaderServiceImpl VCAP_LOADER_SERVICE = new VcapLoaderServiceImpl();

    @Value("${AUDIT_UAA_URL}")
    private String auditUaaUrl;

    @Value("${AUDIT_UAA_CLIENT_ID}")
    private String auditUaaClientId;

    @Value("${AUDIT_UAA_CLIENT_SECRET}")
    private String auditUaaClientSecret;

    @Bean
    public AuditClient auditClient() throws AuditException, EventHubClientException {
        AuditConfiguration sdkConfig;
        try {
            sdkConfig = ACSAuditConfiguration.VCAP_LOADER_SERVICE.getConfigFromVcap();
            sdkConfig.setClientType(AuditClientType.ASYNC);
            sdkConfig.setUaaUrl(this.auditUaaUrl);
            sdkConfig.setUaaClientId(this.auditUaaClientId);
            sdkConfig.setUaaClientSecret(this.auditUaaClientSecret);

        } catch (Exception e) {
            LOGGER.error("Can not create Audit Client bean due to exception", e);
            return null;
        }
        return new AuditClient(sdkConfig, new AcsAuditCallBack());
    }

    private static class AcsAuditCallBack implements AuditCallback {
        @Override
        public void onFailure(final AuditEvent arg0, final FailReport arg1, final String arg2) {
            LOGGER.info("AUDIT EVENT FAILED: {}", arg0);
            LOGGER.info("AUDIT FAIL REPORT: {}", arg1);
        }

        @Override
        public void onSuccees(final AuditEvent arg0) {
            LOGGER.debug("AUDIT EVENT SUCCESS: {}", arg0);
        }

        @Override
        public void onValidate(final AuditEvent arg0, final List<ValidatorReport> arg1) {
            LOGGER.info("AUDIT EVENT VALIDATE: {}", arg0);
            for (ValidatorReport report : arg1) {
                LOGGER.info("AUDIT ValidatorReport: {}", report);
            }
        }

        @Override
        public void onFailure(final FailReport arg0, final String arg1) {
            LOGGER.info("AUDIT EVENT FAILED: {}", arg0);
            LOGGER.info("AUDIT FAIL REPORT: {}", arg1);
        }
    }

}
