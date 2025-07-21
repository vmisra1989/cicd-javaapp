
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

public class TelemetryApp {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryApp.class);

    public static void main(String[] args) {
        // Set up OTLP exporters
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("elastic-apm-server.elastic-system.svc:8200") // Elastic APM OTLP endpoint
                .build();

        OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint("elastic-apm-server.elastic-system.svc:8200")
                .build();

        // Tracer setup
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

        // Metrics setup
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                        .setInterval(Duration.ofSeconds(10))
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

        // Simulate telemetry
        Span span = tracer.spanBuilder("example-span").startSpan();
        span.addEvent("Span started");
        logger.info("This is a log message sent to Elastic APM via OTLP");
        requestCounter.add(1);
        span.end();

        // Shutdown
        tracerProvider.shutdown();
        meterProvider.shutdown();
    }
}

