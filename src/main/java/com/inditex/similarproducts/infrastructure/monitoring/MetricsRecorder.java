package com.inditex.similarproducts.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricsRecorder {
    private final MeterRegistry meterRegistry;
    private static final String KEY = "api.requests";
    private static final String TAG_FLOW = "flow";
    private static final String TAG_TYPE = "type";

    public MetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String endpoint, MetricsType tag) {
        Counter.builder(KEY)
                .tag(TAG_FLOW, endpoint)
                .tag(TAG_TYPE, tag.getValue())
                .register(meterRegistry)
                .increment();
    }

}
