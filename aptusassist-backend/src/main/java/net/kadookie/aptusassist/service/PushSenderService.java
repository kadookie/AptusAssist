package net.kadookie.aptusassist.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kadookie.aptusassist.entity.PushSubscription;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushSenderService {

    private final PushSubscriptionService subscriptionService;

    @Value("${VAPID_PUBLIC_KEY}")
    private String vapidPublicKey;

    @Value("${VAPID_PRIVATE_KEY}")
    private String vapidPrivateKey;

    @Value("${VAPID_SUBJECT}")
    private String vapidSubject;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void registerProvider() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public void sendToAll(String title, String message) {
        List<PushSubscription> subscriptions = subscriptionService.getAll();
        for (PushSubscription sub : subscriptions) {
            try {
                sendNotification(sub, title, message);
            } catch (Exception e) {
                log.warn("Failed to send push to {}", sub.getEndpoint(), e);
            }
        }
    }

    private void sendNotification(PushSubscription sub, String title, String body) throws Exception {
        String payload = objectMapper.writeValueAsString(new NotificationPayload(title, body));

        Notification notification = new Notification(
                sub.getEndpoint(),
                sub.getP256dh(),
                sub.getAuth(),
                payload);

        KeyPair keyPair = new KeyPair(
                Utils.loadPublicKey(vapidPublicKey),
                Utils.loadPrivateKey(vapidPrivateKey));

        PushService pushService = new PushService();
        pushService.setSubject(vapidSubject);
        pushService.setPublicKey((ECPublicKey) keyPair.getPublic());
        pushService.setPrivateKey((ECPrivateKey) keyPair.getPrivate());

        HttpResponse response = pushService.send(notification);
        log.info("Push sent: {} -> {}", response.getStatusLine().getStatusCode(), sub.getEndpoint());
    }

    private record NotificationPayload(String title, String body) {
    }
}
