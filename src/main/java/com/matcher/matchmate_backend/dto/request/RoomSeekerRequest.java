package com.matcher.matchmate_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PATCH /api/v1/users/me/room-seeker — flips the "actively looking for a room"
 * flag from the Discover header toggle. Accepts {@code is_room_seeker} (the
 * documented key) as well as the camelCase {@code isRoomSeeker}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSeekerRequest {

    @NotNull(message = "is_room_seeker is required")
    @JsonProperty("is_room_seeker")
    @JsonAlias({"isRoomSeeker", "is_looking_for_room", "isLookingForRoom"})
    private Boolean isRoomSeeker;
}
