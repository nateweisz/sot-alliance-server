package dev.nateweisz.seacats.messages;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<SavedMessage, Long> {
    void deleteByType(MessageType type);

    Optional<SavedMessage> findByType(MessageType type);
}
