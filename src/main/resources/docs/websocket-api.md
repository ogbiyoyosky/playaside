## WebSocket / STOMP API

This document describes the real-time WebSocket messaging API used by the Playvora backend.
It complements the HTTP APIs documented in Swagger.

### 1. Connection

- **WebSocket endpoint**: `/ws`
- **Protocol**: STOMP over WebSocket (SockJS supported)
- **Application destination prefix**: `/app`
- **Topic (broadcast) prefix**: `/topic`
- **Queue (broker) prefix**: `/queue`
- **User (private) prefix**: `/user`

Authentication is handled by `WebSocketHandshakeInterceptor` and `WebSocketAuthInterceptor`
using the same JWT-based user identity as the REST API (see backend security configuration).
Clients can provide the JWT token in any of these ways:

- **Query parameter** on the WebSocket endpoint: `token` or `auth`,  
  e.g. `https://api.playvora.com/ws?token=YOUR_JWT_HERE`
- **STOMP header** `Authorization: Bearer <JWT>`
- **STOMP headers** `token` or `auth-token` containing the raw JWT

The handshake interceptor extracts the token from query parameters and stores it in the WebSocket
session; the auth interceptor then validates the token on `CONNECT` and for each `SEND` message
to ensure the Spring `SecurityContext` is populated.

The server is configured with STOMP **heartbeats every 10 seconds** in both directions.  
Clients should configure their STOMP implementation with matching heartbeats
(\(stompClient.heartbeat.outgoing = 10000; stompClient.heartbeat.incoming = 10000;\))
to avoid idle connections being closed by intermediaries.

---

### 2. Message envelope

All server-sent messages use the common wrapper `WebSocketMessage`:

- **type**: logical message type (e.g. `team_selection`, `match_update`, `chat_message`,
  `private_chat_message`, `community_chat_message`, `error`, etc.)
- **messageId**: server-generated UUID for this message
- **timestamp**: UTC `LocalDateTime` when the message was created
- **data**: type-specific payload (DTO)
- **message** _(optional)_: human-readable string

Example (simplified JSON):

```json
{
  "type": "chat_message",
  "messageId": "a0e3f0c7-...",
  "timestamp": "2025-01-01T12:00:00Z",
  "data": {
    "id": "chat-message-id",
    "matchId": "match-id",
    "senderId": "user-id",
    "senderName": "John Doe",
    "senderFirstName": "John",
    "senderLastName": "Doe",
    "message": "Kickoff in 10 minutes!",
    "createdAt": "2025-01-01T11:59:50Z"
  }
}
```

---

### 3. Match event WebSocket API

All destinations below are under the `/app` prefix when sending.

#### 3.1 Team selection & match lifecycle

**Send (client → server):**

- `SEND /app/match-events/{matchId}/select-player`
  - **Body**: `PlayerSelectionRequest`
    - `matchId: UUID`
    - `teamId: UUID`
    - `userId: UUID`

- `SEND /app/match-events/{matchId}/generate-teams`
- `SEND /app/match-events/{matchId}/start`
- `SEND /app/match-events/{matchId}/complete`

These correspond to the controller methods in `WebSocketController`.

**Subscribe (server → client):**

- `SUBSCRIBE /topic/match/{matchId}/updates`

The server sends `WebSocketMessage` envelopes with:

- `type = "team_selection"` and `data = TeamSelectionMessage`
- `type = "match_update"` and `data = MatchUpdateMessage`

#### 3.2 Match chat (group chat per event)

**Send (client → server):**

- `SEND /app/match-events/{matchId}/chat`
  - **Body**: `ChatMessageRequest`
    - `matchId: UUID`
    - `message: string`

**Subscribe (server → client):**

- `SUBSCRIBE /topic/match/{matchId}/chat`

The server sends:

- `type = "chat_message"`
- `data = ChatMessageResponse`
  - `id: UUID`
  - `matchId: UUID`
  - `senderId: UUID`
  - `senderName: string`
  - `senderFirstName: string`
  - `senderLastName: string`
  - `message: string`
  - `createdAt: LocalDateTime`

