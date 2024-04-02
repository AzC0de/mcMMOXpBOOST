# mcMMOXpBOOST

AzXPBoost is a Spigot plugin for Minecraft servers that allows players to purchase and use experience boosts in the mcMMO plugin. The plugin integrates with Vault for economy support and provides a variety of commands for managing and using boosts.

## Features

- Purchase experience boosts with in-game currency.
- List available and active boosts.
- Claim and activate purchased boosts.
- Admin commands to give boosts, reload configurations, and view player boosts.

## Dependencies

- mcMMO
- Vault

## Commands

- `/mcm buy <type>` - Purchase a boost of the specified type.
- `/mcm list` - List all purchased boosts.
- `/mcm claim <type>` - Activate a purchased boost.
- `/mcm time` - View the remaining time of the active boost.
- `/mcba give <user> <type> <quantity>` - Give a boost to a player (Admin command).
- `/mcba reload` - Reload the configuration and message files (Admin command).
- `/mcba view <user>` - View the boosts of a player (Admin command).

## Permissions

- `mcb.admin` - Allows access to the admin commands.

## Configuration

Edit the `config.yml` file to configure the available boosts and their properties:

```yaml
boosts:
  x2:
    name: "BoostX2"
    multiplier: 2.0
    duration: 20 # Duration in seconds.
    price: 100.0 # Price in your server currency.
  x4:
    name: "BoostX4"
    multiplier: 4.0
    duration: 20 # Duration in seconds.
    price: 200.0 # Price in your server currency.
