package com.playvora.playvora_api.venue.repo;

import com.playvora.playvora_api.venue.entities.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
}


