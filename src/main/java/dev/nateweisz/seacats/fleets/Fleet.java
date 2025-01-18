package dev.nateweisz.seacats.fleets;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "fleets")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Fleet {

    @Id
    private int numericalId;

    private String name;

    private long startedBy;
    private long categoryId;
    private long roleId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fleet_channel_ship_info", joinColumns = @JoinColumn(name = "fleet_id"))
    @MapKeyColumn(name = "channel_id")
    @Builder.Default
    private Map<Long, ShipInfo> channelShipInfo = new HashMap<>();
}
