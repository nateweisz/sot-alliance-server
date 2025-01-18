package dev.nateweisz.seacats.fleets;

import java.util.List;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipInfo {
    private int shipId;
    private ShipType type = ShipType.SLOOP;
    private List<Long> memberIds;
    private String activity;
    private String emissary;
    private boolean holding;
    private String heldBy;
    private long captain;
    private boolean initialized;
    private long roleId;
    private int maxMembers; // when it's set to -1 there is no override

    public void addMember(long memberId) {
        memberIds.add(memberId);
    }
}
