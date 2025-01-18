package dev.nateweisz.seacats.verification;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "blacklisted_users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlacklistedUser {

    @Id
    private long xUid;
    private long userId;

    private long blacklistedAt;
    private long blacklistedBy;
    private String reason;
    private String gamerTag;
}
