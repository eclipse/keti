package com.ge.predix.acs.rest;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Connection configuration for an adapter to retrieve external resource or subject attributes.")
public class AttributeAdapterConnection {

    private String adapterEndpoint;

    private String uaaTokenUrl;

    private String uaaClientId;

    private String uaaClientSecret;

    public AttributeAdapterConnection() {
        // Required to be here for Jackson deserialization
    }

    public AttributeAdapterConnection(final String adapterEndpoint, final String uaaTokenUrl, final String uaaClientId,
            final String uaaClientSecret) {
        this.adapterEndpoint = adapterEndpoint;
        this.uaaTokenUrl = uaaTokenUrl;
        this.uaaClientId = uaaClientId;
        this.uaaClientSecret = uaaClientSecret;
    }

    public AttributeAdapterConnection(final AttributeAdapterConnection other) {
        this.adapterEndpoint = other.adapterEndpoint;
        this.uaaTokenUrl = other.uaaTokenUrl;
        this.uaaClientId = other.uaaClientId;
        this.uaaClientSecret = other.uaaClientSecret;
    }

    @ApiModelProperty(value = "Adapter URL to retrieve attributes from", required = true)
    public String getAdapterEndpoint() {
        return adapterEndpoint;
    }

    public void setAdapterEndpoint(final String adapterEndpoint) {
        this.adapterEndpoint = adapterEndpoint;
    }

    @ApiModelProperty(value = "UAA URL to request an access token for this adapter", required = true)
    public String getUaaTokenUrl() {
        return uaaTokenUrl;
    }

    public void setUaaTokenUrl(final String uaaTokenUrl) {
        this.uaaTokenUrl = uaaTokenUrl;
    }

    @ApiModelProperty(
            value = "OAuth client used to obtain an access token for this adapter",
            required = true)
    public String getUaaClientId() {
        return uaaClientId;
    }

    public void setUaaClientId(final String uaaClientId) {
        this.uaaClientId = uaaClientId;
    }

    @ApiModelProperty(value = "OAuth client secret used to obtain an access token for this adapter", required = true)
    public String getUaaClientSecret() {
        return uaaClientSecret;
    }

    public void setUaaClientSecret(final String uaaClientSecret) {
        this.uaaClientSecret = uaaClientSecret;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.adapterEndpoint).append(this.uaaTokenUrl).append(this.uaaClientId)
                .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof AttributeAdapterConnection) {
            AttributeAdapterConnection other = (AttributeAdapterConnection) obj;
            return new EqualsBuilder().append(this.adapterEndpoint, other.adapterEndpoint)
                    .append(this.uaaTokenUrl, other.uaaTokenUrl).append(this.uaaClientId, other.uaaClientId).isEquals();
        }
        return false;
    }
}
