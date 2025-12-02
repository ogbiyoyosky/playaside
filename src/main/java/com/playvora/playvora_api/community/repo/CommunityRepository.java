package com.playvora.playvora_api.community.repo;

import com.playvora.playvora_api.community.entities.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityRepository extends JpaRepository<Community, UUID> {

    @Query("""
        SELECT c FROM Community c 
        WHERE (:name IS NULL OR LOWER(c.name) LIKE :name)
          AND (:city IS NULL OR LOWER(c.city) LIKE :city)
          AND (:province IS NULL OR LOWER(c.province) LIKE :province)
          AND (:country IS NULL OR LOWER(c.country) LIKE :country)
    """)
    Page<Community> searchCommunities(@Param("name") String name,
                                      @Param("city") String city,
                                      @Param("province") String province,
                                      @Param("country") String country,
                                      Pageable pageable);

    @Query("SELECT c FROM Community c WHERE c.id = :id")
    Optional<Community> findByIdWithMembers(@Param("id") UUID id);

    boolean existsByName(String name);

    @Query("""
        SELECT c FROM Community c 
        WHERE (:search IS NULL 
               OR LOWER(c.name) LIKE :search
               OR LOWER(c.city) LIKE :search
               OR LOWER(c.province) LIKE :search
               OR LOWER(c.country) LIKE :search)
    """)
    Page<Community> searchAllCommunities(@Param("search") String search, Pageable pageable);
}