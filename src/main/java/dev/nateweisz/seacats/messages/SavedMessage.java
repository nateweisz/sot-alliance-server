package dev.nateweisz.seacats.messages;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "messages")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SavedMessage {
    @Id
    private long id;

    private long channelId;

    @Enumerated(EnumType.STRING)
    private MessageType type;
}
