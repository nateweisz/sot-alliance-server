package dev.nateweisz.seacats.metrics;

/*
 *
 * public class MetricsTracker { private final AtomicInteger fleetsOpenedCounter
 * = new AtomicInteger(0); private final AtomicInteger membersQueuedCounter =
 * new AtomicInteger(0); private final AtomicInteger staffOnDutyCounter = new
 * AtomicInteger(0);
 *
 * public MetricsTracker(MeterRegistry meterRegistry) {
 * Gauge.builder("bot_fleets_opened", fleetsOpenedCounter, AtomicInteger::get)
 * .description("Total number of fleets opened") .register(meterRegistry);
 *
 * Gauge.builder("bot_members_queued", membersQueuedCounter, AtomicInteger::get)
 * .description("Number of members currently in queue")
 * .register(meterRegistry);
 *
 * Gauge.builder("bot_staff_on_duty", staffOnDutyCounter, AtomicInteger::get)
 * .description("Number of staff currently on duty") .register(meterRegistry); }
 *
 * public void incrementFleetsOpened() { fleetsOpenedCounter.incrementAndGet();
 * }
 *
 * public void decrementFleetsOpened() { fleetsOpenedCounter.decrementAndGet();
 * }
 *
 * public void updateMembersQueued(int queuedMembers) {
 * membersQueuedCounter.set(queuedMembers); }
 *
 * public void updateStaffOnDuty(int staffCount) {
 * staffOnDutyCounter.set(staffCount); } }
 *
 *
 */
