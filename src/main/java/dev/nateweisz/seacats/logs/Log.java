package dev.nateweisz.seacats.logs;

import java.util.Map;

public sealed interface Log {
    String createMessage();

    record QueueLog(QueueLogType type, Map<String, String> properties) implements Log {
        @Override
        public String createMessage() {
            return switch (type) {
                case JOIN -> ":man_golfing: %s **QUEUE JOIN** <@%s>: `%s`".formatted(
                    Log.getCurrentTime(), properties.get("user_id"), properties.get("note")
                );
                case LEAVE -> ":wave: %s **QUEUE LEAVE** <@%s>"
                    .formatted(Log.getCurrentTime(), properties.get("user_id"));
                case PROCESS -> ":white_check_mark: %s **QUEUE PROCESS** <@%s> by <@%s> onto %s".formatted(
                    Log.getCurrentTime(), properties.get("user_id"), properties.get("staff_id"), properties.get("ship")
                );
                case CANCEL_PROCESS -> ":x: %s **QUEUE CANCEL PROCESS** <@%s> by <@%s>".formatted(
                    Log.getCurrentTime(), properties.get("user_id"), properties.get("staff_id")
                );
                case UPDATE_NOTE -> ":pencil: %s **QUEUE UPDATE NOTE** <@%s>: `%s`".formatted(
                    Log.getCurrentTime(), properties.get("user_id"), properties.get("note")
                );
                case MISSED_RESERVATION -> ":alarm_clock: %s **QUEUE MISSED RESERVATION** <@%s>".formatted(
                    Log.getCurrentTime(), properties.get("user_id")
                );
            };
        }
    }

    record StaffLog(StaffLogType type, Map<String, String> properties) implements Log {
        @Override
        public String createMessage() {
            return switch (type) {
                case ON_DUTY -> ":chart_with_upwards_trend: %s **STAFF ON DUTY** <@%s>"
                    .formatted(Log.getCurrentTime(), properties.get("staff_id"));
                case OFF_DUTY -> ":chart_with_downwards_trend: %s **STAFF OFF DUTY** <@%s>"
                    .formatted(Log.getCurrentTime(), properties.get("staff_id"));
            };
        }
    }

    record FleetLog(FleetLogType type, Map<String, String> properties) implements Log {
        @Override
        public String createMessage() {
            return switch (type) {
                case CREATE -> ":ship: %s **FLEET CREATE** %s Created by <@%s> with %s ships"
                        .formatted(Log.getCurrentTime(), properties.get("fleet_name"), properties.get("created_by"), properties.get("ships"));
                case DELETE -> ":wastebasket: %s **FLEET DELETE** %s deleted by <@%s>"
                        .formatted(Log.getCurrentTime(), properties.get("fleet_name"), properties.get("deleted_by"));
                case RENAME -> ":pencil: %s **FLEET RENAME** %s Renamed by <@%s> to %s"
                        .formatted(Log.getCurrentTime(), properties.get("fleet_name"), properties.get("renamed_by"), properties.get("new_name"));
                case HOLDING -> ":anchor: %s **FLEET HOLDING** %s Holding updated to %s with holder <@%s>"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("holding"), properties.get("holder"));
                case SHIP_TYPE -> ":ship: %s **FLEET SHIP TYPE** %s updated to `%s`"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("ship_type"));
                case HOP_MODE -> ":ship: %s **FLEET HOP MODE** %s Hop mode updated to %s"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("hop_mode"));
                case OVERRIDE -> ":shield: %s **FLEET OVERRIDE** %s Override max members updated to %s"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("max_members"));
                case REMOVE_USER -> ":x: %s **FLEET REMOVE USER** %s <@%s> Removed user <@%s>"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("removed_by"), properties.get("user_id"));
                case ACTIVITY -> ":chart_with_upwards_trend: %s **FLEET ACTIVITY** %s Activity updated to %s by <@%s>"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("activity"), properties.get("updated_by"));
                case EMISSARY -> ":flag_white: %s **FLEET EMISSARY** %s Emissary updated to %s by <@%s>"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("emissary"), properties.get("updated_by"));
                case CAPTAIN -> ":crossed_swords: %s **FLEET CAPTAIN** %s Captain updated to <@%s> by <@%s>"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("captain"), properties.get("updated_by"));
                case LEAVE -> ":anchor: %s **FLEET LEAVE** %s <@%s> Left the fleet and their removal period has expired"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("user_id"));
                case JOIN -> ":anchor: %s **FLEET JOIN** %s <@%s> Joined the fleet"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("user_id"));
                case LEAVE_TIMEOUT_STARTED -> ":alarm_clock: %s **FLEET LEAVE TIMEOUT STARTED** %s <@%s> Left the fleet and their removal period has started"
                        .formatted(Log.getCurrentTime(), properties.get("ship"), properties.get("user_id"));
            };
        }
    }
    
    private static String getCurrentTime() {
        return "<t:%d:R>".formatted(System.currentTimeMillis() / 1000);
    }
}
