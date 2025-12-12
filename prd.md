## Playvora API – Product Requirements Document (PRD)

### 1. Overview

Playvora is a backend platform for organizing and playing community sports matches. It provides:

- Authentication and user management
- Communities and membership
- Match creation and lifecycle management
- Real‑time team draft and match updates
- Match, private, and community chat
- Venues and bookings
- Wallet and payments
- Notifications (WebSocket + push)

The primary clients are a web frontend and mobile apps consuming REST + WebSocket APIs.

### 2. Objectives & Success Criteria

- **Enable organizers and community admins** to create, publish, and manage matches with minimal friction.
- **Enable players** to discover matches, express availability, participate in team drafts, and chat.
- **Provide reliable real‑time updates** for team selection, match state, and chats.
- **Support safe, auditable payments** via wallet-based flows.
- **Be secure, multi-tenant, and scalable**, ready for production workloads.

Success indicators:

- Organizers can create and manage matches end‑to‑end without manual workarounds.
- Players can join communities, register for matches, and see accurate real‑time state.
- Message delivery (chat and match updates) is reliable with no “lost” events.
- Payments and wallet balances stay consistent and traceable.

### 3. Users & Roles

- **Player**
  - Joins communities and matches.
  - Marks availability and participates in team drafts.
  - Uses chat (match chat, private DMs, community chat).
- **Captain**
  - All player capabilities.
  - Additional permissions for manual team selection during drafts.
- **Community Admin**
  - Creates and manages communities.
  - Manages community members and roles.
  - Creates and manages matches for their community.
- **System Admin (internal)**
  - Operational tools (debugging, data inspection via DB/Swagger).
  - Not a separate product role yet, but assumed for operations.

### 4. System Architecture (High Level)

- **API Layer (REST – HTTP)**
  - Versioned under `/api/v1/**`.
  - Documented via Swagger / OpenAPI.
- **Real‑time Layer (WebSocket / STOMP)**
  - WebSocket endpoint: `/ws`
  - STOMP prefixes: `/app` (send), `/topic` (broadcast), `/user` (per‑user queues).
  - Common envelope: `WebSocketMessage { type, messageId, timestamp, data, message? }`.
- **Security & Auth**
  - Google OAuth2 login (`/api/v1/auth/authorize/google`).
  - JWT-based auth stored in HTTP cookies (`access_token`, `refresh_token`).
  - Spring Security for route protection and WebSocket authentication.
- **Persistence**
  - PostgreSQL with Flyway migrations (`db/migration`).
  - Entities for users, communities, matches, registrations, payments, wallets, chats, etc.
- **Integrations**
  - Stripe (via `StripeConfig`) for payment processing.
  - S3 (via `S3Config`) for file storage.
  - Push notification providers (abstracted via notification services).

### 5. Functional Areas

#### 5.1 Authentication & Onboarding

**Requirements**

- **Google OAuth2 login**
  - Endpoint: `GET /api/v1/auth/authorize/google` starts OAuth flow.
  - Callback handled by Spring Security at `/api/v1/auth/callback/google`.
  - On success, frontend is redirected (e.g., to `https://expitra.com`) with cookies set:
    - `access_token` – short‑lived JWT.
    - `refresh_token` – longer‑lived refresh token.
- **Refresh tokens**
  - Backend persists refresh tokens (see `RefreshToken` entity) and exposes a refresh endpoint.
  - Old refresh tokens can be invalidated/rotated.
- **Password reset (email flow)**
  - API to request password reset (`PasswordResetRequest`) generating a `PasswordResetToken`.
  - API to confirm reset (`PasswordResetConfirmRequest`) and set a new password.
- **Basic user profile**
  - User entity includes identity info (name, email, avatar, etc.).
  - API to fetch and update the authenticated user’s profile (e.g. `/api/v1/user/profile`).

**Non‑Goals (for now)**

- Social login providers other than Google.
- Fine‑grained per‑endpoint permission management beyond current role model.

#### 5.2 Communities & Membership

**Core capabilities**

- Create communities (e.g. clubs, groups) with metadata (name, description, location, etc.).
- Search communities (`CommunitySearchRequest`) with pagination and filters.
- Update community details (name, description, visibility).
- Manage members via `CommunityMember`:
  - Invite/add/remove members.
  - Assign roles (captain, admin, member) via `AssignRoleRequest`.
