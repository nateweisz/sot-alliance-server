package dev.nateweisz.seacats.fleets;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FleetRepository extends JpaRepository<Fleet, Integer> {
    Fleet findByCategoryId(long categoryId);

    boolean existsByName(@NotNull String name);

    Fleet findByName(@NotNull String name);

    void deleteByName(@NotNull String name);

    @Query("SELECT f FROM fleets f WHERE KEY(f.channelShipInfo) = :channelId")
    Optional<Fleet> findByChannelId(@Param("channelId") long channelId);
}
