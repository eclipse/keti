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

package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class CachedAttributes {

    private State state;
    private Set<Attribute> attributes;

    public CachedAttributes() {
        // Needed for jackson serialization
    }

    public CachedAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
        this.state = State.SUCCESS;
    }

    public CachedAttributes(final State state) {
        this.state = state;
    }

    public State getState() {
        return this.state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public enum State {

        SUCCESS,
        DO_NOT_RETRY
    }
}
