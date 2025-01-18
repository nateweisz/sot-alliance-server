package dev.nateweisz.seacats.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueueStateRepository extends JpaRepository<QueueState, String> {}
