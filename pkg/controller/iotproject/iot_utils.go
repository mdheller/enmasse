/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/util"

	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

func (r *ReconcileIoTProject) findIoTProjectsByPredicate(predicate func(project *iotv1alpha1.IoTProject) bool) ([]iotv1alpha1.IoTProject, error) {

	var result []iotv1alpha1.IoTProject

	list := &iotv1alpha1.IoTProjectList{}

	err := r.client.List(context.TODO(), list)
	if err != nil {
		return nil, err
	}

	for _, item := range list.Items {
		if predicate(&item) {
			result = append(result, item)
		}
	}

	return result, nil
}

func getOrDefaults(strategy *iotv1alpha1.ProvidedDownstreamStrategy) (string, string, iotv1alpha1.EndpointMode, error) {
	endpointName := strategy.EndpointName
	if len(endpointName) == 0 {
		endpointName = DefaultEndpointName
	}
	portName := strategy.PortName
	if len(portName) == 0 {
		portName = DefaultPortName
	}

	var endpointMode iotv1alpha1.EndpointMode
	if strategy.EndpointMode != nil {
		endpointMode = *strategy.EndpointMode
	} else {
		endpointMode = DefaultEndpointMode
	}

	if len(strategy.Namespace) == 0 {
		return "", "", 0, fmt.Errorf("missing namespace")
	}
	if len(strategy.AddressSpaceName) == 0 {
		return "", "", 0, fmt.Errorf("missing address space name")
	}

	return endpointName, portName, endpointMode, nil
}

func extractEndpointInformation(
	endpointName string,
	endpointMode iotv1alpha1.EndpointMode,
	portName string,
	credentials *iotv1alpha1.Credentials,
	addressSpace *enmassev1beta1.AddressSpace,
	forceTls *bool,
) (*iotv1alpha1.ExternalDownstreamStrategy, error) {

	if !addressSpace.Status.IsReady {
		// not ready, yet … wait
		return nil, util.NewObjectNotReadyYetError(addressSpace)
	}

	endpoint := new(iotv1alpha1.ExternalDownstreamStrategy)

	endpoint.Credentials = *credentials

	foundEndpoint := false
	for _, es := range addressSpace.Status.EndpointStatus {
		if es.Name != endpointName {
			continue
		}

		foundEndpoint = true

		var ports []enmassev1beta1.Port

		switch endpointMode {
		case iotv1alpha1.Service:
			endpoint.Host = es.ServiceHost
			ports = es.ServicePorts
		case iotv1alpha1.External:
			endpoint.Host = es.ExternalHost
			ports = es.ExternalPorts
		}

		log.V(2).Info("Ports to scan", "ports", ports)

		endpoint.Certificate = addressSpace.Status.CACertificate

		foundPort := false
		for _, port := range ports {
			if port.Name == portName {
				foundPort = true

				endpoint.Port = port.Port

				tls, err := isTls(addressSpace, &es, &port, forceTls)
				if err != nil {
					return nil, err
				}
				endpoint.TLS = tls

			}
		}

		if !foundPort {
			return nil, fmt.Errorf("unable to find port: %s for endpoint: %s", portName, endpointName)
		}

	}

	if !foundEndpoint {
		return nil, fmt.Errorf("unable to find endpoint: %s", endpointName)
	}

	return endpoint, nil
}

func findEndpointSpec(addressSpace *enmassev1beta1.AddressSpace, endpointStatus *enmassev1beta1.EndpointStatus) *enmassev1beta1.EndpointSpec {
	for _, end := range addressSpace.Spec.Endpoints {
		if end.Name != endpointStatus.Name {
			continue
		}
		return &end
	}
	return nil
}

// get a an estimate if TLS should be enabled for a port, or not
func isTls(
	addressSpace *enmassev1beta1.AddressSpace,
	endpointStatus *enmassev1beta1.EndpointStatus,
	_ *enmassev1beta1.Port,
	forceTls *bool) (bool, error) {

	if forceTls != nil {
		return *forceTls, nil
	}

	endpoint := findEndpointSpec(addressSpace, endpointStatus)

	if endpoint == nil {
		return false, fmt.Errorf("failed to locate endpoint named: %v", endpointStatus.Name)
	}

	if endpointStatus.Certificate != nil {
		// if there is a certificate, enable tls
		return true, nil
	}

	if endpoint.Expose != nil {
		// anything set as tls termination counts as tls enabled = true
		return len(endpoint.Expose.RouteTlsTermination) > 0, nil
	}

	return false, nil

}
