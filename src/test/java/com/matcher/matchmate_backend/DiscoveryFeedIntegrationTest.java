package com.matcher.matchmate_backend;

import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.entity.Listing;
import com.matcher.matchmate_backend.model.enums.auth.Gender;
import com.matcher.matchmate_backend.model.enums.auth.PrimaryAuthProvider;
import com.matcher.matchmate_backend.model.enums.lifestyle.CleanlinessLevel;
import com.matcher.matchmate_backend.model.enums.lifestyle.DietaryPreference;
import com.matcher.matchmate_backend.model.enums.lifestyle.GuestTolerance;
import com.matcher.matchmate_backend.model.enums.lifestyle.SleepSchedule;
import com.matcher.matchmate_backend.model.enums.lifestyle.SmokingHabit;
import com.matcher.matchmate_backend.model.enums.listing.FurnishingStatus;
import com.matcher.matchmate_backend.model.enums.listing.ListingType;
import com.matcher.matchmate_backend.model.enums.listing.RoomType;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import com.matcher.matchmate_backend.repository.ListingRepository;
import com.matcher.matchmate_backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Milestone 4 end-to-end test: real PostGIS in a Testcontainers-managed Postgres, hitting
 * GET /api/v1/discovery exactly as the Flutter app would (JWT bearer auth included).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DiscoveryFeedIntegrationTest {

    // Same image docker-compose.yml uses for local dev, so the spatial SQL behaves identically.
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // Central Kolkata, same default the app falls back to.
    private static final double SEEKER_LAT = 22.5726;
    private static final double SEEKER_LNG = 88.3639;

    private String seekerToken;

    @BeforeEach
    void seedData() {
        listingRepository.deleteAll();
        userRepository.deleteAll();

        AppUser seeker = userRepository.save(completeUser("seeker-uid", "Seeker", nearbyPoint(0, 0), true));
        seekerToken = jwtTokenProvider.generateToken(seeker.getId(), seeker.getFirebaseUid());

        // Within the default 10km radius, onboarded, lifestyle fully answered -> should appear with a score.
        AppUser nearHost = userRepository.save(completeUser("near-uid", "Near Host", nearbyPoint(0.02, 0.02), true));
        listingRepository.save(listingFor(nearHost, nearbyPoint(0.02, 0.02)));

        // Far outside the radius (~110km away at this latitude) -> must be excluded by ST_DWithin.
        AppUser farHost = userRepository.save(completeUser("far-uid", "Far Host", nearbyPoint(1.0, 1.0), true));
        listingRepository.save(listingFor(farHost, nearbyPoint(1.0, 1.0)));

        // Nearby but never finished onboarding -> must be excluded (BACKEND_PROGRESS.md: placeholder users).
        AppUser incompleteHost = userRepository.save(completeUser("incomplete-uid", "Incomplete Host", nearbyPoint(0.01, 0.01), false));
        listingRepository.save(listingFor(incompleteHost, nearbyPoint(0.01, 0.01)));
    }

    @Test
    void discoveryFeedAppliesRadiusAndOnboardingFilters() throws Exception {
        mockMvc.perform(get("/api/v1/discovery")
                        .header("Authorization", "Bearer " + seekerToken)
                        .param("lat", String.valueOf(SEEKER_LAT))
                        .param("lng", String.valueOf(SEEKER_LNG))
                        .param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].hostName").value("Near Host"))
                .andExpect(jsonPath("$.content[0].matchPercentage").value(100))
                .andExpect(jsonPath("$.content[0].distanceKm").isNumber());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/discovery"))
                .andExpect(status().isUnauthorized());
    }

    private Point nearbyPoint(double latOffset, double lngOffset) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(SEEKER_LNG + lngOffset, SEEKER_LAT + latOffset));
    }

    private Listing listingFor(AppUser host, Point location) {
        return Listing.builder()
                .hostUser(host)
                .listingType(ListingType.HAVE_ROOM)
                .rentAmount(9000)
                .depositAmount(18000)
                .roomType(RoomType.PRIVATE)
                .furnishingStatus(FurnishingStatus.SEMI_FURNISHED)
                .location(location)
                .localityName("Test Locality")
                .city("Kolkata")
                .isAvailable(true)
                .build();
    }

    private AppUser completeUser(String firebaseUid, String name, Point location, boolean onboardingCompleted) {
        return AppUser.builder()
                .firebaseUid(firebaseUid)
                .primaryAuthProvider(PrimaryAuthProvider.GOOGLE)
                .fullName(name)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(1998, 1, 1))
                .smokingHabit(SmokingHabit.NON_SMOKER)
                .dietaryPreference(DietaryPreference.VEGETARIAN)
                .sleepSchedule(SleepSchedule.NIGHT_OWL)
                .cleanlinessLevel(CleanlinessLevel.VERY_CLEAN)
                .guestTolerance(GuestTolerance.NEVER)
                .lastKnownLocation(location)
                .onboardingCompleted(onboardingCompleted)
                .build();
    }
}
