package cn.apisium.football

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.potion.PotionEffect
import team.unstudio.udpl.command.anno.Command
import team.unstudio.udpl.command.anno.Required

class CommandImpl(private val p: Main) {
  private val effects = p.effects.map { PotionEffect(it, 999999999, 2) }

  @Command("help")
  fun onHelp(sender: CommandSender) {
    sender.sendMessage(arrayOf(
      "",
      "${ChatColor.GREEN}======================",
      "${ChatColor.YELLOW}Author: ${ChatColor.AQUA}Shirasawa",
      "${ChatColor.YELLOW}Repository: ${ChatColor.AQUA}https://github.com/Apisium/Football",
      "${ChatColor.GREEN}======================",
      ""
    ))
  }
  @Command("start", senders = [Player::class], permission = "football.start")
  fun onStart(sender: Player, @Required(name = "Team") team: Int) = when {
    p.teamA.containsPlayer(sender) -> sender.sendMessage(p.l("log.inPreparation", sender, p.teamA.teamname))
    p.teamB.containsPlayer(sender) -> sender.sendMessage(p.l("log.inPreparation", sender, p.teamB.teamname))
    p.audience.containsPlayer(sender) ->
      sender.sendMessage(p.l("log.inPreparation", sender, p.audience.teamname))
    team != 1 && team != 2 -> {
      p.audience.joinTeam(sender)
      sender.sendMessage(p.l("log.inPreparation", sender, p.audience.teamname))
      p.config.audienceRoom.apply { sender.teleport(Location(p.world, x, y, z)) }
      sender.playSound(sender.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
      if (!p.matching) p.startMatch()
      null
    }
    p.started -> sender.sendMessage(p.l("log.started", sender))
    p.teamA.currentSize + p.teamB.currentSize >= p.config.maxPlayer ->
      sender.sendMessage(p.l("log.full", sender))
    sender.inventory.any { it != null } -> sender.sendMessage(p.l("log.hasItems", sender))
    else -> {
      sender.activePotionEffects.forEach { sender.removePotionEffect(it.type) }
      if (team == 1) {
        val name = p.teamA.teamname
        p.teamA.joinTeam(sender)
        giveItems(sender, p.teamAColor, name)
        sender.sendMessage(p.l("log.inPreparation", sender, name))
        sender.addPotionEffects(effects)
        p.config.teamARoom.apply { sender.teleport(Location(p.world, x, y, z)) }
      } else {
        val name = p.teamB.teamname
        p.teamB.joinTeam(sender)
        giveItems(sender, p.teamBColor, name)
        sender.addPotionEffects(effects)
        sender.sendMessage(p.l("log.inPreparation", sender, name))
        p.config.teamBRoom.apply { sender.teleport(Location(p.world, x, y, z)) }
      }
      sender.playSound(sender.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
      sender.health = sender.getAttribute(Attribute.GENERIC_MAX_HEALTH).value
      sender.foodLevel = 20
      if (!p.matching) p.startMatch()
      null
    }
  }

  @Command("quit", senders = [Player::class], permission = "football.quit")
  fun onQuit(sender: Player) {
    when {
      !p.state -> return
      p.teamA.containsPlayer(sender) -> {
        p.teamA.leaveTeam(sender)
        sender.inventory.clear()
        p.effects.forEach { sender.removePotionEffect(it) }
        sender.scoreboard = p.server.scoreboardManager.mainScoreboard
      }
      p.teamB.containsPlayer(sender) -> {
        p.teamB.leaveTeam(sender)
        sender.inventory.clear()
        p.effects.forEach { sender.removePotionEffect(it) }
        sender.scoreboard = p.server.scoreboardManager.mainScoreboard
      }
      p.audience.containsPlayer(sender) -> p.audience.leaveTeam(sender)
      else -> return
    }
    p.checkPlayers()
    sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(""))
    p.bar?.removePlayer(sender)
    p.config.audienceRoom.apply { sender.teleport(Location(p.world, x, y, z)) }
    sender.sendMessage(p.l("log.left", sender))
    sender.playSound(sender.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
  }

  private fun giveItems(player: Player, color: Color, team: String) {
    val inv = player.inventory
    for (i in 0..3) {
      inv.setItem(i, ItemStack(Material.STICK).apply {
        itemMeta = itemMeta.apply {
          isUnbreakable = true
          displayName = p.l("item.stick$i.name", player)
          lore = p.l("item.stick$i.lore", player).split("\n")
          addEnchant(Enchantment.LUCK, 10, true)
          addEnchant(Enchantment.LURE, 10, true)
          addEnchant(Enchantment.VANISHING_CURSE, 10, true)
        }
      })
    }
    inv.helmet = addMetadata(ItemStack(Material.LEATHER_HELMET), color, team)
    inv.chestplate = addMetadata(ItemStack(Material.LEATHER_CHESTPLATE), color, team)
    inv.leggings = addMetadata(ItemStack(Material.LEATHER_LEGGINGS), color, team)
    inv.boots = addMetadata(ItemStack(Material.LEATHER_BOOTS), color, team)
  }

  private fun <T: ItemStack>addMetadata(item: T, c: Color, team: String): T = item.apply {
    itemMeta = (itemMeta as LeatherArmorMeta).apply {
      displayName = team
      color = c
      addEnchant(Enchantment.VANISHING_CURSE, 10, true)
    }
  }
}
