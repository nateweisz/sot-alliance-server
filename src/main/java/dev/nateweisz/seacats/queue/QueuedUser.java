package dev.nateweisz.seacats.queue;

import java.util.concurrent.TimeUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Entity
@Data
@NoArgsConstructor
public class QueuedUser {

    @Id
    private long memberId;

    @Enumerated(EnumType.STRING)
    private QueueType type;

    private @Nullable Long queueTime;
    private @Nullable Long pendingChannel;
    private @Nullable Long pendingTimeout;
    private @Nullable Long pendingRemoval;
    private @Nullable Long afkTimeout;
    private @Nullable String queueNote;
    private @Nullable String staffNote;
    private @Nullable String priorityReason;
    private boolean sentReminder = false;
    private boolean hopMode = false; // If the user is in hop mode they will be able to join and leave any vc without being removed from the fleet.

    /**
     * @param memberId       the member id
     * @param type           the type of queue, either NORMAL or PRIORITY
     * @param queueTime      the time that they joined the queue or null if no
     *                       longer queued
     * @param pendingChannel the channel that they are currently being processed
     *                       into or null
     * @param pendingTimeout the time that they started being processed or null
     * @param pendingRemoval the time that they left their ship's voice channel or
     *                       null
     * @param queueNote      the note that the user provided when joining the queue
     *                       or null if no longer queued
     * @param staffNote      a note that staff have added to the user or null
     * @param priorityReason the reason that the user has priority or null
     */
    public QueuedUser(
        long memberId, QueueType type, @Nullable Long queueTime, @Nullable Long pendingChannel,
        @Nullable Long pendingTimeout, @Nullable Long pendingRemoval, @Nullable Long afkTimeout, @Nullable String queueNote,
        @Nullable String staffNote, @Nullable String priorityReason
    ) {
        this.memberId = memberId;
        this.type = type;
        this.queueTime = queueTime;
        this.pendingChannel = pendingChannel;
        this.pendingTimeout = pendingTimeout;
        this.pendingRemoval = pendingRemoval;
        this.afkTimeout = afkTimeout;
        this.queueNote = queueNote;
        this.staffNote = staffNote;
        this.priorityReason = priorityReason;
    }

    public boolean isInQueue() {
        return queueNote != null;
    }

    public boolean isPending() {
        return pendingChannel != null;
    }

    public boolean isPendingTimeoutOver() {
        if (pendingTimeout == null) {
            return false;
        }

        return System.currentTimeMillis() - pendingTimeout > TimeUnit.MINUTES.toMillis(3);
    }

    public boolean isPendingRemovalOver() {
        if (pendingRemoval == null) {
            return false;
        }

        return System.currentTimeMillis() - pendingRemoval > TimeUnit.MINUTES.toMillis(3);
    }

    public boolean isAfkTimeoutOver() {
        if (afkTimeout == null) {
            return false;
        }

        return System.currentTimeMillis() - afkTimeout > TimeUnit.MINUTES.toMillis(5);
    }
}
