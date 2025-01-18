package dev.nateweisz.seacats.fleets;

import lombok.Getter;

@Getter
public enum ShipType {
    SLOOP("Sloop", 2), BRIGANTINE("Brigantine", 3), GALLEON("Galleon", 4), CLOSED("Closed", 0), MAN_OF_WAR("Man of War", 16);

    private final String pretty;
    private final int maxMembers;

    ShipType(String pretty, int maxMembers) {
        this.pretty = pretty;
        this.maxMembers = maxMembers;
    }

}
