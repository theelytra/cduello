# cDuello

A simple yet powerful dueling plugin for Minecraft servers running Paper 1.21.4.

## Features

- Challenge other players to duels
- Automatic duel management with timeouts
- Configurable duel settings (keep inventory, heal after duel, etc.)
- Statistics tracking (wins, losses, win ratio)
- Customizable messages
- Permission-based access control

## Installation

1. Download the latest version of the plugin from the releases section.
2. Place the JAR file in your server's `plugins` directory.
3. Restart your server.
4. Configure the plugin by editing the `config.yml` file in the `plugins/cDuello` directory.

## Commands

- `/duel <player>` - Send a duel request to a player
- `/duel accept` - Accept a pending duel request
- `/duel deny` - Deny a pending duel request
- `/duel stats [player]` - View your or another player's duel stats
- `/duel reload` - Reload the plugin configuration (requires permission: `cduello.admin`)

## Permissions

- `cduello.use` - Allows the player to use dueling functionality (default: true)
- `cduello.admin` - Allows the player to use administrative commands (default: op)

## Configuration

The plugin is highly configurable through the `config.yml` file. Here are some key configuration options:

- `duels.request-timeout` - Time in seconds before a duel request expires
- `duels.countdown` - Time in seconds for countdown before duel starts
- `duels.teleport-back` - Whether to teleport players back to their original location after a duel
- `duels.heal-after-duel` - Whether to heal players after a duel
- `duels.clear-effects` - Whether to clear effects after a duel
- `duels.keep-inventory` - Whether to keep inventory during a duel
- `duels.allowed-commands` - List of commands allowed during a duel

## Setup Instructions

### IntelliJ IDEA
1. Open IntelliJ IDEA
2. Select "Open" and navigate to the project folder
3. IntelliJ should automatically recognize this as a Maven project
4. If not, right-click on the `pom.xml` file and select "Add as Maven Project"
5. Wait for Maven to download all dependencies
6. If you still see import errors, go to File > Invalidate Caches / Restart

### Eclipse
1. Open Eclipse
2. Select File > Import > Maven > Existing Maven Projects
3. Navigate to the project folder and select it
4. Eclipse should automatically recognize the project structure
5. If you still see import errors, right-click on the project > Maven > Update Project

### VS Code
1. Open VS Code
2. Open the project folder
3. Install the "Extension Pack for Java" and "Maven for Java" extensions if not already installed
4. VS Code should automatically recognize the project as a Maven project
5. If you still see import errors, open the Command Palette (Ctrl+Shift+P), type "Java: Clean Java Language Server Workspace", and select "Restart"

### Manual Setup (if IDE integration fails)
1. Make sure you have Maven installed
2. Open a terminal in the project directory
3. Run `mvn clean install`
4. Add the Paper API jar file (located in `libs/paper-api-1.21.4-R0.1-SNAPSHOT.jar`) to your project's classpath manually

## Building

To build the plugin from source:

1. Clone the repository
2. Run `mvn clean package`
3. The compiled JAR file will be in the `target` directory

## Requirements

- Paper 1.21.4
- Java 8 or higher

## License

This project is licensed under the [MIT License](LICENSE).

## Support

If you encounter any bugs or have any suggestions, please create an issue on the [GitHub repository](https://github.com/ItsCactus/cDuello).

## Credits

- Created by ItsCactus 

## Dependencies
- Java 21
- Paper API 1.21.4-R0.1-SNAPSHOT 