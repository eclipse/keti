package com.ge.predix.acs.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.privilege.management.dao.GraphResourceRepository;
import com.ge.predix.acs.privilege.management.dao.TitanMigrationManager;
import com.thinkaurelius.titan.core.QueryException;
import com.thinkaurelius.titan.core.TitanConfigurationException;
import com.thinkaurelius.titan.diskstorage.ResourceUnavailableException;

@Component
@Profile({ "titan" })
public class GraphDbHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphDbHealthIndicator.class);
    private static final String ERROR_MESSAGE_FORMAT = "Unexpected exception while checking graph database status: {}";
    static final String DESCRIPTION = "Health check performed by attempting to create a version vertex and retrieve it "
            + "from the underlying graph store";

    private final GraphResourceRepository resourceHierarchicalRepository;

    @Value("${TITAN_ENABLE_CASSANDRA:false}")
    private boolean cassandraEnabled;

    @Autowired
    public GraphDbHealthIndicator(final GraphResourceRepository resourceHierarchicalRepository) {
        this.resourceHierarchicalRepository = resourceHierarchicalRepository;
    }

    @Override
    public Health health() {
        Health health = AcsMonitoringUtilities.health(this::check, DESCRIPTION);
        Status status = health.getStatus();
        if (status.equals(Status.UP) && !this.cassandraEnabled) {
            health = AcsMonitoringUtilities.health(status, AcsMonitoringUtilities.HealthCode.IN_MEMORY, DESCRIPTION);
        }
        return health;
    }

    private AcsMonitoringUtilities.HealthCode check() {
        AcsMonitoringUtilities.HealthCode healthCode = AcsMonitoringUtilities.HealthCode.ERROR;

        try {
            LOGGER.debug("Checking graph database status");
            if (this.resourceHierarchicalRepository
                    .checkVersionVertexExists(TitanMigrationManager.INITIAL_ATTRIBUTE_GRAPH_VERSION)) {
                healthCode = AcsMonitoringUtilities.HealthCode.AVAILABLE;
            }
        } catch (QueryException e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.INVALID_QUERY, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        } catch (ResourceUnavailableException e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.UNAVAILABLE, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        } catch (TitanConfigurationException e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.MISCONFIGURATION, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        } catch (Exception e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.ERROR, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        }

        return healthCode;
    }
}
