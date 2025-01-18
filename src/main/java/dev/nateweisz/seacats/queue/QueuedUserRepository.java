package dev.nateweisz.seacats.queue;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueuedUserRepository extends JpaRepository<QueuedUser, Long> {
    List<QueuedUser> findAllByQueueTimeIsNotNull();

    List<QueuedUser> findAllByPendingChannelIsNotNull();

    List<QueuedUser> findAllByPendingRemovalIsNotNull();

    List<QueuedUser> findAllByAfkTimeoutIsNotNull();
}
