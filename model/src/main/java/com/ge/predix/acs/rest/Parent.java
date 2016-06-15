package com.ge.predix.acs.rest;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.util.Assert;

import com.ge.predix.acs.model.Attribute;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a source of inherited attributes.
 */
@ApiModel(description = "Represents a source of inherited attributes.")
public class Parent {
    @ApiModelProperty(
            value = "The unique identifier of the parent, e.g. \"tom@ge.com\" or \"/site/sanramon\".",
            required = true)
    private String identifier;

    @ApiModelProperty(
            value = "Restrictions on when a subject inherits parent attributes based on the resource accessed,"
                    + " i.e. a subject may only inherit the attributes from a parent subject if the user accesses"
                    + " a resource with specific attributes. e.g. \"tom@ge.com\" inherits attributes from the"
                    + " \"analysts\" group only when accessing resources where the \"site\" is \"sanramon\".",
            required = true)
    private Set<Attribute> scopes = Collections.emptySet();

    public Parent() {
        // Default constructor.
    }

    public Parent(final String identifier) {
        this.identifier = identifier;
    }

    public Parent(final String identifier, final Set<Attribute> scopes) {
        setScopes(scopes);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public Set<Attribute> getScopes() {
        return this.scopes;
    }

    public void setScopes(final Set<Attribute> scopes) {
        Assert.isTrue(scopes.size() < 2, "Multiple scope attributes are not supported, yet.");
        this.scopes = scopes;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.identifier).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Parent) {
            final Parent other = (Parent) obj;
            return new EqualsBuilder().append(this.identifier, other.identifier).isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "Parent [identifier=" + identifier + ", scopes=" + scopes + "]";
    }

}
