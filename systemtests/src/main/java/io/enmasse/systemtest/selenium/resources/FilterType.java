/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;


public enum FilterType {
    TYPE, ADDRESS, STATUS, NAMESPACE, NAME, HOSTNAME, CONTAINER;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
