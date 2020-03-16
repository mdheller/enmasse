/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DowntimeData {

    private String name;
    private String reconnectTimeAverage;
    private String createAddressTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReconnectTimeAverage() {
        return reconnectTimeAverage;
    }

    public void setReconnectTimeAverage(String reconnectTimeAverage) {
        this.reconnectTimeAverage = reconnectTimeAverage;
    }

    public String getCreateAddressTime() {
        return createAddressTime;
    }

    public void setCreateAddressTime(String createAddressTime) {
        this.createAddressTime = createAddressTime;
    }

}
