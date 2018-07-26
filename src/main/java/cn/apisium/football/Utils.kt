package cn.apisium.football

import org.bukkit.Color
import org.bukkit.ChatColor

fun chatColorToColor(color: ChatColor): Color = when (color) {
  ChatColor.AQUA -> Color.AQUA
  ChatColor.BLACK -> Color.BLACK
  ChatColor.BLUE -> Color.BLUE
  ChatColor.DARK_AQUA -> Color.BLUE
  ChatColor.DARK_BLUE -> Color.BLUE
  ChatColor.DARK_GRAY -> Color.GRAY
  ChatColor.DARK_GREEN -> Color.GREEN
  ChatColor.DARK_PURPLE -> Color.PURPLE
  ChatColor.DARK_RED -> Color.RED
  ChatColor.GOLD -> Color.YELLOW
  ChatColor.GRAY -> Color.GRAY
  ChatColor.GREEN -> Color.GREEN
  ChatColor.LIGHT_PURPLE -> Color.PURPLE
  ChatColor.RED -> Color.RED
  ChatColor.WHITE -> Color.WHITE
  ChatColor.YELLOW -> Color.YELLOW
  else -> Color.RED
}
