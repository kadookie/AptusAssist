package net.kadookie.aptusassist.controller;

import lombok.RequiredArgsConstructor;
import net.kadookie.aptusassist.service.PushSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushSubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody Map<String, Object> body) {
        try {
            Map<String, String> keys = (Map<String, String>) body.get("keys");
            String endpoint = (String) body.get("endpoint");
            String p256dh = keys.get("p256dh");
            String auth = keys.get("auth");

            subscriptionService.saveIfNotExists(endpoint, p256dh, auth);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<?> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAll());
    }
}
