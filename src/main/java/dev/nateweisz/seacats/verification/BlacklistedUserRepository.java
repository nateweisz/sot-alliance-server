package dev.nateweisz.seacats.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlacklistedUserRepository extends JpaRepository<BlacklistedUser, Long> {
    boolean existsByUserId(long userId);
    List<BlacklistedUser> findAllByUserId(long userId);
    void deleteAllByUserId(long userId);
    boolean existsByxUid(long xUid);

}
