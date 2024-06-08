# VelocityUtils

A Velocity utility plugin that does various things:
* The `/apply` command for Student applications.
* The `/players` command that lists the players on the network by rank.
* The `/list` command that lists the players on the network by server.
* A server alias command to shortcut `/server name` as `/name`.
* A "message of the day" (MOTD).
* A maintenance mode to be able to limit who can join.
* The ability to remember where a player last was and save that location.
* The ability to send players to different servers by command.
* Maintains a status message for Discord, displaying servers and players.

## Commands
| Command                   | Permission                      | Description                           |
|---------------------------|---------------------------------|---------------------------------------|
| `/apply`                  | `velocityutils.apply`           | Begin the student application process |
| `/players\|/online`       | `velocityutils.players`         | List players by rank                  |
| `/list\|/ls\|/glist`      | `velocityutils.list`            | List players by server                |
| `/<server>`               | `velocityutils.server.<server>` | Change servers                        |
| `/motd`                   | `velocityutils.motd`            | Show MOTD                             |
| `/send <player> <server>` | `velocityutils.send`            | Send a player to a specific server    |
| `/sendall <server>`       | `velocityutils.sendall`         | Send all players to a specific server |
| `/velocityutils version`  | `velocityutils.manage`          | Show the VelocityUtils version        |
| `/velocityutils reload`   | `velocityutils.manage`          | Reload VelocityUtils config           |
