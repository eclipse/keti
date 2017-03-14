package com.ge.predix.acs.rest;

public class AttributeAdapterConnection {

    private String adapterEndpoint;

    private String uaaTokenUrl;

    private String uaaClientId;

    private String uaaClientSecret;
    
    public AttributeAdapterConnection() {
        
    }

    public AttributeAdapterConnection(final String adapterEndpoint, final String uaaTokenUrl, final String uaaClientId,
            final String uaaClientSecret) {
        this.adapterEndpoint = adapterEndpoint;
        this.uaaTokenUrl = uaaTokenUrl;
        this.uaaClientId = uaaClientId;
        this.uaaClientSecret = uaaClientSecret;
    }

    public String getAdapterEndpoint() {
        return adapterEndpoint;
    }

    public void setAdapterEndpoint(final String adapterEndpoint) {
        this.adapterEndpoint = adapterEndpoint;
    }

    public String getUaaTokenUrl() {
        return uaaTokenUrl;
    }

    public void setUaaTokenUrl(final String uaaTokenUrl) {
        this.uaaTokenUrl = uaaTokenUrl;
    }

    public String getUaaClientId() {
        return uaaClientId;
    }

    public void setUaaClientId(final String uaaClientId) {
        this.uaaClientId = uaaClientId;
    }

    public String getUaaClientSecret() {
        return uaaClientSecret;
    }

    public void setUaaClientSecret(final String uaaClientSecret) {
        this.uaaClientSecret = uaaClientSecret;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.adapterEndpoint == null) ? 0 : this.adapterEndpoint.hashCode());
        result = prime * result + ((this.uaaClientId == null) ? 0 : this.uaaClientId.hashCode());
//        result = prime * result + ((this.uaaClientSecret == null) ? 0 : this.uaaClientSecret.hashCode());
        result = prime * result + ((this.uaaTokenUrl == null) ? 0 : this.uaaTokenUrl.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AttributeAdapterConnection other = (AttributeAdapterConnection) obj;
        if (this.adapterEndpoint == null) {
            if (other.adapterEndpoint != null) {
                return false;
            }
        } else if (!this.adapterEndpoint.equals(other.adapterEndpoint)) {
            return false;
        }
        if (this.uaaClientId == null) {
            if (other.uaaClientId != null) {
                return false;
            }
        } else if (!this.uaaClientId.equals(other.uaaClientId)) {
            return false;
        }
//        if (this.uaaClientSecret == null) {
//            if (other.uaaClientSecret != null) {
//                return false;
//            }
//        } else if (!this.uaaClientSecret.equals(other.uaaClientSecret)) {
//            return false;
//        }
        if (this.uaaTokenUrl == null) {
            if (other.uaaTokenUrl != null) {
                return false;
            }
        } else if (!this.uaaTokenUrl.equals(other.uaaTokenUrl)) {
            return false;
        }
        return true;
    }
}
