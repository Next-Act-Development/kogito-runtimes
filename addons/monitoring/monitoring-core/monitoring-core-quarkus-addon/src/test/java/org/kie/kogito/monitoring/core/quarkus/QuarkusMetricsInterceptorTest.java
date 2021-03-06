/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.monitoring.core.quarkus;

import org.junit.jupiter.api.Test;
import org.kie.kogito.monitoring.core.common.system.interceptor.MetricsInterceptor;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class QuarkusMetricsInterceptorTest {

    @Test
    public void filter() {
        commonMetricInterceptorFilter(Arrays.asList("url1", "url2"), 200, "url1");
        commonMetricInterceptorFilter(Collections.singletonList("/url2"), 200, "/url2");
        commonMetricInterceptorFilter(Collections.emptyList(), 404, null);
        commonMetricInterceptorFilter(Collections.emptyList(), 200, null);
        commonMetricInterceptorFilter(Collections.singletonList("/url2"), 404, "/url2");
    }

    private void commonMetricInterceptorFilter(List<String> matchedUris, int statusCode, String expectedMatchedUrl) {
        ContainerRequestContext requestMock = mock(ContainerRequestContext.class);
        ContainerResponseContext responseMock = mock(ContainerResponseContext.class);
        UriInfo uriInfoMock = mock(UriInfo.class);
        Response.StatusType statusInfoMock = mock(Response.StatusType.class);

        when(requestMock.getUriInfo()).thenReturn(uriInfoMock);
        when(uriInfoMock.getMatchedURIs()).thenReturn(matchedUris);
        when(responseMock.getStatusInfo()).thenReturn(statusInfoMock);
        when(statusInfoMock.getStatusCode()).thenReturn(statusCode);

        try (MockedStatic<MetricsInterceptor> metricsInterceptor = Mockito.mockStatic(MetricsInterceptor.class)) {

            final ArgumentCaptor<String> matchedUrl = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor.forClass(int.class);

            QuarkusMetricsInterceptor interceptor = new QuarkusMetricsInterceptor();

            interceptor.filter(requestMock, responseMock);

            metricsInterceptor.verify(times(1), () -> MetricsInterceptor.filter(matchedUrl.capture(), statusCodeCaptor.capture()));

            List<String> endpoints = matchedUrl.getAllValues();
            assertThat(endpoints.isEmpty()).isFalse();
            assertThat(endpoints.size()).isEqualTo(1);
            assertThat(endpoints.get(0)).isEqualTo(expectedMatchedUrl);

            List<Integer> statusCodes = statusCodeCaptor.getAllValues();
            assertThat(statusCodes.isEmpty()).isFalse();
            assertThat(statusCodes.size()).isEqualTo(1);
            assertThat(statusCodes.get(0)).isEqualTo(statusCode);
        }
    }
}