- Fetch community details and member lists.

**Constraints / Behaviour**

- Only community admins can:
  - Update community settings.
  - Assign or revoke elevated roles.
- Players can join public communities or be invited to restricted ones (exact rules defined by service).

#### 5.3 Matches & Events

**Match lifecycle**

- **Create match event**
  - `POST /api/v1/match-events`
  - Includes title, description, date/time, capacity, pricing, community, venue/location data, etc.
- **Read / list matches**
  - Paginated list with sorting and optional search:
    - `GET /api/v1/match-events`
    - `GET /api/v1/match-events/upcoming`
    - `GET /api/v1/match-events/community/{communityId}`
    - `GET /api/v1/match-events/my-matches` (matches where user has availability/registration).
  - Detailed single match:
    - `GET /api/v1/match-events/{id}` (includes community, teams, availabilities, etc.).
- **Update & delete**
  - `PATCH /api/v1/match-events/{id}` and `DELETE /api/v1/match-events/{id}` (owner/admin only).
- **Cancel match**
  - `POST /api/v1/match-events/{id}/cancel` – mark match as cancelled; notify participants.

**Availability & registration**

- Players can mark their availability for a match:
  - `POST /api/v1/match-events/{matchId}/mark-availability` with `AvailabilityRequest`.
- Players can remove their availability:
  - `DELETE /api/v1/match-events/{id}/availability`.
- System tracks registrations/availability for capacity enforcement and waitlisting logic.

**Teams & captains**

- **Team management**
  - Assign players to teams (manual and/or algorithmic).
  - Each team has a captain flag.
- **Assign captain**
  - `POST /api/v1/match-events/teams/{teamId}/assign-captain/{userId}`.

#### 5.4 Real‑time Draft & Match Updates

**WebSocket endpoints (match events)**

- **Send (client → server, `/app` prefix):**
  - `/app/match-events/{matchId}/select-player`
  - `/app/match-events/{matchId}/generate-teams`
  - `/app/match-events/{matchId}/start`
  - `/app/match-events/{matchId}/complete`
  - `/app/match-events/{matchId}/chat`
- **Subscribe (server → client, `/topic` prefix):**
  - `/topic/match/{matchId}/updates` – match + draft updates.
  - `/topic/match/{matchId}/chat` – match chat stream.

**Draft behaviour**

- Only the current picking team’s captain may select a player.
- When a player is selected:
  - Match teams and availability are updated in the DB.
  - Updated match state is broadcast to `/topic/match/{matchId}/updates` with:
    - `type = "team_selection"` or `type = "match_update"`.
  - Selected player is removed from the available list; UI reflects the change in real time.
- Draft completion:
  - When all teams reach capacity:
    - `draftInProgress` becomes `false`.
    - Remaining players can be moved to a reserve team.
    - Match status updates to something like `TEAMS_SELECTED`.

**REST alternatives**

- Player selection via HTTP:
  - `POST /api/v1/match-events/match/{matchId}/select-player?teamId={teamId}&userId={userId}`.
  - `POST /api/v1/match-events/{matchId}/teams/{teamId}/select-player/{userId}` (variant).
- Team generation via HTTP:
  - `POST /api/v1/match-events/match/{matchId}/generate-teams`.

#### 5.5 Chat (Match, Private, Community)

**Match chat**

- Real‑time group chat for each match:
  - Send: `/app/match-events/{matchId}/chat`.
  - Subscribe: `/topic/match/{matchId}/chat`.
- Chat messages persisted in `ChatMessage` with metadata (sender, match, timestamps).
- Read state tracking via `MatchChatReadState` so the system can compute unread counts.
- HTTP endpoints (via `ChatHistoryController`) for:
  - Fetching paginated match chat history.
  - Marking messages as read.

**Private (direct) chat**

- WebSocket‑only real‑time private messages:
  - Send: `/app/chat/private/{recipientId}` with `PrivateChatMessageRequest`.
  - Subscribe: `/user/queue/private-chat` for all conversations the user participates in.
- Payload: `PrivateChatMessageResponse` with message IDs, participants, content, timestamps, and a stable `conversationId`.
- Messages are delivered to both sender and recipient; push notifications may be triggered separately.

**Community chat**

