package dev.nateweisz.seacats.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class V2QueuedService {
    private final QueuedUserRepository queuedUserRepository;
    private final QueueStateRepository queueStateRepository;

    public V2QueuedService(
        QueuedUserRepository queuedUserRepository, QueueStateRepository queueStateRepository
    ) {
        this.queuedUserRepository = queuedUserRepository;
        this.queueStateRepository = queueStateRepository;
    }

    @Cacheable(value = "queueState", key = "'queueOpenState'")
    public boolean isQueueOpen() {
        return queueStateRepository.findById(QueueState.QUEUE_STATE)
            .map(QueueState::isQueueOpen)
            .orElse(false);
    }

    @Transactional
    @CacheEvict(value = "queueState", key = "'queueOpenState'")
    public boolean toggleQueue() {
        boolean newState = !isQueueOpen();
        queueStateRepository
            .save(new QueueState(QueueState.QUEUE_STATE, newState, new ArrayList<>()));
        return newState;
    }

    @Cacheable(value = "queuedUsers", key = "'allUsers'")
    public List<QueuedUser> findAllQueuedUsers() {
        return queuedUserRepository.findAllByQueueTimeIsNotNull();
    }

    @Cacheable(value = "queueState", key = "'onDutyUsers'")
    public List<Long> findAllOnDutyUsers() {
        return queueStateRepository.findById(QueueState.QUEUE_STATE)
            .map(QueueState::getOnDutyUsers)
            .orElse(new ArrayList<>());
    }

    @Transactional
    @CacheEvict(value = "queueState", key = "'onDutyUsers'")
    public void addOnDutyUser(long memberId) {
        queueStateRepository.findById(QueueState.QUEUE_STATE)
            .ifPresent(state -> {
                state.getOnDutyUsers().add(memberId);
                queueStateRepository.save(state);
            });
    }

    @Transactional
    @CacheEvict(value = "queueState", key = "'onDutyUsers'")
    public void removeOnDutyUser(long memberId) {
        queueStateRepository.findById(QueueState.QUEUE_STATE)
            .ifPresent(state -> {
                state.getOnDutyUsers().remove(memberId);
                queueStateRepository.save(state);
            });
    }

    @Cacheable(value = "queuedUsers", key = "'queueEmpty_' + #type")
    public boolean isQueueEmpty(QueueType type) {
        return findAllQueuedUsers().stream()
            .noneMatch(u -> u.getType() == type);
    }

    @Cacheable(value = "queuedUsers", key = "'user_' + #id")
    public Optional<QueuedUser> findQueuedUserById(long id) {
        return queuedUserRepository.findById(id);
    }

    @Transactional
    @CacheEvict(value = "queuedUsers", allEntries = true)
    public void updateQueuedUser(QueuedUser queuedUser) {
        queuedUserRepository.save(queuedUser);
    }

    @Transactional
    @CacheEvict(value = "queuedUsers", allEntries = true)
    public void joinQueue(long memberId, QueueType type, String note) {
        QueuedUser user =
            new QueuedUser(
                memberId,
                type,
                System.currentTimeMillis(),
                null,
                null,
                null,
                null,
                note,
                null,
                null
            );

        queuedUserRepository.save(user);
    }

    @Cacheable(value = "queuedUsers", key = "'pendingMembers'")
    public List<QueuedUser> getPendingMembers() {
        return queuedUserRepository.findAllByPendingChannelIsNotNull();
    }

    @Cacheable(value = "queuedUsers", key = "'pendingRemovals'")
    public List<QueuedUser> getPendingRemovals() {
        return queuedUserRepository.findAllByPendingRemovalIsNotNull();
    }

    @Cacheable(value = "queuedUsers", key = "'pendingAfk'")
    public List<QueuedUser> getPendingAfk() {
        return queuedUserRepository.findAllByAfkTimeoutIsNotNull();
    }
}
