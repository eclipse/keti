package com.ge.predix.acs.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
// Modified version of org.springframework.boot.actuate.health.OrderedHealthAggregator
public class AcsHealthAggregator implements HealthAggregator {

    static final Status DEGRADED_STATUS = new Status(AcsMonitoringUtilities.HealthCode.DEGRADED.toString());

    private List<String> statusOrder;

    AcsHealthAggregator() {
        this.setStatusOrder(Status.DOWN, Status.OUT_OF_SERVICE, DEGRADED_STATUS, Status.UP, Status.UNKNOWN);
    }

    void setStatusOrder(final Status... statusOrder) {
        String[] order = new String[statusOrder.length];
        for (int i = 0; i < statusOrder.length; ++i) {
            order[i] = statusOrder[i].getCode();
        }
        this.setStatusOrder(Arrays.asList(order));
    }

    void setStatusOrder(final List<String> statusOrder) {
        Assert.notNull(statusOrder, "StatusOrder must not be null");
        this.statusOrder = statusOrder;
    }

    @Override
    public final Health aggregate(final Map<String, Health> healths) {
        List<Status> statusCandidates = new ArrayList<>();
        for (Map.Entry<String, Health> entry : healths.entrySet()) {
            Status status = entry.getValue().getStatus();
            if (entry.getKey().toLowerCase().contains("cache")) {
                if (status.equals(Status.DOWN)) {
                    statusCandidates.add(DEGRADED_STATUS);
                } else if (status.equals(Status.UNKNOWN)) {
                    statusCandidates.add(Status.UP);
                }
            } else {
                statusCandidates.add(entry.getValue().getStatus());
            }
        }
        Status status = this.aggregateStatus(statusCandidates);
        Map<String, Object> details = aggregateDetails(healths);
        return new Health.Builder(status, details).build();
    }

    private Status aggregateStatus(final List<Status> candidates) {
        List<Status> filteredCandidates = new ArrayList<>();
        for (Status candidate : candidates) {
            if (this.statusOrder.contains(candidate.getCode())) {
                filteredCandidates.add(candidate);
            }
        }
        if (filteredCandidates.isEmpty()) {
            return Status.UNKNOWN;
        }
        filteredCandidates.sort(new StatusComparator(this.statusOrder));
        return filteredCandidates.get(0);
    }

    private static Map<String, Object> aggregateDetails(final Map<String, Health> healths) {
        return new LinkedHashMap<>(healths);
    }

    private class StatusComparator implements Comparator<Status> {

        private final List<String> statusOrder;

        StatusComparator(final List<String> statusOrder) {
            this.statusOrder = statusOrder;
        }

        @Override
        public int compare(final Status s1, final Status s2) {
            int i1 = this.statusOrder.indexOf(s1.getCode());
            int i2 = this.statusOrder.indexOf(s2.getCode());
            if (i1 < i2) {
                return -1;
            } else if (i1 == i2) {
                return s1.getCode().compareTo(s2.getCode());
            } else {
                return 1;
            }
        }
    }
}
