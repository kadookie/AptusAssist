package net.kadookie.aptusassist.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.kadookie.aptusassist.service.PushSenderService;
import net.kadookie.aptusassist.service.PushSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushSubscriptionService subscriptionService;
    private final PushSenderService pushSenderService;

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody SubscriptionPayload payload) {
        try {
            subscriptionService.saveIfNotExists(
                    payload.getEndpoint(),
                    payload.getKeys().getP256dh(),
                    payload.getKeys().getAuth());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace(); // 🔍 Add this to see real cause
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Void> test() {
        pushSenderService.sendToAll("Test Notification", "This is a test push message.");
        return ResponseEntity.ok().build();
    }

    @Data
    static class SubscriptionPayload {
        private String endpoint;
        private SubscriptionKeys keys;
    }

    @Data
    static class SubscriptionKeys {
        private String p256dh;
        private String auth;
    }
}
