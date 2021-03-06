/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.device.impl;

import static io.vertx.core.json.JsonObject.mapFrom;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.eclipse.hono.util.RegistrationResult.from;

import org.eclipse.hono.util.RegistrationConstants;
import org.eclipse.hono.util.RegistrationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.enmasse.iot.jdbc.store.device.AbstractDeviceStore;
import io.enmasse.iot.registry.device.AbstractRegistrationService;
import io.enmasse.iot.registry.device.DeviceKey;
import io.opentracing.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

@Component
public class RegistrationServiceImpl extends AbstractRegistrationService {

    private final AbstractDeviceStore store;

    @Autowired
    public RegistrationServiceImpl(final AbstractDeviceStore store) {
        this.store = store;
    }

    @Override
    protected Future<RegistrationResult> processGetDevice(final DeviceKey key, final Span span) {

        return this.store.readDevice(key, span.context())
                .map(r -> {

                    if (r.isPresent()) {

                        var result = r.get();
                        var data = mapFrom(result.getDevice());
                        var payload = new JsonObject()
                                .put(RegistrationConstants.FIELD_DATA, data);
                        return from(HTTP_OK, payload, null);

                    } else {

                        return from(HTTP_NOT_FOUND);

                    }

                });

    }

}
