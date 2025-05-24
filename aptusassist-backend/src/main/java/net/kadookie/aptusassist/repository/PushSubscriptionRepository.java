package net.kadookie.aptusassist.repository;

import net.kadookie.aptusassist.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);

    boolean existsByEndpoint(String endpoint);
}
