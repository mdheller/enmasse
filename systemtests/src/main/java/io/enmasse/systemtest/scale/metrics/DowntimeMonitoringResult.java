/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DowntimeMonitoringResult {

    private String normalTimeToCreateAddress;
    private List<DowntimeData> downtimeData = new ArrayList<>();

    public String getNormalTimeToCreateAddress() {
        return normalTimeToCreateAddress;
    }

    public void setNormalTimeToCreateAddress(String normalTimeToCreateAddress) {
        this.normalTimeToCreateAddress = normalTimeToCreateAddress;
    }

    public List<DowntimeData> getDowntimeData() {
        return downtimeData;
    }

    public void setDowntimeData(List<DowntimeData> downtimeData) {
        this.downtimeData = downtimeData;
    }

}