- Group chat scoped to a community:
  - Send: `/app/communities/{communityId}/chat` with `CommunityChatMessageRequest`.
  - Subscribe: `/topic/communities/{communityId}/chat`.
- Only active members of the community may send messages.
- Messages are persisted and can be fetched via REST history endpoints.

**Per‑user notifications & errors**

- Per‑user queues:
  - `/user/queue/notifications` – generic notifications (e.g. selection confirmations).
  - `/user/queue/errors` – non‑fatal WebSocket errors.
- All error payloads use `type = "error"` in the `WebSocketMessage` envelope.

#### 5.6 Locations & Venues

**Location services**

- Expose APIs to:
  - Resolve/normalize geographic locations for matches and venues.
  - Search for nearby matches or communities based on location filters.
- Typed DTOs for location requests and responses (e.g., city, country, coordinates).

**Venues & bookings**

- Model venues (`Venue` entity) with:
  - Basic info (name, address, surface type, etc.).
  - Availability slots / opening hours (as supported by current schema).
- Bookings:
  - `VenueBooking` entities represent match‑to‑venue reservations.
  - APIs to:
    - Create a venue booking for a match (admin/organizer only).
    - Query bookings for a venue or match.

#### 5.7 Wallet & Payments

**Wallet**

- Each user may have a wallet record with:
  - Current balance.
  - Currency.
  - Timestamps and ownership fields.
- Key flows:
  - Top‑up (deposit) from card/Stripe.
  - Deduct fees for joining or hosting matches.
  - View wallet balance and transaction history.

**Payments**

- Payment entities represent:
  - Wallet deposits.
  - Match-related charges (e.g. participation fees).
  - Refunds and adjustments if implemented.
- REST endpoints:
  - Initialize a payment (e.g., create Stripe PaymentIntent).
  - Confirm/update payment once Stripe notifies success.
  - List payments for a user.
- Integration considerations:
  - Use `StripeConfig` for API keys and environment configuration.
  - All amounts should use minor units (e.g. cents) and map to well‑defined currencies via `CurrencyMapper`.

#### 5.8 Files & Media

- **User file uploads**
  - Store user avatars and attachments in S3 (via `FileService` + `S3Config`).
  - DB table for `user_files` tracks:
    - Owner user ID.
    - File path / key in S3.
    - File type and metadata.
- **Endpoints**
  - Upload file for current user.
  - Fetch file by ID or presigned URL.
  - Delete/update file metadata as needed.

#### 5.9 Notifications

- **Push notifications**
  - Device tokens managed via a dedicated table.
  - `IPushNotificationService` + implementation to send push notifications for:
    - New match chat messages.
    - New private or community messages.
    - Match status changes (start, cancelled, teams ready).
- **Email**
  - `IMailService` for sending transactional emails:
    - Password reset.
    - Waitlist or registration confirmations.
    - Match reminders (if configured).

### 6. Non‑Functional Requirements

- **Performance**
  - Support at least hundreds of concurrent WebSocket connections per node.
  - Latency for WebSocket events ideally < 300ms door‑to‑door under normal load.
- **Security**
  - All APIs require authentication except explicitly public endpoints.
  - JWTs signed with strong secrets and reasonable expiry.
  - Enforce CORS and HTTPS (`HttpsConfig`) in production.
  - Validate all incoming DTOs (`jakarta.validation`) and sanitize user input.
- **Reliability**
  - Use DB transactions for payments, wallet updates, and critical state transitions.
  - Ensure idempotency or at‑least‑once safety for webhooks and WebSocket events where needed.
- **Observability**
  - Structured logging around:
    - Auth flows.
    - Match lifecycle changes.
    - Payments/wallet updates.
    - WebSocket connect/disconnect and errors.

### 7. Open Questions / Future Enhancements

- **Admin tooling**
  - Do we need dedicated admin UIs for moderating communities, matches, and chats?
- **Advanced matchmaking**
  - Automatic match suggestions based on skill level, history, or location.
- **Analytics**
  - Engagement dashboards (matches played, retention, payment volume).
- **Moderation**
  - Chat moderation, abuse reports, and user suspension flows.
- **More auth providers**
  - Apple ID, Facebook, email/password sign‑up.

This PRD is derived from the current codebase and migrations; it should be updated as new domains, endpoints, or flows are introduced.

