package dev.nateweisz.seacats.queue;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueState {
    public static final String QUEUE_STATE = "QUEUE_STATE";

    @Id
    private String id;

    private boolean queueOpen;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Long> onDutyUsers;
}