Additionally, push notifications may be sent to match participants using
`IPushNotificationService` (mobile clients), but this is separate from WebSocket.

Chat history and unread counts for match chat are accessed via the HTTP API:

- `GET /api/v1/chat/match-events/{matchId}` – paginated history for a match (newest first)
- `POST /api/v1/chat/match-events/{matchId}/mark-read` – mark all messages as read for the current user

---

### 4. Private chat (DM between two users)

#### 4.1 Send private messages

**Send:**

- `SEND /app/chat/private/{recipientId}`
  - **Body**: `PrivateChatMessageRequest`
    - `recipientId: UUID` _(optional, should match path variable – the path value is the source of truth)_
    - `message: string`

#### 4.2 Receive private messages

**Subscribe:**

- `SUBSCRIBE /user/queue/private-chat`

For any message in a conversation between two users, the server sends a `WebSocketMessage` with:

- `type = "private_chat_message"`
- `data = PrivateChatMessageResponse`
  - `id: UUID`
  - `senderId: UUID`
  - `senderName: string`
  - `recipientId: UUID`
  - `recipientName: string`
  - `message: string`
  - `createdAt: LocalDateTime`
  - `conversationId: string` (stable identifier based on both user IDs)

The same payload is delivered to **both** participants’ `/user/queue/private-chat` queues.

Chat history and unread counts for private conversations are accessed via the HTTP API:

- `GET /api/v1/chat/private/{userId}` – paginated history, ordered by `createdAt`
- `POST /api/v1/chat/private/{userId}/mark-read` – mark all messages as read for the current user

---

### 5. Community chat

#### 5.1 Send community messages

**Send:**

- `SEND /app/communities/{communityId}/chat`
  - **Body**: `CommunityChatMessageRequest`
    - `communityId: UUID`
    - `message: string`

Only active community members (per `CommunityMember`) are allowed to send.

#### 5.2 Receive community messages

**Subscribe:**

- `SUBSCRIBE /topic/communities/{communityId}/chat`

Server messages:

- `type = "community_chat_message"`
- `data = CommunityChatMessageResponse`
  - `id: UUID`
  - `communityId: UUID`
  - `senderId: UUID`
  - `senderName: string`
  - `message: string`
  - `createdAt: LocalDateTime`

Push notifications may also be sent to active members (excluding the sender) via
`IPushNotificationService`.

Chat history and unread counts for community chat are accessed via the HTTP API:

- `GET /api/v1/chat/communities/{communityId}` – paginated history for a community
- `POST /api/v1/chat/communities/{communityId}/mark-read` – mark all messages as read for the current user

---

### 6. Per-user queues and errors

In addition to private chat, the backend uses user-specific queues:

- `/user/queue/notifications`
  - Informational messages such as selection confirmations.

- `/user/queue/errors`
  - Error messages for failed WebSocket operations.
  - Envelope:
    - `type = "error"`
    - `data = string` or small error DTO

Clients should subscribe to `/user/queue/errors` after connecting to display
non-fatal WebSocket errors to the user.

---

### 7. Summary of main STOMP destinations

**Send (`/app` prefix):**

- `/app/match-events/{matchId}/select-player`
- `/app/match-events/{matchId}/generate-teams`
- `/app/match-events/{matchId}/start`
- `/app/match-events/{matchId}/complete`
- `/app/match-events/{matchId}/chat`
- `/app/chat/private/{recipientId}`
- `/app/communities/{communityId}/chat`

**Subscribe (`/topic` or `/user` prefixes):**

- `/topic/match/{matchId}/updates`
- `/topic/match/{matchId}/chat`
- `/topic/communities/{communityId}/chat`
- `/user/queue/private-chat`
- `/user/queue/notifications`
- `/user/queue/errors`

Use this document together with the HTTP Swagger docs to understand how WebSocket
messaging integrates with the rest of the API (e.g. match creation, community membership,
and REST-based chat history + read/unread endpoints).


