package com.ge.predix.acs.attribute.connector.management.dao;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "attribute_connector")
public class AttributeConnectorEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "cached_interval_minutes", nullable = true)
    private int cachedIntervalMinutes;

    @Column(name = "active", nullable = true)
    private boolean isActive = false;

    @OneToMany(mappedBy = "connector", cascade = { CascadeType.ALL }, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<AttributeAdapterConnectionEntity> attributeAdapterConnections;

    public long getId() {
        return this.id;
    }

    public void setCachedIntervalMinutes(final int cachedIntervalMinutes) {
        this.cachedIntervalMinutes = cachedIntervalMinutes;
    }

    public int getCachedIntervalMinutes() {
        return this.cachedIntervalMinutes;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(final boolean active) {
        this.isActive = active;
    }

    public Set<AttributeAdapterConnectionEntity> getAttributeAdapterConnections() {
        return this.attributeAdapterConnections;
    }

    public void setAttributeAdapterConnections(
            final Set<AttributeAdapterConnectionEntity> attributeAdapterConnections) {
        this.attributeAdapterConnections = attributeAdapterConnections;
    }
}
