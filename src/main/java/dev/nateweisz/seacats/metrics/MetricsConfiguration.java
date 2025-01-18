package dev.nateweisz.seacats.metrics;
/*
 *
 * @Configuration public class MetricsConfiguration {
 *
 * @Value("${spring.profiles.active}") private String env;
 *
 * @Bean public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
 * return registry -> registry.config() .commonTags( "application",
 * "fleet manager", "env", env ); }
 *
 *
 * @Bean
 *
 * @Profile("prod") public MetricsTracker metricsTracker(MeterRegistry
 * meterRegistry) { return new MetricsTracker(meterRegistry); }
 *
 * @Bean
 *
 * @Profile("!prod") public NopMetricsTracker nopMetricsTracker(MeterRegistry
 * meterRegistry) { return new NopMetricsTracker(meterRegistry); } }
 *
 *
 */
