package com.ge.predix.acs.zone.management.dao;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

    @OneToMany(mappedBy = "connector", cascade = { CascadeType.ALL }, orphanRemoval = true)
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

    public Set<AttributeAdapterConnectionEntity> getAttributeAdapterConnections() {
        return this.attributeAdapterConnections;
    }

    public void setAttributeAdapterConnections(
            final Set<AttributeAdapterConnectionEntity> attributeAdapterConnections) {
        this.attributeAdapterConnections = attributeAdapterConnections;
    }
}
