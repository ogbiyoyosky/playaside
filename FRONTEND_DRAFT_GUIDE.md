# Frontend Draft Selection Guide

## Overview

This guide explains how to use the real-time player draft selection interface. The frontend uses WebSocket (STOMP over SockJS) to receive real-time updates when players are selected during the manual draft process.

## Features

- **Real-time Updates**: See player selections instantly as they happen
- **Available Players List**: View all available players that can be selected
- **Team Visualization**: See all teams and their selected players
- **Turn Indicators**: Visual indicators show which team's turn it is to pick
- **Captain Controls**: Only team captains can select players when it's their turn
- **Auto-removal**: Selected players are automatically removed from the available list

## Setup

### 1. Access the Frontend

Navigate to: `http://localhost:8080/draft-selection.html`

### 2. Connection Configuration

When you first load the page, you'll see a connection setup form. Enter:

- **WebSocket URL**: Default is `http://localhost:8080/ws`
- **Match ID**: The ID of the match you want to join
- **Your User ID**: Your user ID in the system
- **Your Team ID**: (Optional) Your team ID if you're a captain

### 3. Connect

Click the "Connect" button to establish the WebSocket connection.

## How It Works

### WebSocket Connection

The frontend connects to the WebSocket server using STOMP over SockJS:

```javascript
// Connection endpoint: /ws
// Subscribe to: /topic/match/{matchId}/updates
// Send messages to: /app/match-events/{matchId}/select-player
```

#### Heartbeats & Logging

- **Heartbeat Interval**: The client is configured to send and expect STOMP heartbeats every 10 seconds (`stompClient.heartbeat.outgoing = 10000`, `stompClient.heartbeat.incoming = 10000`), matching the backend broker configuration.
- **When Heartbeats Run**: Heartbeats are only exchanged while there is an active WebSocket/STOMP connection and at least one subscription; they are used by the client and broker to detect dead connections.
- **Where You See Them**:
  - **Browser**: Open the DevTools console; heartbeat frames and other STOMP frames appear as `"STOMP Debug: ..."` messages when the connection is established.
  - **Server**: Heartbeat frames are not logged by default; enable DEBUG/TRACE logging for Spring WebSocket/STOMP packages if you need to see server-side heartbeat activity.

### Real-time Updates

When a player is selected:

1. **Backend Updates Database**: Player is added to team, availability status changes to `SELECTED`
2. **Backend Broadcasts Update**: Full match data is sent to all subscribers via WebSocket
3. **Frontend Receives Update**: The UI automatically updates:
   - Selected player is removed from available players list
   - Selected player appears in the appropriate team
   - Current picking team indicator updates
   - Draft state updates (if draft completes)

### Message Format

#### Incoming Messages (from server):

```json
{
  "type": "team_selection",
  "messageId": "uuid",
  "timestamp": "2024-01-01T12:00:00",
  "data": {
    "action": "SELECT_PLAYER",
    "matchId": 1,
    "teamId": 1,
    "userId": 5,
    "userName": "John Doe",
    "teamName": "Team A",
    "message": "John Doe has been selected for the team",
    "data": {
      "id": 1,
      "title": "Match Title",
      "teams": [...],
      "availablePlayersList": [...],
      "currentPickingTeamId": 2,
      "draftInProgress": true,
      ...
    }
  }
}
```

#### Outgoing Messages (to server):

```json
{
  "matchId": 1,
  "teamId": 1,
  "userId": 5
}
```

## UI Components

### Available Players Section

- **Left Sidebar**: Shows all available players
- **Player Cards**: Each card shows:
  - Player avatar (or initials)
  - Player name
  - Select button (enabled only for current picking captain)
- **Auto-update**: Players are removed from list when selected

### Teams Section

- **Team Cards**: Each team is displayed as a card
- **Current Turn Indicator**: Team with current turn has:
  - Highlighted border
  - Pulsing animation
  - "Current Turn" indicator
- **Team Players**: Shows all selected players with:
  - Player avatar
  - Player name
  - Captain badge (if applicable)
- **Team Progress**: Shows player count (e.g., "3 / 5 players")

### Status Bar

- **Connection Status**: Shows if connected to WebSocket
- **Draft Status**: Shows if draft is in progress
- **Current Picker**: Shows which team/captain is picking

## User Roles

### Captain

- Can select players when it's their team's turn
- Select button is enabled only when:
  - Draft is in progress
  - It's their team's turn
  - They are the team captain

