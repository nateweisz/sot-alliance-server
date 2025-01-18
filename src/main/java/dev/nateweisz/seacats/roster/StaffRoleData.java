package dev.nateweisz.seacats.roster;

import java.util.List;

public record StaffRoleData(
        long roleId,
        List<Long> staffMembers
) {}
