/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.amqp.Artemis;
import io.enmasse.amqp.SyncRequestClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class BrokerStatusCollector {
    private static final Logger log = LoggerFactory.getLogger(BrokerStatusCollector.class);

    private final Kubernetes kubernetes;
    private final BrokerClientFactory brokerClientFactory;
    private final StandardControllerOptions options;

    BrokerStatusCollector(Kubernetes kubernetes, BrokerClientFactory brokerClientFactory, StandardControllerOptions options) {
        this.kubernetes = kubernetes;
        this.brokerClientFactory = brokerClientFactory;
        this.options = options;
    }

    Set<String> getAddressNames(String clusterId) throws Exception {
        Set<String> addressNames = new HashSet<>();
        List<Pod> pods = kubernetes.listBrokers(clusterId);
        for (Pod broker : pods) {
            if (Readiness.isPodReady(broker)) {
                try (
                        SyncRequestClient brokerClient = brokerClientFactory.connectBrokerManagementClient(broker.getStatus().getPodIP(), 5673);
                        Artemis artemis = new Artemis(brokerClient, options.getManagementQueryTimeout().toMillis());
                        ) {
                    addressNames.addAll(artemis.getAddressNames());
                }
            } else {
                throw new IllegalStateException(String.format("Broker pod '%s' in cluster '%s' is not ready (%s), cannot get address names at this time.",
                        broker.getMetadata().getName(),
                        clusterId,
                        broker.getStatus()));
            }
        }
        log.debug("Cluster '{}' ({} replica(s)) has the following addresses: {}", clusterId, pods.size(), addressNames);
        return addressNames;
    }

    Set<String> getQueueNames(String clusterId) throws Exception {
        Set<String> queueNames = new HashSet<>();
        List<Pod> pods = kubernetes.listBrokers(clusterId);
        for (Pod broker : pods) {
            if (Readiness.isPodReady(broker)) {
                try (
                        SyncRequestClient brokerClient = brokerClientFactory.connectBrokerManagementClient(broker.getStatus().getPodIP(), 5673);
                        Artemis artemis = new Artemis(brokerClient, options.getManagementQueryTimeout().toMillis());
                ) {
                    queueNames.addAll(artemis.getQueueNames());
                }
            } else {
                throw new IllegalStateException(String.format("Broker pod '%s' in cluster '%s' is not ready (%s), cannot get queue names at this time.",
                        broker.getMetadata().getName(),
                        clusterId,
                        broker.getStatus()));
            }
        }
        log.debug("Cluster '{}' ({} replica(s)) has the following queues: {}", clusterId, pods.size(), queueNames);
        return queueNames;
    }

    long getQueueMessageCount(String queue, String clusterId) throws Exception {
        long totalMessageCount = 0;
        List<Pod> pods = kubernetes.listBrokers(clusterId);
        for (Pod broker : pods) {
            if (Readiness.isPodReady(broker)) {
                try (
                        SyncRequestClient brokerClient = brokerClientFactory.connectBrokerManagementClient(broker.getStatus().getPodIP(), 5673);
                        Artemis artemis = new Artemis(brokerClient, options.getManagementQueryTimeout().toMillis());
                        ) {
                    long queueMessageCount = artemis.getQueueMessageCount(queue);
                    if (queueMessageCount < 0) {
                        // ARTEMIS-1982?
                        throw new IllegalStateException(String.format("Depth for queue '%s' on broker pod '%s' in cluster '%s' reported negative (%d)",
                                queue,
                                broker.getMetadata().getName(),
                                clusterId,
                                queueMessageCount));
                    }
                    totalMessageCount += queueMessageCount;
                }
            } else {
                throw new IllegalStateException(String.format("Broker pod '%s' in cluster '%s' is not ready (%s), cannot get depth for queue '%s' at this time.",
                        broker.getMetadata().getName(),
                        clusterId,
                        broker.getStatus(),
                        queue));
            }
        }
        log.info("Queue '{}' on cluster '{}' ({} replica(s)) has depth: {}", queue,  clusterId, pods.size(), totalMessageCount);
        return totalMessageCount;
    }
}
