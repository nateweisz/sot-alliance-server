package dev.nateweisz.seacats.commands;

import static dev.nateweisz.seacats.commands.autocomplete.ActivityComplete.ACTIVITY_COMPLETE;
import static dev.nateweisz.seacats.commands.autocomplete.EmissaryComplete.EMISSARY_COMPLETE;
import static dev.nateweisz.seacats.commands.autocomplete.ShipTypeComplete.SHIP_TYPE_COMPLETE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import dev.nateweisz.seacats.fleets.Fleet;
import dev.nateweisz.seacats.fleets.FleetService;
import dev.nateweisz.seacats.fleets.ShipInfo;
import dev.nateweisz.seacats.fleets.ShipType;
import dev.nateweisz.seacats.logs.FleetLogType;
import dev.nateweisz.seacats.messages.MessageService;

import dev.nateweisz.seacats.queue.QueuedUser;
import dev.nateweisz.seacats.queue.V2QueuedService;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.annotations.Optional;
import io.github.freya022.botcommands.api.commands.annotations.UserPermissions;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.Length;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;

@Command
public class FleetCommands extends ApplicationCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(FleetCommands.class);
    private final FleetService fleetService;
    private final V2QueuedService queueService;
    private final MessageService messageService;

    @Value("${jda.roles.staff}")
    private long staffRoleId;

    @Value("${jda.fleet-order}")
    private int fleetOrder;

    public FleetCommands(FleetService fleetService, V2QueuedService queueService, MessageService messageService) {
        this.fleetService = fleetService;
        this.queueService = queueService;
        this.messageService = messageService;
    }

    @JDASlashCommand(name = "fleet", subcommand = "create", description = "Creates a new fleet")
    @TopLevelSlashCommandData(description = "Manage fleets")
    public void create(
        GuildSlashEvent event,
        @SlashOption(name = "name", description = "The name of the fleet") String name,
        @SlashOption(name = "ships", description = "The number of ships that you spiked") int ships,
        @SlashOption(
            name = "type", description = "The type of fleet, default is sloop", autocomplete = SHIP_TYPE_COMPLETE
        ) @Nullable ShipType type
    ) {
        if (fleetService.fleetExists(name)) {
            event.reply("A fleet with that name already exists.").setEphemeral(true).queue();
            return;
        }

        if (ships <= 0) {
            event.reply("You cannot spike a fleet with 0 or less ships cause what's the point?")
                .setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        // Create channels and category
        // TODO: permissions
        int id = fleetService.getAllFleets().size() + 1;

        event.getGuild().createRole()
            .setName("Fleet #%s".formatted(id))
            .setMentionable(true)
            .queue(fleetRole -> {
                event.getGuild().createCategory("Alliance Fleet #%s".formatted(id))
                    .setPosition(fleetOrder)
                    .addPermissionOverride(
                        event.getGuild().getPublicRole(), List.of(),
                        List.of(Permission.VIEW_CHANNEL)
                    )
                    .addRolePermissionOverride(
                        staffRoleId,
                        List.of(
                            Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY,
                            Permission.VOICE_CONNECT, Permission.VOICE_SPEAK,
                            Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES
                        ), List.of()
                    )
                    .addRolePermissionOverride(
                        fleetRole.getIdLong(),
                        List.of(
                            Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY,
                            Permission.VOICE_CONNECT, Permission.VOICE_SPEAK,
                            Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES
                        ), List.of()
                    )
                    .queue(category -> {
                        category
                            .createVoiceChannel(
                                "\uD83D\uDFE2 Fleet #%s [%d Ships]".formatted(id, ships)
                            )
                            .addRolePermissionOverride(
                                fleetRole.getIdLong(), List.of(Permission.VIEW_CHANNEL),
                                List.of(Permission.VOICE_CONNECT)
                            )
                            .setPosition(0)
                            .queue();

                        category.createTextChannel("fleet-general")
                            .setPosition(1)
                            .queue();

                        List<CompletableFuture<Void>> channelCreationFutures = new ArrayList<>();
                        Map<Long, ShipInfo> shipInfo = new ConcurrentHashMap<>();

                        for (int i = 1; i <= ships; i++) {
                            int finalI = i;
                            CompletableFuture<Void> channelFuture =
                                CompletableFuture.supplyAsync(() -> {
                                    Role role =
                                        event.getGuild().createRole()
                                            .setName("Fleet #%s, Ship %d".formatted(id, finalI))
                                            .setMentionable(true)
                                            .complete();

                                    VoiceChannel channel =
                                        category
                                            .createVoiceChannel(
                                                "F%s, Ship %d".formatted(id, finalI)
                                            )
                                            .addPermissionOverride(
                                                event.getGuild().getPublicRole(), List.of(),
                                                List.of(Permission.VIEW_CHANNEL)
                                            )
                                            .addRolePermissionOverride(
                                                fleetRole.getIdLong(),
                                                List.of(Permission.VIEW_CHANNEL),
                                                List.of(Permission.VOICE_CONNECT)
                                            )
                                            .addRolePermissionOverride(
                                                role.getIdLong(),
                                                List.of(
                                                    Permission.VIEW_CHANNEL,
                                                    Permission.VOICE_CONNECT,
                                                    Permission.VOICE_SPEAK, Permission.MESSAGE_SEND,
                                                    Permission.MESSAGE_ATTACH_FILES
                                                ), List.of()
                                            )
                                            .addRolePermissionOverride(
                                                staffRoleId,
                                                List.of(
                                                    Permission.VIEW_CHANNEL,
                                                    Permission.VOICE_CONNECT,
                                                    Permission.VOICE_SPEAK, Permission.MESSAGE_SEND,
                                                    Permission.MESSAGE_ATTACH_FILES
                                                ), List.of()
                                            )
                                            .setPosition(finalI + 1)
                                            .complete();

                                    ShipInfo info =
                                        new ShipInfo(
                                            finalI,
                                            type == null ? ShipType.SLOOP : type,
                                            new ArrayList<>(),
                                            "No Activity Set",
                                            "NA",
                                            true,
                                            null,
                                            -1,
                                            true,
                                            role.getIdLong(),
                                            -1
                                        );

                                    shipInfo.put(channel.getIdLong(), info);
                                    return null;
                                });

                            channelCreationFutures.add(channelFuture);
                        }

                        CompletableFuture
                            .allOf(channelCreationFutures.toArray(new CompletableFuture[0]))
                            .thenRun(() -> {
                                Fleet fleet =
                                    Fleet.builder()
                                        .numericalId(id)
                                        .name(name)
                                        .categoryId(category.getIdLong())
                                        .startedBy(event.getUser().getIdLong())
                                        .channelShipInfo(shipInfo)
                                        .roleId(fleetRole.getIdLong())
                                        .build();

                                fleetService.createFleet(fleet);

                                messageService.updatePanelEmbed(event.getJDA());
                                event.getHook().editOriginalEmbeds(
                                    new EmbedBuilder()
                                        .setAuthor(
                                            event.getGuild().getName() + " - Fleet Created",
                                            "https://scallycats.com", event.getGuild().getIconUrl()
                                        )
                                        .setDescription(
                                            """
                                                # Fleet Created

                                                A fleet has been created with %d ships and all voice channels
                                                have been created. All ships are currently marked as as a %s.
                                                If any of the ships are not a %s, please update the ship type
                                                using the command /fleet ship-type <voice_channel> <ship_type>.

                                                In addition, all ships are marked as captained by default. If
                                                any ships are not captained you can update the ship's captaincy
                                                via /fleet captain <voice_channel> false.

                                                We ask that you give priority to all members who assisted in the
                                                spiking process and give them priority queue.
                                                """
                                                .formatted(
                                                    ships,
                                                    type == null ? "Sloop" : type.getPretty(),
                                                    type == null ? "Sloop" : type.getPretty()
                                                )
                                        )
                                        .setColor(0x2b2d31)
                                        .build()
                                ).queue();

                                MDC.put("type", "fleet");
                                MDC.put("fleet_type", FleetLogType.CREATE.name());
                                MDC.put("created_by", event.getUser().getId());
                                MDC.put("fleet_name", name);
                                MDC.put("ships", String.valueOf(ships));
                                LOGGER.info("Fleet has been created by {}", event.getUser().getId());
                                MDC.clear();
                            })
                            .exceptionally(ex -> {
                                ex.printStackTrace();
                                event.getHook().editOriginal(
                                    "An error occurred while creating the fleet. Please try again."
                                ).queue();
                                return null;
                            });
                    });
            });
    }

    @JDASlashCommand(name = "fleet", subcommand = "delete", description = "Deletes a fleet")
    public void delete(
        GuildSlashEvent event,
        @SlashOption(name = "name", description = "The name of the fleet") String name
    ) {
        if (!fleetService.fleetExists(name)) {
            event.reply("A fleet with that name does not exist.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        Fleet fleet = fleetService.getFleet(name);
        event.getGuild().getCategoryById(fleet.getCategoryId()).getChannels()
            .forEach(channel -> channel.delete().queue());
        event.getGuild().getCategoryById(fleet.getCategoryId()).delete().queue();
        event.getGuild().getRoleById(fleet.getRoleId()).delete().queue();

        fleet.getChannelShipInfo().values().forEach(info -> {
            event.getGuild().getRoleById(info.getRoleId()).delete().queue();
        });

        fleetService.deleteFleet(name);
        event.getHook().editOriginal("Fleet deleted.").queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.DELETE.name());
        MDC.put("fleet_name", name);
        MDC.put("deleted_by", event.getUser().getId());
        LOGGER.info("Fleet has been deleted by {}", event.getUser().getId());
        MDC.clear();
    }

    @JDASlashCommand(
        name = "fleet", subcommand = "ship-type", description = "Updates the ship type of a ship"
    )
    public void shipType(
        GuildSlashEvent event,
        @SlashOption(
            name = "ship_channel", description = "The voice channel of the ship"
        ) VoiceChannel channel,
        @SlashOption(
            name = "ship_type", description = "The type of ship", autocomplete = SHIP_TYPE_COMPLETE
        ) ShipType type
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        info.setType(type);
        info.setInitialized(true);
        fleet.getChannelShipInfo().put(channel.getIdLong(), info);
        fleetService.saveFleet(fleet);

        event.reply("Ship type updated.").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.SHIP_TYPE.name());
        MDC.put("ship", channel.getName());
        MDC.put("ship_type", type.name());
        LOGGER.info("Fleet ship type has been updated by {}", event.getUser().getId());
        MDC.clear();
    }

    @JDASlashCommand(
        name = "fleet", subcommand = "activity", description = "Updates the activity of a ship"
    )
    public void activity(
        GuildSlashEvent event,
        @SlashOption(
            name = "ship_channel", description = "The voice channel of the ship"
        ) VoiceChannel channel,
        @SlashOption(
            name = "activity", description = "The activity of the ship", autocomplete = ACTIVITY_COMPLETE
        ) String activity
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        info.setActivity(activity);

        if (info.isHolding()) {
            info.setHolding(false);
        }

        fleet.getChannelShipInfo().put(channel.getIdLong(), info);
        fleetService.saveFleet(fleet);

        event.reply("Activity updated.").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.ACTIVITY.name());
        MDC.put("ship", channel.getName());
        MDC.put("activity", activity);
        MDC.put("updated_by", event.getUser().getId());
        LOGGER.info("Fleet activity has been updated by {}", event.getUser().getId());
        MDC.clear();
    }

    @JDASlashCommand(
        name = "fleet", subcommand = "emissary", description = "Updates the emissary of a ship"
    )
    public void emissary(
        GuildSlashEvent event,
        @SlashOption(
            name = "ship_channel", description = "The voice channel of the ship"
        ) VoiceChannel channel,
        @SlashOption(
            name = "emissary", description = "The emissary of the ship", autocomplete = EMISSARY_COMPLETE
        ) String emissary
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        info.setEmissary(emissary);

        if (info.isHolding()) {
            info.setHolding(false);
        }

        fleet.getChannelShipInfo().put(channel.getIdLong(), info);
        fleetService.saveFleet(fleet);

        event.reply("Emissary updated.").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.EMISSARY.name());
        MDC.put("ship", channel.getName());
        MDC.put("emissary", emissary);
        MDC.put("updated_by", event.getUser().getId());
        LOGGER.info("Fleet emissary has been updated by {}", event.getUser().getId());
        MDC.clear();
    }

    @JDASlashCommand(
        name = "fleet", subcommand = "holding", description = "Toggles the holding status of a ship"
    )
    public void holding(
        GuildSlashEvent event,
        @SlashOption(
            name = "ship_channel", description = "The voice channel of the ship"
        ) VoiceChannel channel,
        @SlashOption(
            name = "holding", description = "The holding status of the ship"
        ) boolean holding,
        @SlashOption(
            name = "held_by", description = "The user holding the ship"
        ) @Optional Member heldBy
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        if (holding && heldBy == null) {
            info.setHeldBy(event.getUser().getEffectiveName());
        } else if (holding) {
            info.setHeldBy(heldBy.getEffectiveName());
        }

        info.setHolding(holding);
        fleet.getChannelShipInfo().put(channel.getIdLong(), info);
        fleetService.saveFleet(fleet);

        event.reply("Holding status updated.").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.HOLDING.name());
        MDC.put("ship", channel.getName());
        MDC.put("holding", String.valueOf(holding));
        MDC.put("holder", heldBy == null ? event.getUser().getId() : heldBy.getId());
        LOGGER.info("Fleet holding status has been updated by {}", event.getUser().getId());
        MDC.clear();
    }

    @JDASlashCommand(name = "fleet", subcommand = "rename", description = "Renames a fleet")
    public void rename(
        GuildSlashEvent event,
        @SlashOption(name = "id") int id,
        @SlashOption(name = "name") String name
    ) {
        Fleet fleet = fleetService.getFleet(id);
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        fleet.setName(name);
        fleetService.saveFleet(fleet);
        event.reply("Fleet renamed.").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.RENAME.name());
        MDC.put("fleet_name", name);
        MDC.put("renamed_by", event.getUser().getId());
        MDC.put("new_name", name);
        LOGGER.info("Fleet has been renamed by {}", event.getUser().getId());
        MDC.clear();
    }

    @JDASlashCommand(
        name = "fleet", subcommand = "captain", description = "Gets or sets the captain of a ship"
    )
    public void captain(
        GuildSlashEvent event,
        @SlashOption(
            name = "ship_channel", description = "The voice channel of the ship"
        ) VoiceChannel channel,
        @SlashOption(
            name = "captain", description = "The new captain of the ship"
        ) @Optional Member captain
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        if (captain == null) {
            if (info.getCaptain() == -1) {
                event.reply("There is no captain set for this ship.").setEphemeral(true).queue();
                return;
            }

            event.reply(
                "The captain of the ship is %s."
                    .formatted(event.getGuild().getMemberById(info.getCaptain()).getEffectiveName())
            ).setEphemeral(true).queue();
            return;
        }

        if (!info.getMemberIds().contains(captain.getIdLong())) {
            event.reply("That member must be part of the ship to be marked as the captain.")
                .setEphemeral(true).queue();
            return;
        }

        info.setCaptain(captain.getIdLong());
        fleet.getChannelShipInfo().put(channel.getIdLong(), info);
        fleetService.saveFleet(fleet);

        event.reply("Captain updated.").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.CAPTAIN.name());
        MDC.put("ship", channel.getName());
        MDC.put("captain", captain.getId());
        MDC.put("updated_by", event.getUser().getId());
        LOGGER.info("Fleet captain has been updated by {}", event.getUser().getId());
        MDC.clear();
    }

    @JDASlashCommand(
        name = "fleet", subcommand = "captain-remove", description = "Removes the captain of a ship"
    )
    public void removeCaptain(
        GuildSlashEvent event,
        @SlashOption(
            name = "ship_channel", description = "The voice channel of the ship"
        ) VoiceChannel channel
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        info.setCaptain(-1);
        fleet.getChannelShipInfo().put(channel.getIdLong(), info);
        fleetService.saveFleet(fleet);

        event.reply("Captain removed.").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());
    }

    @JDASlashCommand(
            name = "fleet", subcommand = "hop-mode", description = "Toggles your hop mode status"
    )
    public void hopMode(
            GuildSlashEvent event
    ) {
        java.util.Optional<QueuedUser> user = this.queueService.findQueuedUserById(event.getMember().getIdLong());
        if (user.isEmpty()) {
            event.reply("Join the queue so that we can make your profile and then run this again.").setEphemeral(true).queue();
            return;
        }
        
        user.get().setHopMode(!user.get().isHopMode());
        queueService.updateQueuedUser(user.get());

        if (user.get().isHopMode()) {
            event.reply("Your hop mode has been toggled on. If you leave your ship's voice channel while on a fleet, it will not remove you from the boat. Allowing you to pounce around and support other users. Make sure that you manually remove yourself once you are done using the `/fleet remove <voice_channel> <user>.").setEphemeral(true).queue();

        } else {
            event.reply("Your hop mode has been toggled off.").setEphemeral(true).queue();
        }

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.HOP_MODE.name());
        MDC.put("hop_mode", String.valueOf(user.get().isHopMode()));
    }

    @JDASlashCommand(
            name = "fleet", subcommand = "remove", description = "Removes a specific user from a fleet"
    )
    public void remove(
            GuildSlashEvent event,
            @SlashOption(name = "ship_channel") VoiceChannel channel,
            @SlashOption(name = "user") Member user
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());
        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        if (!info.getMemberIds().contains(user.getIdLong())) {
            event.reply("That member must be part of the ship to be removed.").setEphemeral(true).queue();
            return;
        }

        var queuedUser = queueService.findQueuedUserById(user.getIdLong());
        queuedUser.ifPresent(u -> {
            u.setPendingRemoval(null);
            queueService.updateQueuedUser(u);
        });

        info.getMemberIds().remove(user.getIdLong());
        fleet.getChannelShipInfo().put(channel.getIdLong(), info);
        fleetService.saveFleet(fleet);

        event.getGuild().removeRoleFromMember(user, event.getGuild().getRoleById(info.getRoleId())).queue();
        event.getGuild().removeRoleFromMember(user, event.getGuild().getRoleById(fleet.getRoleId())).queue();

        event.reply("User removed.").setEphemeral(true).queue();

        // remove their roles and everything
        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.REMOVE_USER.name());
        MDC.put("ship", channel.getName());
        MDC.put("user_id", user.getId());
        MDC.put("removed_by", event.getUser().getId());
        LOGGER.info("User has been removed from fleet by {}", event.getUser().getId());
        MDC.clear();
    }

    @JDASlashCommand(name = "fleet", subcommand = "override", description = "Set's the max amount of members needed for a ship")
    public void override(
            GuildSlashEvent event,
            @SlashOption(name = "ship_channel") VoiceChannel channel,
            @SlashOption(name = "max_members") int maxMembers
    ) {
        if (maxMembers < 0 && maxMembers != -1) {
            event.reply("You cannot set max members to be less than 0.").setEphemeral(true).queue();
            return;
        }

        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        if (maxMembers < info.getMemberIds().size() && maxMembers != -1) {
            event.reply("You cannot set the max amount of members to be less than the amount of members currently on the ship.").setEphemeral(true).queue();
            return;
        }

        info.setMaxMembers(maxMembers);
        fleet.getChannelShipInfo().put(channel.getIdLong(), info);

        fleetService.saveFleet(fleet);

        messageService.updatePanelEmbed(event.getJDA());

        event.reply("The max amount of members has been updated. To remove this override provide -1 as the max members.").setEphemeral(true).queue();

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.OVERRIDE.name());
        MDC.put("ship", channel.getName());
        MDC.put("max_members", String.valueOf(maxMembers));
        LOGGER.info("Fleet max members has been updated by {}", event.getUser().getId());
        MDC.clear();
    }


    @JDASlashCommand(name = "fleet", subcommand = "info", description = "View all the information about a ship")
    public void info(
            GuildSlashEvent event,
            @SlashOption(name = "ship_channel") VoiceChannel channel
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("That channel is not part of a fleet.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Ship Info", "https://scallycats.com", event.getGuild().getIconUrl());

        StringBuilder text = new StringBuilder();
        text.append("## Ship Information\n");

        text.append("Id: %d\n".formatted(info.getShipId()));
        text.append("Type: %s\n".formatted(info.getType().getPretty()));
        text.append("Emissary: %s\n".formatted(info.getEmissary()));
        text.append("Activity: %s\n".formatted(info.getActivity()));
        text.append("Holding: %s\n".formatted(info.isHolding()));
        text.append("Captain: <@%s>\n".formatted(info.getCaptain()));
        text.append("Fleet Role: <@%s>\n".formatted(info.getRoleId()));
        text.append("Override Max Members: %d\n\n".formatted(info.getMaxMembers()));

        int i = 0;
        for (long member : info.getMemberIds()) {
            text.append("%d. <@%d>".formatted(i++, member));
        }

        embed.setDescription(text.toString());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
