package dev.nateweisz.seacats.bot;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.nateweisz.seacats.fleets.Fleet;
import dev.nateweisz.seacats.fleets.FleetService;
import dev.nateweisz.seacats.fleets.ShipInfo;
import dev.nateweisz.seacats.fleets.ShipType;
import dev.nateweisz.seacats.messages.MessageType;
import dev.nateweisz.seacats.queue.QueueType;
import dev.nateweisz.seacats.queue.QueuedUser;
import dev.nateweisz.seacats.queue.V2QueuedService;

import io.github.freya022.botcommands.api.components.Buttons;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class EmbedService {
    public static final String QUEUE_JOIN_UPDATE = "QUEUE_JOIN_UPDATE";
    public static final String QUEUE_LEAVE = "QUEUE_LEAVE";
    public static final String CONTACT_STAFF = "CONTACT_STAFF";
    private static final Logger log = LoggerFactory.getLogger(EmbedService.class);

    private final V2QueuedService queueService;
    private final FleetService fleetService;
    private final Buttons buttons;

    public EmbedService(V2QueuedService queueService, FleetService fleetService, Buttons buttons) {
        this.queueService = queueService;
        this.fleetService = fleetService;
        this.buttons = buttons;
    }

    public void updateEmbed(Guild guild, MessageType type, long channelId, long messageId) {
        Pair<List<MessageEmbed>, List<Button>> messageData = switch (type) {
            case FLEET_EMBED -> {
                List<MessageEmbed> embeds = new ArrayList<>();

                if (!queueService.isQueueOpen() && fleetService.isAnyFleetOpen()) {
                    // Display catnap
                    embeds.add(
                            new EmbedBuilder()
                                    .setAuthor(
                                            guild.getName() + " - Fleet Info", "https://scallycats.com",
                                            guild.getIconUrl()
                                    )
                                    .setDescription(
                                            """
                                            # __Cat Nap__
                                            
                                            Our staff are currently taking a cat nap. The queue is closed \
                                            but don't fur-get, we do have a fleet and a staff member will paws \
                                            to reopen the queue when they are bright eyed and ready to manage the fleet.
                                            """
                                    )
                                    .setColor(0xfe1946)
                                    .setThumbnail(
                                            "https://cdn.discordapp.com/attachments/1310454939650363474/1317824044510351401/333.png?ex=676016c8&is=675ec548&hm=fbeaa376cfdd1c6e7dff5655cffaf8976f02516aff4f37c10295b227b6251ca9&"
                                    )
                                    .build()
                    );
                } else if (fleetService.isAnyFleetOpen() && queueService.isQueueOpen()) {
                    for (var fleet : fleetService.getAllFleets()) {
                        embeds.add(
                            new EmbedBuilder()
                                .setAuthor(
                                    guild.getName() + " - Fleet Info", "https://scallycats.com",
                                    guild.getIconUrl()
                                )
                                .setDescription(getFleetDescription(fleet))
                                .setColor(0xfe1946)
                                .setThumbnail(
                                    "https://cdn.discordapp.com/attachments/1310454939650363474/1317824044510351401/333.png?ex=676016c8&is=675ec548&hm=fbeaa376cfdd1c6e7dff5655cffaf8976f02516aff4f37c10295b227b6251ca9&"
                                )
                                .build()
                        );
                    }
                } else if (queueService.isQueueOpen()) {
                    embeds.add(
                        new EmbedBuilder()
                            .setAuthor(
                                guild.getName() + " - Fleet Info", "https://scallycats.com",
                                guild.getIconUrl()
                            )
                            .setDescription(
                                """
                                    If you're feline confuse, tap the 'Contact Fleet Staff' button below \
                                    and submit a request. Our paw-some staff will respond as soon as \
                                    they're available!

                                    **__Your Help is Needed to Spike a Fleet!__**
                                    The queue is currently **open**, but we've not yet spiked a fleet! \
                                    Staff will begin the spiking process when there's a sufficient \
                                    number in the queue.

                                    Everyone can lend a paw with the fleet creation process! If you \
                                    wish to help us, join the <#1316238086635520061> channel and our staff \
                                    will help you join in!
                                    """
                            )
                            .setColor(0xfe1946)
                            .setThumbnail(
                                "https://cdn.discordapp.com/attachments/1310454939650363474/1317824044510351401/333.png?ex=676016c8&is=675ec548&hm=fbeaa376cfdd1c6e7dff5655cffaf8976f02516aff4f37c10295b227b6251ca9&"
                            )
                            .build()
                    );
                }

                embeds.add(
                    new EmbedBuilder()
                        .setAuthor(
                            guild.getName() + " - Queue Info", "https://scallycats.com",
                            guild.getIconUrl()
                        )
                        .setDescription(getQueueDescription())
                        .setColor(0xfe1946)
                        .setThumbnail(
                            "https://cdn.discordapp.com/attachments/1310454939650363474/1317824044510351401/333.png?ex=676016c8&is=675ec548&hm=fbeaa376cfdd1c6e7dff5655cffaf8976f02516aff4f37c10295b227b6251ca9&"
                        )
                        .build()
                );

                yield Pair.of(
                    embeds, List.of(
                        buttons.success("Join/Update Queue Note")
                            .withEmoji(":index_pointing_at_the_viewer:")
                            .persistent()
                            .bindTo(QUEUE_JOIN_UPDATE)
                            .build(),
                        buttons.danger("Leave Queue")
                            .withEmoji(":headstone:")
                            .persistent()
                            .bindTo(QUEUE_LEAVE)
                            .build(),
                        buttons.primary("Contact Fleet Staff")
                            .withEmoji(":sos:")
                            .persistent()
                            .bindTo(CONTACT_STAFF)
                            .build()
                    )
                );
            }
            case STAFF_ROSTER -> null;
        };

        guild.getTextChannelById(channelId).editMessageById(
            messageId,
            MessageEditData.fromEmbeds(messageData.getFirst())
        ).setContent(null).setActionRow(messageData.getSecond()).queue();
    }

    private String getQueueDescription() {
        if (queueService.isQueueOpen()) {
            StringBuilder description = new StringBuilder();

            if (!queueService.findAllOnDutyUsers().isEmpty()) {
                description.append("## On Duty Staff Members: ")
                    .append(
                        queueService.findAllOnDutyUsers().stream().map("<@%d>"::formatted)
                            .collect(Collectors.joining(", "))
                    ).append("\n");
            }

            description.append("""

                The following pirates are currently queued for a spot on our fur-\
                midable fleets. You can pounce into or claw your way out of the \
                queue using the buttons below.
                """);

            if (!queueService.isQueueEmpty(QueueType.PRIORITY)) {
                description.append(
                    "# __Priority Queue (%s)__"
                        .formatted(getQueueOrdered(QueueType.PRIORITY).size())
                );
                int queueSpot = 1;
                for (var entry : getQueueOrdered(QueueType.PRIORITY)) {
                    description.append(
                        "\n#**%d**. **<@%d>** `%s` [<t:%d:R>]\n".formatted(
                            queueSpot++, entry.getMemberId(), entry.getQueueNote(),
                            entry.getQueueTime() / 1000
                        )
                    );
                    description.append(
                        " *➥* :arrow_up: **Queue Priority: ** `%s`\n"
                            .formatted(entry.getPriorityReason())
                    );

                    if (entry.getStaffNote() != null) {
                        description.append(
                            "  *➥ :notepad_spiral: Staff Note:* %s\n"
                                .formatted(entry.getStaffNote())
                        );
                    }

                    if (entry.isPending()) {
                        Fleet fleet = fleetService.getFleetByChannel(entry.getPendingChannel()).get();
                        description.append("  *➥ :hourglass: **Pending Reservation: F%d, Ship %d**\n".formatted(
                                fleet.getNumericalId(),
                                fleet.getChannelShipInfo().get(entry.getPendingChannel()).getShipId()
                        ));
                    }

                    if (entry.getAfkTimeout() != null && !entry.isAfkTimeoutOver()) {
                        description.append(" ➥ :x: **AFK Check: Pending...**\n");
                    }
                }
            }

            // TODO: extract these things into a dedicated function to reduce duplication

            description
                .append("# __Queue (%s)__".formatted(getQueueOrdered(QueueType.NORMAL).size()));

            int queueSpot = 0;
            for (var entry : getQueueOrdered(QueueType.NORMAL)) {
                queueSpot++;
                description.append(
                    "\n#**%d**. **<@%d>** `%s` [<t:%d:R>]\n".formatted(
                        queueSpot, entry.getMemberId(), entry.getQueueNote(),
                        entry.getQueueTime() / 1000
                    )
                );

                if (entry.getStaffNote() != null) {
                    description.append(
                        "  *➥ :notepad_spiral: Staff Note:* %s\n".formatted(entry.getStaffNote())
                    );
                }

                if (entry.isPending()) {
                    Fleet fleet = fleetService.getFleetByChannel(entry.getPendingChannel()).get();
                    description.append("  *➥ :hourglass: **Pending Reservation: F%d, Ship %d**\n".formatted(
                            fleet.getNumericalId(),
                            fleet.getChannelShipInfo().get(entry.getPendingChannel()).getShipId()
                    ));
                }

                if (entry.getAfkTimeout() != null && !entry.isAfkTimeoutOver()) {
                    description.append(" ➥ :x: **AFK Check: Pending...**\n");
                }
            }

            if (queueSpot == 0) {
                description.append(
                    "\nThe queue is currently empty. You can join by using the button below!"
                );
            }

            return description.toString();
        }

        return """
            # __Queue Currently Closed__
            The queue is currently closed. Our fur-tastic staff will meow \
            at the Fleet Alert role when the queue reopens at <t:%d:F>. Be sure to \
            claw your way to the Channels & Roles section at the top to \
            pounce on that role if you wish to be alerted.

            %s [Support us on Patreon](https://www.patreon.com/seacatsscallywags) for Discord purr-ks and Queue Priority \
            during special events!
            """
            .formatted(
                    getNextQueueOpenTime(),
                Emoji.fromCustom("seaCatsCoins", 1306419566968176720L, false).getAsMention()
            );
    }

    private String getFleetDescription(Fleet fleet) {
        StringBuilder description = new StringBuilder();
        description
            .append("# __Fleet #%d - %s__\n".formatted(fleet.getNumericalId(), fleet.getName()));

        var ships = new ArrayList<>(fleet.getChannelShipInfo().entrySet());
        ships.sort(Comparator.comparingInt(entry -> entry.getValue().getShipId()));

        for (Map.Entry<Long, ShipInfo> entry : ships) {
            ShipInfo info = entry.getValue();
            int shipNumber = info.getShipId();

            if (info.getType() == ShipType.CLOSED) {
                description.append(
                        "➥ :red_circle: **F%s-S%d** `[Closed]\n`"
                                .formatted(fleet.getNumericalId(), shipNumber)
                );
            } else if ((info.getMaxMembers() != -1 && info.getMemberIds().size() >= info.getMaxMembers()) || (info.getType().getMaxMembers() == info.getMemberIds().size())) {
                // green dot
                // full ship
                description.append(
                    "➥ :green_circle: **F%s-S%d** `[%s/%s/%s]\n`".formatted(
                        fleet.getNumericalId(), shipNumber, info.getType().getPretty(),
                        info.getEmissary(), info.getActivity()
                    )
                );
            } else if (!info.isInitialized()) {
                description.append(
                    "➥ :red_circle: **F%s-S%d** `[%s]\n`"
                        .formatted(fleet.getNumericalId(), shipNumber, info.getType().getPretty())
                );
            } else if (info.isHolding()) {
                // holding
                description.append(
                        "➥ :orange_circle: **F%s-S%d** `[%s/Holding: %s (Need %d to start)]\n`"
                                .formatted(
                                        fleet.getNumericalId(), shipNumber, info.getType().getPretty(),
                                        info.getHeldBy() == null ? "Unknown" : info.getHeldBy(),
                                        info.getType().getMaxMembers() - info.getMemberIds().size()
                                )
                );
            } else if (info.getType().getMaxMembers() > info.getMemberIds().size()) {
                // open
                description.append(
                    "➥ %s **F%s-S%d** `[%s/%s/%s]\n`".formatted(
                        info.getMemberIds().isEmpty() ? ":red_circle:" : ":yellow_circle:",
                        fleet.getNumericalId(), shipNumber, info.getType().getPretty(),
                        info.getEmissary(), info.getActivity()
                    )
                );


                description.append(
                    "  *➥ Crew needed: %s*\n".formatted(
                        convertNumberToEmoji(
                                (info.getMaxMembers() != -1 ? info.getMaxMembers() : info.getType().getMaxMembers()) - info.getMemberIds().size()
                        )
                    )
                );

                // TODO: fix spacing on these
                // TODO: last pirate standing
            }

            // general stuff
            if (info.getCaptain() != -1 && info.getType() != ShipType.CLOSED) {
                description.append("  *➥ :pirate_flag: Ship is captained.*\n");
            }

            // TODO: voice channel empty

            description.append("\n");
        }

        return description.toString();
    }

    private String convertNumberToEmoji(int number) {
        return switch (number) {
            case 1 -> ":one:";
            case 2 -> ":two:";
            case 3 -> ":three:";
            case 4 -> ":four:";
            case 5 -> ":five:";
            case 6 -> ":six:";
            case 7 -> ":seven:";
            case 8 -> ":eight:";
            case 9 -> ":nine:";
            case 10 -> ":number_10:";
            case 11 -> ":one::one:";
            case 12 -> ":one::two:";
            case 13 -> ":one::three:";
            case 14 -> ":one::four:";
            case 15 -> ":one::five:";
            case 16 -> ":one::six:";

            default -> throw new IllegalStateException("Unexpected value: " + number);
        };
    }

    private List<QueuedUser> getQueueOrdered(QueueType type) {
        return queueService.findAllQueuedUsers().stream()
            .filter(u -> u.getType() == type)
            .sorted(Comparator.comparing(QueuedUser::getQueueTime))
            .collect(Collectors.toList());
    }

    private long getNextQueueOpenTime() {
        // returns 5am pst the next day
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
        ZonedDateTime nextQueueOpen = now.withHour(5).withMinute(0).withSecond(0);
        if (now.getHour() >= 5) {
            nextQueueOpen = nextQueueOpen.plusDays(1);
        }
        return nextQueueOpen.toEpochSecond();
    }
}
