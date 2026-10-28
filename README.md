# Miras Space - Networked 2D Space Game

A networked 2D Java game where a spaceship moves in space using keyboard controls. The game follows a client-server architecture where the server handles all game logic and the client handles serverPlayer input and rendering.

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/miranoserqu/miras_space/
│   │   │       ├── Main.java                  # Game entry point
│   │   │       ├── game/
│   │   │       │   └── Player.java            # Player spaceship data and logic
│   │   │       ├── network/
│   │   │       │   └── Payload.java           # Data packets for client-server communication
│   │   │       ├── resource/
│   │   │       │   └── Resource.java          # Resource management (images, sounds)
│   │   │       ├── server/
│   │   │       │   └── GameServer.java        # Server-side game logic
│   │   │       └── client/
│   │   │           └── GameClient.java        # Client-side input handling and rendering
│   │   └── resources/                         # Game resources (images, sounds)
│   │       ├── images/
│   │   │   └── (add your spaceship.png here)
│   │   └── sounds/
│   │       └── (add your sound effects here)
│   └── test/
│       └── java/
│           └── miras/space/AppTest.java
```

## How It Works

### Architecture
- **Server (GameServer)**: Handles all game logic, updates serverPlayer positions at 60 ticks per second
- **Client (GameClient)**: Captures keyboard input, sends payloads to server, renders game state
- **Communication**: Uses Payload objects to exchange information between client and server

### Game Loop
1. **Client** captures keyboard state and creates input payloads
2. **Client** sends input payloads to **Server**
3. **Server** processes input and updates serverPlayer positions (60 times per second)
4. **Server** sends update payloads back to **Client**
5. **Client** applies updates and renders the game

### Payload System
Two types of payloads are used:
- **PLAYER_INPUT**: Sent from client to server containing keyboard input
- **PLAYER_UPDATE**: Sent from server to client containing serverPlayer position updates

## Using the Game

### Adding Resources
1. Place your image files in `app/src/main/resources/images/`
   - Example: `spaceship.png` for the serverPlayer's spaceship
   - Recommended size: 50x50 pixels

2. Place your sound files in `app/src/main/resources/sounds/`
   - Example: `laser.wav` for shooting sound
   - Supported formats: WAV, AIFF, AU (via Java Sound API)

### Game Controls
- **Arrow Keys** or **WASD**: Move the spaceship
  - ← Left or A: Move left
  - → Right or D: Move right
  - ↑ Up or W: Move up
  - ↓ Down or S: Move down

The spaceship is constrained to stay within the game window boundaries.

## Key Features

### Resource Management
- Caching system for efficient resource reuse
- Automatic fallback to placeholder images
- Proper resource cleanup

### Networked Game Logic
- Fixed timestep game loop (60 ticks/second)
- Client-server separation of concerns
- Input prediction and state synchronization
- Payload-based communication

### Extensibility
- Designed for easy multiplayer extension
- Modular architecture separates concerns
- Clear interfaces between components

## Running the Game

1. Compile the project:
   ```
   ./gradlew build
   ```

2. Run the game:
   ```
   ./gradlew run
   ```
   Or execute the Main class directly from your IDE.

## Implementation Details

### Tick System
- Game runs at exactly 60 ticks per second
- Each tick processes input, updates logic, and prepares rendering
- Fixed timestep ensures consistent gameplay across different hardware

### Player Class
- Encapsulates all spaceship data (position, velocity, boundaries)
- Handles movement and collision with window boundaries
- Separates game state from rendering concerns

### Client-Server Communication
- Client captures input and sends to server
- Server authoritative for all game logic
- Client renders based on server-authoritative state
- Prevents cheating and ensures consistent game state

## Dependencies
- Java JDK 8 or higher
- No external libraries required (uses standard Java APIs)

## Testing
Run unit tests with:
```
./gradlew test
```