package com.matcher.matchmate_backend.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.entity.Listing;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import com.matcher.matchmate_backend.repository.ListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AppUserRepository userRepository;
    private final ListingRepository listingRepository;
    private final MatchingHeuristicEngine matchingEngine;

    public NotificationService(
            AppUserRepository userRepository,
            ListingRepository listingRepository,
            MatchingHeuristicEngine matchingEngine
    ) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.matchingEngine = matchingEngine;
    }

    /**
     * Sends an individual FCM push notification asynchronously.
     */
    @Async
    public void sendPush(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank()) return;

        if (FirebaseApp.getApps().isEmpty()) {
            log.info("[FCM] (Mock) To: {} | Title: {} | Body: {} | Data: {}", fcmToken, title, body, data);
            return;
        }

        try {
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setNotification(AndroidNotification.builder()
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .setChannelId("matchmate_matches")
                            .build())
                    .build();

            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .setAndroidConfig(androidConfig)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            FirebaseMessaging.getInstance().sendAsync(builder.build());
            log.info("[FCM] Successfully queued push to token {} with title: '{}'", fcmToken, title);
        } catch (Exception e) {
            log.warn("[FCM] Failed to dispatch push notification: {}", e.getMessage());
        }
    }

    /**
     * Triggered when a new room is listed (either by a user or admin).
     * Finds all room seekers whose search radius covers the new listing's location,
     * calculates the compatibility match percentage, and notifies them.
     */
    @Async
    public void notifyMatchingSeekersForNewListing(Listing listing) {
        if (listing == null || listing.getLocation() == null || listing.getHostUser() == null) return;

        try {
            List<AppUser> matchingSeekers = userRepository.findMatchingSeekersForListing(
                    listing.getLocation(),
                    listing.getHostUser().getId()
            );

            log.info("[FCM] Found {} matching seekers within radius for listing #{}", matchingSeekers.size(), listing.getId());

            for (AppUser seeker : matchingSeekers) {
                if (seeker.getFcmToken() == null || seeker.getFcmToken().isBlank()) continue;

                MatchingHeuristicEngine.MatchResult match = matchingEngine.evaluate(seeker, listing.getHostUser());
                int matchPercent = (match != null && match.matchPercentage() != null)
                        ? match.matchPercentage()
                        : 85;

                String roomTypeStr = listing.getRoomType() != null
                        ? listing.getRoomType().name().replace('_', ' ')
                        : "place";

                String title = "\uD83C\uDFE0 New Room Match! (" + matchPercent + "% Match)";
                String body = listing.getHostUser().getFullName() + " listed a " + roomTypeStr
                        + " in " + listing.getLocalityName() + " within your search radius.";

                Map<String, String> data = new HashMap<>();
                data.put("type", "ROOM_LISTING_MATCH");
                data.put("listingId", String.valueOf(listing.getId()));
                data.put("matchPercentage", String.valueOf(matchPercent));
                data.put("hostUserId", String.valueOf(listing.getHostUser().getId()));
                data.put("locality", listing.getLocalityName() != null ? listing.getLocalityName() : "");
                data.put("route", "/discover/room/" + listing.getId());
                data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

                sendPush(seeker.getFcmToken(), title, body, data);
            }
        } catch (Exception e) {
            log.error("[FCM] Error notifying seekers for listing #{}: {}", listing.getId(), e.getMessage());
        }
    }

    /**
     * Triggered when another user comments on a host's listing. The host is taken
     * from the caller (already loaded inside the request transaction) so this
     * async method never touches a lazy proxy.
     */
    @Async
    public void notifyHostOfNewComment(Listing listing, AppUser host, AppUser author, String content) {
        if (listing == null || host == null || author == null) return;
        if (host.getFcmToken() == null || host.getFcmToken().isBlank()) return;

        try {
            String roomLabel = listing.getTitle() != null && !listing.getTitle().isBlank()
                    ? listing.getTitle()
                    : "your room in " + (listing.getLocalityName() != null ? listing.getLocalityName() : "your area");
            String preview = content == null ? "" : content.trim();
            if (preview.length() > 90) preview = preview.substring(0, 87) + "…";

            String title = "💬 " + author.getFullName() + " asked about " + roomLabel;
            String body = preview.isEmpty() ? "Open the listing to reply." : "\"" + preview + "\"";

            Map<String, String> data = new HashMap<>();
            data.put("type", "LISTING_COMMENT");
            data.put("listingId", String.valueOf(listing.getId()));
            data.put("commentAuthorId", String.valueOf(author.getId()));
            data.put("route", "/discover/room/" + listing.getId());
            data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

            sendPush(host.getFcmToken(), title, body, data);
        } catch (Exception e) {
            log.error("[FCM] Error notifying host #{} about a comment on listing #{}: {}",
                    host.getId(), listing.getId(), e.getMessage());
        }
    }

    /**
     * Triggered when a room finder updates their target location / search radius.
     * Finds active listings within that radius, calculates the compatibility match percentage,
     * and notifies the hosts that an interested seeker matches their room.
     */
    @Async
    public void notifyHostsForNewSeeker(AppUser seeker) {
        if (seeker == null || seeker.getTargetSearchLocation() == null) return;

        try {
            double radiusKm = seeker.getSearchRadiusKm() != null ? seeker.getSearchRadiusKm() : 10.0;
            double radiusMeters = radiusKm * 1000.0;

            List<Listing> nearbyListings = listingRepository.findActiveListingsWithinRadius(
                    seeker.getTargetSearchLocation(),
                    radiusMeters,
                    seeker.getId()
            );

            log.info("[FCM] Found {} active listings for seeker #{} search radius ({} km)",
                    nearbyListings.size(), seeker.getId(), radiusKm);

            for (Listing listing : nearbyListings) {
                AppUser host = listing.getHostUser();
                if (host == null || host.getFcmToken() == null || host.getFcmToken().isBlank()) continue;

                MatchingHeuristicEngine.MatchResult match = matchingEngine.evaluate(seeker, host);
                int matchPercent = (match != null && match.matchPercentage() != null)
                        ? match.matchPercentage()
                        : 85;

                String title = "\uD83D\uDC4B New Roommate Match! (" + matchPercent + "% Match)";
                String body = seeker.getFullName() + " is looking for a room in your area ("
                        + matchPercent + "% compatibility match).";

                Map<String, String> data = new HashMap<>();
                data.put("type", "ROOM_SEEKER_MATCH");
                data.put("seekerUserId", String.valueOf(seeker.getId()));
                data.put("matchPercentage", String.valueOf(matchPercent));
                data.put("listingId", String.valueOf(listing.getId()));
                data.put("route", "/users/" + seeker.getId());
                data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

                sendPush(host.getFcmToken(), title, body, data);
            }
        } catch (Exception e) {
            log.error("[FCM] Error notifying hosts for seeker #{}: {}", seeker.getId(), e.getMessage());
        }
    }
}
