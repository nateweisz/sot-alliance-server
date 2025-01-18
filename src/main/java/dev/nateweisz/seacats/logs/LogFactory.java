package dev.nateweisz.seacats.logs;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class LogFactory {

    public Log createLog(String type, Map<String, String> properties) {
        return switch (type) {
            case "queue" ->
                new Log.QueueLog(QueueLogType.valueOf(properties.get("queue_type")), properties);
            case "staff" ->
                new Log.StaffLog(StaffLogType.valueOf(properties.get("staff_type")), properties);
            case "fleet" ->
                new Log.FleetLog(FleetLogType.valueOf(properties.get("fleet_type")), properties);
            default -> null;
        };
    }
}
