package net.kadookie.aptusassist.service;

import lombok.RequiredArgsConstructor;
import net.kadookie.aptusassist.entity.PushSubscription;
import net.kadookie.aptusassist.repository.PushSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository repository;

    public void saveIfNotExists(String endpoint, String p256dh, String auth) {
        if (!repository.existsByEndpoint(endpoint)) {
            PushSubscription subscription = PushSubscription.builder()
                    .endpoint(endpoint)
                    .p256dh(p256dh)
                    .auth(auth)
                    .build();
            repository.save(subscription);
        }
    }

    public List<PushSubscription> getAll() {
        return repository.findAll();
    }

    public Optional<PushSubscription> getByEndpoint(String endpoint) {
        return repository.findByEndpoint(endpoint);
    }
}
