
package com.example.telemetry;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TelemetryApp {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryApp.class);

    public static void main(String[] args) {
        // Set up OTLP exporters
	Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"), "javaapp",
                AttributeKey.stringKey("deployment.environment"), "dev",
                AttributeKey.stringKey("service.version"), "1.0.0"
            )));
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://elastic-apm-server.elastic-system.svc.cluster.local:8200")
                .addHeader("Authorization", "Bearer Wl9sQ0xKZ0JWNlJDY3RrRTlvY0U6cWZ4X0lxX0dwQXE2TGptdkFNWEpnQQ==")
		.build();

        OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint("http://elastic-apm-server.elastic-system.svc.cluster.local:8200")
                .addHeader("Authorization", "Bearer Wl9sQ0xKZ0JWNlJDY3RrRTlvY0U6cWZ4X0lxX0dwQXE2TGptdkFNWEpnQQ==")
		.build();

        // Tracer setup
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
		.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

        // Metrics setup
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                        .setInterval(Duration.ofMinutes(1))
                        .build())
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();

        Tracer tracer = openTelemetry.getTracer("example-app");
        Meter meter = openTelemetry.getMeter("example-app");

        LongCounter requestCounter = meter.counterBuilder("requests.count")
                .setDescription("Counts requests")
                .setUnit("1")
                .build();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            Span span = tracer.spanBuilder("example-span").startSpan();
            span.addEvent("Span started");
            logger.info("This is a log message sent to Elastic APM via OTLP");
            requestCounter.add(1);
            span.end();
        }, 0, 1, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            tracerProvider.shutdown();
            meterProvider.shutdown();
        }));
    }
}
