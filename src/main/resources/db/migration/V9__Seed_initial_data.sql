DO $$
DECLARE
    hashed_password TEXT := '{bcrypt}$2b$12$rexYW8Epm8NqgFAttik0gu61JIj273umhB6Y51VgIuLGnPfOPmTZO';
    role_user_id UUID;
    community1_id UUID;
    match1_id UUID;
    user_email TEXT;
    first_name TEXT;
    nickname TEXT;
BEGIN
    -- Ensure ROLE_USER id is available
    SELECT id INTO role_user_id FROM roles WHERE name = 'ROLE_USER';

    -- Create 30 users with a shared seeded password
    FOR i IN 1..30 LOOP
        user_email := format('seed_user_%s@example.com', lpad(i::TEXT, 2, '0'));
        first_name := 'Seed' || lpad(i::TEXT, 2, '0');
        nickname := 'Seeder' || lpad(i::TEXT, 2, '0');

        INSERT INTO users (email, password, first_name, last_name, nickname)
        VALUES (user_email, hashed_password, first_name, 'Player', nickname)
        ON CONFLICT (email) DO NOTHING;
    END LOOP;

    -- Attach the default ROLE_USER to the seeded accounts
    INSERT INTO user_roles (user_id, role_id, created_at)
    SELECT u.id, role_user_id, NOW()
    FROM users u
    LEFT JOIN user_roles ur
        ON ur.user_id = u.id
        AND ur.role_id = role_user_id
    WHERE u.email LIKE 'seed_user_%@example.com'
      AND ur.id IS NULL;

    -- Create 10 communities
    WITH new_communities AS (
        SELECT *
        FROM (VALUES
            ('Seed Community 01', 'Competitive and friendly matches for everyone.', '123 Main St', 'Metropolis', 'Central Province', 'Countryland', 'ZIP1001', 40.712800, -74.006000),
            ('Seed Community 02', 'Indoor training sessions twice a week.', '45 Elm Ave', 'Riverdale', 'North Province', 'Countryland', 'ZIP1002', 41.878100, -87.629800),
            ('Seed Community 03', 'Weekend competitions and social events.', '78 Oak Blvd', 'Lakeside', 'West Province', 'Countryland', 'ZIP1003', 34.052200, -118.243700),
            ('Seed Community 04', 'Community-focused recreational games.', '12 Pine Road', 'Hillcrest', 'East Province', 'Countryland', 'ZIP1004', 29.760400, -95.369800),
            ('Seed Community 05', 'Advanced drills and coaching programs.', '99 Cedar Lane', 'Harborview', 'South Province', 'Countryland', 'ZIP1005', 33.448400, -112.074000),
            ('Seed Community 06', 'Youth development and mentorship.', '5 Maple Court', 'Sunnyvale', 'Central Province', 'Countryland', 'ZIP1006', 37.774900, -122.419400),
            ('Seed Community 07', 'Mixed-ability fun matches.', '350 Spruce St', 'Greenfields', 'North Province', 'Countryland', 'ZIP1007', 47.606200, -122.332100),
            ('Seed Community 08', 'Fitness and conditioning bootcamps.', '220 Birch Way', 'Silverstone', 'West Province', 'Countryland', 'ZIP1008', 39.739200, -104.990300),
            ('Seed Community 09', 'Friendly five-a-side tournaments.', '18 Willow Park', 'Bluewater', 'East Province', 'Countryland', 'ZIP1009', 32.776700, -96.797000),
            ('Seed Community 10', 'Beginner-friendly introductory sessions.', '74 Aspen Loop', 'Clearview', 'South Province', 'Countryland', 'ZIP1010', 25.761700, -80.191800)
        ) AS nc(name, description, address, city, province, country, post_code, latitude, longitude)
    )
    INSERT INTO communities (name, description, address, city, province, country, post_code, latitude, longitude)
    SELECT nc.name, nc.description, nc.address, nc.city, nc.province, nc.country, nc.post_code, nc.latitude, nc.longitude
    FROM new_communities nc
    WHERE NOT EXISTS (
        SELECT 1
        FROM communities c
        WHERE c.name = nc.name
    );

    -- Capture the first community id for memberships and matches
    SELECT id INTO community1_id
    FROM communities
    WHERE name = 'Seed Community 01';

    -- Enroll the first 20 seeded users into the first community
    INSERT INTO community_members (community_id, user_id, is_active, created_at)
    SELECT community1_id, u.id, TRUE, NOW()
    FROM (
        SELECT u.id
        FROM users u
        WHERE u.email LIKE 'seed_user_%@example.com'
        ORDER BY u.email
        LIMIT 20
    ) AS u
    WHERE NOT EXISTS (
        SELECT 1
        FROM community_members cm
        WHERE cm.community_id = community1_id
          AND cm.user_id = u.id
    );

    -- Create 5 matches across the seeded communities
    WITH new_matches AS (
        SELECT *
        FROM (VALUES
            ('Seed Match 01', 'Season opener friendly under the lights.', 'Seed Community 01', NOW() + INTERVAL '10 days', NOW() + INTERVAL '5 days', 11, 'REGISTRATION_OPEN'),
            ('Seed Match 02', 'Tactical training scrimmage.', 'Seed Community 01', NOW() + INTERVAL '17 days', NOW() + INTERVAL '12 days', 9, 'REGISTRATION_OPEN'),
            ('Seed Match 03', 'Community derby showdown.', 'Seed Community 02', NOW() + INTERVAL '24 days', NOW() + INTERVAL '18 days', 11, 'REGISTRATION_OPEN'),
            ('Seed Match 04', 'Weekend cup qualifier.', 'Seed Community 03', NOW() + INTERVAL '31 days', NOW() + INTERVAL '24 days', 7, 'REGISTRATION_OPEN'),
            ('Seed Match 05', 'Friendly exhibition match.', 'Seed Community 04', NOW() + INTERVAL '38 days', NOW() + INTERVAL '30 days', 5, 'REGISTRATION_OPEN')
        ) AS nm(title, description, community_name, match_date, registration_deadline, players_per_team, status)
    )
    INSERT INTO matches (title, description, community_id, match_date, registration_deadline, players_per_team, status, is_auto_selection)
    SELECT nm.title,
           nm.description,
           c.id,
           nm.match_date,
           nm.registration_deadline,
           nm.players_per_team,
           nm.status::match_status,
           TRUE
    FROM new_matches nm
    JOIN communities c
      ON c.name = nm.community_name
    WHERE NOT EXISTS (
        SELECT 1
        FROM matches m
        WHERE m.title = nm.title
    );

    -- Capture the first seeded match id
    SELECT id INTO match1_id
    FROM matches
    WHERE title = 'Seed Match 01';

    -- Mark availability for the first 20 seeded users in the first match
    INSERT INTO availabilities (match_id, user_id, status, created_at)
    SELECT match1_id, u.id, 'AVAILABLE'::availability_status, NOW()
    FROM (
        SELECT u.id
        FROM users u
        WHERE u.email LIKE 'seed_user_%@example.com'
        ORDER BY u.email
        LIMIT 20
    ) AS u
    WHERE NOT EXISTS (
        SELECT 1
        FROM availabilities a
        WHERE a.match_id = match1_id
          AND a.user_id = u.id
    );
END $$;

