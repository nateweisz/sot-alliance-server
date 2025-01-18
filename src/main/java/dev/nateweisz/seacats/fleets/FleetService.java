package dev.nateweisz.seacats.fleets;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
public class FleetService {
    private final FleetRepository fleetRepository;
    // private final MetricsTracker metricsTracker;

    public FleetService(FleetRepository fleetRepository) {
        this.fleetRepository = fleetRepository;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onLoad() {
        // Load channel members into cache here and all members of fleet
    }

    public boolean fleetExists(String name) {
        return fleetRepository.existsByName(name);
    }

    public boolean isAnyFleetOpen() {
        return !fleetRepository.findAll().isEmpty();
    }

    public void createFleet(Fleet fleet) {
        fleetRepository.save(fleet);

        // record metric here
        // metricsTracker.incrementFleetsOpened();
    }

    public void saveFleet(Fleet fleet) {
        fleetRepository.save(fleet);
    }

    public Fleet getFleet(String name) {
        return fleetRepository.findByName(name);
    }

    public Fleet getFleet(int id) {
        return fleetRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteFleet(String name) {
        fleetRepository.deleteByName(name);

        // record metric here
        // metricsTracker.decrementFleetsOpened();
    }

    public List<Fleet> getAllFleets() {
        return fleetRepository.findAll();
    }

    public Fleet getFleetByCategory(long categoryId) {
        return fleetRepository.findByCategoryId(categoryId);
    }

    public Optional<Fleet> getFleetByChannel(long channelId) {
        return fleetRepository.findByChannelId(channelId);
    }
}