### Regular Player/Observer

- Can view the draft in real-time
- Cannot select players
- See all updates as they happen

## Player Selection Flow

1. **Wait for Turn**: Captain waits for their team's turn (indicated by visual highlight)
2. **View Available Players**: Available players are shown in the left sidebar
3. **Select Player**: Click "Select" button next to desired player
4. **Real-time Update**: 
   - Player is added to team
   - Player is removed from available list
   - Next team's turn is indicated
   - All connected users see the update

## Draft Completion

When all teams are full:

1. **Draft Ends**: `draftInProgress` becomes `false`
2. **Status Updates**: Draft status badge disappears
3. **Reserve Team**: Remaining players are assigned to reserve team
4. **Match Ready**: Match status changes to `TEAMS_SELECTED`
5. **Match Can Start**: Match can now be started

## Error Handling

### Connection Errors

- **Connection Failed**: Red notification appears
- **Reconnection**: User can reconnect by refreshing the page

### Selection Errors

- **Not Your Turn**: Error notification appears
- **Not Captain**: Error notification appears
- **Player Already Selected**: Error notification appears
- **Team Full**: Error notification appears

## WebSocket Endpoints

### Subscribe To:

- `/topic/match/{matchId}/updates` - Match updates (all users)
- `/user/{userId}/queue/notifications` - User-specific notifications
- `/user/{userId}/queue/errors` - Error messages

### Send To:

- `/app/match-events/{matchId}/select-player` - Select a player
- `/app/match-events/{matchId}/generate-teams` - Generate teams
- `/app/match-events/{matchId}/start` - Start match
- `/app/match-events/{matchId}/complete` - Complete match

## API Integration

The frontend can also use REST API endpoints:

- `GET /api/v1/match-events/{id}` - Get match details (includes available players list)
- `POST /api/v1/match-events/{matchId}/teams/{teamId}/select-player/{userId}` - Select player
- `POST /api/v1/match-events/match/{matchId}/generate-teams` - Generate teams

## Customization

### Styling

The frontend uses inline CSS for easy customization. Key classes:

- `.player-card` - Available player cards
- `.team-card` - Team cards
- `.team-card.current-turn` - Current picking team
- `.status-badge` - Status indicators

### Configuration

Modify the connection setup form to:
- Change default WebSocket URL
- Add authentication tokens
- Add additional configuration options

## Troubleshooting

### Connection Issues

1. **Check WebSocket URL**: Ensure it matches your server configuration
2. **Check CORS**: Ensure server allows your origin
3. **Check Authentication**: Ensure WebSocket authentication is configured

### Missing Updates

1. **Check Subscription**: Ensure subscribed to correct topic
2. **Check Match ID**: Ensure using correct match ID
3. **Check Console**: Check browser console for errors

### Player Not Removed

1. **Check Message Format**: Ensure backend sends correct format
2. **Check availablePlayersList**: Ensure field is populated in response
3. **Check UI Update Logic**: Ensure frontend handles update correctly

## Example Usage

### Basic Connection

```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Subscribe to match updates
    stompClient.subscribe(`/topic/match/${matchId}/updates`, function(message) {
        const data = JSON.parse(message.body);
        handleMatchUpdate(data);
    });
});
```

### Select Player

```javascript
// Send selection message
const destination = `/app/match-events/${matchId}/select-player`;
const message = {
    matchId: matchId,
    teamId: teamId,
    userId: playerUserId
};

stompClient.send(destination, {}, JSON.stringify(message));
```

### Handle Updates

```javascript
function handleMatchUpdate(wsMessage) {
    if (wsMessage.type === 'team_selection') {
        const matchData = wsMessage.data.data;
        
        // Update available players
        updateAvailablePlayers(matchData.availablePlayersList);
        
        // Update teams
        updateTeams(matchData.teams, matchData.currentPickingTeamId);
    }
}
```

## Security Considerations

1. **Authentication**: Implement WebSocket authentication
2. **Authorization**: Verify user permissions on server
3. **Validation**: Validate all inputs on server
4. **CORS**: Configure CORS properly
5. **Rate Limiting**: Implement rate limiting for WebSocket messages

## Future Enhancements

- [ ] Add chat functionality during draft
- [ ] Add draft timer with countdown
- [ ] Add player statistics display
- [ ] Add draft history/replay
- [ ] Add mobile responsive design
- [ ] Add keyboard shortcuts
- [ ] Add sound notifications
- [ ] Add draft analytics


