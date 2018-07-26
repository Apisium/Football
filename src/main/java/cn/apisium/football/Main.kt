package cn.apisium.football

import me.McVier3ck.scoreboard.CustomScoreboard
import me.McVier3ck.team.Team
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarFlag
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.util.Vector
import team.unstudio.udpl.command.anno.AnnoCommandManager
import team.unstudio.udpl.i18n.I18n
import team.unstudio.udpl.i18n.YamlI18n
import team.unstudio.udpl.util.PluginUtils
import java.io.File
import java.text.SimpleDateFormat

class Main: JavaPlugin() {
  private lateinit var i18n: I18n
  private lateinit var events: Events
  private var timer: BukkitTask? = null
  lateinit var boardA: CustomScoreboard
  lateinit var boardB: CustomScoreboard
  val formatter = SimpleDateFormat("mm:ss")
  var goalA = 0
  var goalB = 0
  var bar: BossBar? = null
  var onlyKick = 0
  lateinit var teamA: Team
  lateinit var teamB: Team
  lateinit var world: World
  lateinit var centerA: Vector
  lateinit var centerB: Vector
  lateinit var teamAColor: Color
  lateinit var teamBColor: Color
  lateinit var audience: Team
  lateinit var config: Config
  lateinit var center: Location
  var lock = false
  var state = false
  var started = false
  var matching = false
  var entity: ArmorStand? = null
  val effects = arrayOf(PotionEffectType.NIGHT_VISION, PotionEffectType.SPEED, PotionEffectType.JUMP)
  override fun onEnable() {
    saveDefaultConfig()
    config = Config()
    config.load(getConfig())
    val dir = File(dataFolder, "lang")
    if (!dir.exists()) PluginUtils.saveDirectory(this, "lang", false)
    i18n = YamlI18n.fromFile(dir)
    events = Events(this)
    readConfig()

    AnnoCommandManager
      .builder()
      .name("football")
      .alias("fb")
      .plugin(this)
      .build()
      .addHandler(CommandImpl(this))
      .unsafeRegisterCommand()
    this.server.pluginManager.registerEvents(events, this)
  }

  private fun readConfig() {
    val t1 = l("teamA")
    val t2 = l("teamB")
    val t3 = l("audience")

    world = server.getWorld(config.world)
    center = Location(world, config.center.x, config.center.y + 5, config.center.z)

    val c1 = ChatColor.getByChar(config.teamAColor[0])
    val c2 = ChatColor.getByChar(config.teamBColor[0])
    val c3 = ChatColor.getByChar(config.audienceColor[0])

    teamAColor = chatColorToColor(c1)
    teamBColor = chatColorToColor(c2)

    teamA = Team(t1).apply {
      color = c1.asBungee()
      teamname = c1.toString() + ChatColor.BOLD + t1 + ChatColor.RESET
      canPlace = false
      canBreak = false
      friendlyFire = true
    }
    teamB = Team(t2).apply {
      color = c2.asBungee()
      teamname = c2.toString() + ChatColor.BOLD + t2 + ChatColor.RESET
      canPlace = false
      canBreak = false
      friendlyFire = true
    }
    audience = Team(t3).apply {
      color = c3.asBungee()
      teamname = c3.toString() + ChatColor.BOLD + t3 + ChatColor.RESET
      canPlace = false
      canBreak = false
      friendlyFire = true
    }

    val aStart = config.teamAGoalStart
    val aEnd = config.teamAGoalEnd
    centerA = aStart.getMidpoint(aEnd)

    val bStart = config.teamBGoalStart
    val bEnd = config.teamBGoalEnd
    centerB = bStart.getMidpoint(bEnd)

    events.apply {
      maxAX = Math.abs(aStart.x - aEnd.x) / 2 + 1.5
      maxAY = Math.abs(aStart.y - aEnd.y) / 2 + 1.5
      maxAZ = Math.abs(aStart.z - aEnd.z) / 2 + 1.5

      maxBX = Math.abs(bStart.x - bEnd.x) / 2 + 1.5
      maxBY = Math.abs(bStart.y - bEnd.y) / 2 + 1.5
      maxBZ = Math.abs(bStart.z - bEnd.z) / 2 + 1.5

      val pStart = config.playgroundStart
      val pEnd = config.playgroundEnd
      centerP = pStart.getMidpoint(pEnd)
      maxPX = Math.abs(pStart.x - pEnd.x) / 2 + 1.5
      maxPY = Math.abs(pStart.y - pEnd.y) / 2 + 1.5
      maxPZ = Math.abs(pStart.z - pEnd.z) / 2 + 1.5
    }
  }

  fun startMatch() {
    goalA = 0
    goalB = 0
    onlyKick = 0
    lock = false
    state = true
    started = false
    matching = true
    removeBall()
    val fullTime = formatter.format(config.time * 1000L)
    timer = server.scheduler.runTaskTimer(this, {
      teamA.players.forEach {
        it.spigot().sendMessage(
          ChatMessageType.ACTION_BAR,
          TextComponent(l("actionBar", it,"00:00", fullTime, teamA.teamname))
        )
      }
      teamB.players.forEach {
        it.spigot().sendMessage(
          ChatMessageType.ACTION_BAR,
          TextComponent(l("actionBar", it,"00:00", fullTime, teamA.teamname))
        )
      }
      audience.players.forEach {
        it.spigot().sendMessage(
          ChatMessageType.ACTION_BAR,
          TextComponent(l("actionBar", it,"00:00", fullTime, teamA.teamname))
        )
      }
    }, 0, 1)
    server.scheduler.runTaskLater(this, {
      matching = false
      val t1 = teamA.currentSize
      val t2 = teamB.currentSize
      val p1 = teamA.players
      val p2 = teamB.players
      if (t1 + t2 < config.minPlayer) {
        val msg = TextComponent("")
        p1.forEach {
          it.sendMessage(l("log.notEnough", it))
          it.inventory.clear()
          for (e in effects) it.removePotionEffect(e)
          it.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg)
          it.scoreboard = server.scoreboardManager.mainScoreboard
          it.teleport(Location(world, config.audienceRoom.x, config.audienceRoom.y, config.audienceRoom.z))
          teamA.leaveTeam(it)
        }
        p2.forEach {
          it.inventory.clear()
          for (e in effects) it.removePotionEffect(e)
          it.sendMessage(l("log.notEnough", it))
          it.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg)
          it.scoreboard = server.scoreboardManager.mainScoreboard
          it.teleport(Location(world, config.audienceRoom.x, config.audienceRoom.y, config.audienceRoom.z))
          teamB.leaveTeam(it)
        }
        audience.players.forEach {
          it.sendMessage(l("log.notEnough", it))
          it.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg)
          audience.leaveTeam(it)
        }
        timer?.cancel()
        timer = null
        bar?.removeAll()
        bar = null
        state = false
        server.scheduler.cancelTasks(this)
        return@runTaskLater
      }
      val t3 = Math.abs(t1 - t2)
      if (t3 > 1 && (t1 - t3 > 0 || t2 - t3 > 0) && (t1 - 1 < t1 || t2 - 1 < t2)) {
        if (t1 > t2) {
          val name = teamA.color.toString() + p1.slice((t1 - t3)..(t1 - 1))
            .joinToString("", transform = {
              teamA.leaveTeam(it)
              teamB.joinTeam(it)
              it.name
            })
          p1.forEach { it.sendMessage(l("log.replace", it, name, teamA.teamname)) }
          p2.forEach { it.sendMessage(l("log.replace", it, name, teamA.teamname)) }
        } else {
          val name = teamB.color.toString() + p2.slice((t2 - t3)..(t2 + 1))
            .joinToString("", transform = {
            teamB.leaveTeam(it)
            teamA.joinTeam(it)
            it.name
          })
          p1.forEach { it.sendMessage(l("log.replace", it, name, teamB.teamname)) }
          p2.forEach { it.sendMessage(l("log.replace", it, name, teamB.teamname)) }
        }
      }
      val b = server.createBossBar(
        l("boosBar", null, teamA.teamname, teamB.teamname, teamA.color.toString(), 0,
          teamB.color.toString(), 0),
        BarColor.PURPLE,
        BarStyle.SOLID,
        BarFlag.PLAY_BOSS_MUSIC
      )
      bar = b
      b.progress = 0.5
      val c = center.toVector()
      val teamAPoint = centerA.getMidpoint(c)
      val teamBPoint = centerB.getMidpoint(c)

      boardA = CustomScoreboard(teamA.teamname, DisplaySlot.SIDEBAR)
      boardB = CustomScoreboard(teamB.teamname, DisplaySlot.SIDEBAR)
      teamA.players.forEach {
        it.teleport(Location(world, teamAPoint.x + Math.random() * 5,
          centerA.y,teamAPoint.z + Math.random() * 5))
        it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        it.sendMessage(l("log.wait", it))
        boardA.setScore(teamA.color.toString() + it.name, 0)
        b.addPlayer(it)
      }
      teamB.players.forEach {
        it.teleport(Location(world, teamBPoint.x + Math.random() * 5,
          centerB.y,teamBPoint.z + Math.random() * 5))
        it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        it.sendMessage(l("log.wait", it))
        boardB.setScore(teamB.color.toString() + it.name, 0)
        b.addPlayer(it)
      }
      audience.players.forEach {
        it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        it.sendMessage(l("log.wait", it))
        b.addPlayer(it)
      }
      boardA.setScoreboard(teamA)
      boardB.setScoreboard(teamB)
      server.scheduler.runTaskLater(this, { lock = true }, 30)
      server.scheduler.runTaskLater(this, {
        val team: String
        onlyKick = if (Math.random() > 0.5) {
          team = teamA.teamname
          1
        } else {
          team = teamB.teamname
          2
        }
        teamA.players.forEach {
          it.sendMessage(l("log.start", it, team))
          title(it, l("title.start.main", it), l("title.start.sub", it, team))
        }
        teamB.players.forEach {
          it.sendMessage(l("log.start", it, team))
          title(it, l("title.start.main", it), l("title.start.sub", it, team))
        }
        audience.players.forEach {
          it.sendMessage(l("log.start", it, team))
          title(it, l("title.start.main", it), l("title.start.sub", it, team))
        }
        firework(center)
        var i = 5
        object: BukkitRunnable() {
          override fun run() {
            if (i == 0) {
              this.cancel()
              spawnBall()
              teamA.players.forEach {
                title(it, l("title.countDown.start", it), "")
                it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
              }
              teamB.players.forEach {
                title(it, l("title.countDown.start", it), "")
                it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
              }
              audience.players.forEach {
                title(it, l("title.countDown.start", it), "")
                it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
              }
              lock = false
            } else {
              teamA.players.forEach {
                title(it, l("title.countDown.main", it, i), "")
                it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
              }
              teamB.players.forEach {
                title(it, l("title.countDown.main", it, i), "")
                it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
              }
              audience.players.forEach {
                title(it, l("title.countDown.main", it, i), "")
                it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
              }
            }
            i--
          }
        }.runTaskTimer(this, 0, 20)
      }, 100)
    }, config.prepareTime * 20)
  }

  fun updateBoosBar() {
    bar?.apply {
      title = l("boosBar", null, teamA.teamname, teamB.teamname, teamA.color.toString(), goalA,
        teamB.color.toString(), goalB)
      val p = goalA + goalB
      progress = if (p == 0) 0.5 else goalA / p.toDouble()
    }
  }

  fun startGame() {
    started = true
    val time = System.currentTimeMillis()
    val endTime = time + config.time * 1000L
    timer?.cancel()
    timer = null
    object: BukkitRunnable() {
      override fun run() {
        val now = System.currentTimeMillis()
        if (endTime <= now) {
          this.cancel()
          var t1 = "win"
          var t2 = "defeat"
          if (goalA == goalB) {
            t1 = "draw"
            t2 = "draw"
          } else if (goalA < goalB) {
            t1 = "defeat"
            t2 = "win"
          }
          val msg = TextComponent("")
          teamA.players.forEach {
            it.sendMessage(l("log.competition", it, teamA.teamname, teamB.teamname,
              teamA.color.toString(), goalA, teamB.color.toString(), goalB))
            it.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg)
            title(it, l("title.end.main", it), l("title.end.sub", it, t1))
            it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            it.inventory.clear()
            it.scoreboard = server.scoreboardManager.mainScoreboard
            for (e in effects) it.removePotionEffect(e)
            it.teleport(Location(world, config.audienceRoom.x, config.audienceRoom.y, config.audienceRoom.z))
            teamA.leaveTeam(it)
          }
          teamB.players.forEach {
            it.sendMessage(l("log.competition", it, teamA.teamname, teamB.teamname,
              teamA.color.toString(), goalA, teamB.color.toString(), goalB))
            it.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg)
            title(it, l("title.end.main", it), l("title.end.sub", it, t2))
            it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            it.inventory.clear()
            it.scoreboard = server.scoreboardManager.mainScoreboard
            for (e in effects) it.removePotionEffect(e)
            it.teleport(Location(world, config.audienceRoom.x, config.audienceRoom.y, config.audienceRoom.z))
            teamB.leaveTeam(it)
          }
          audience.players.forEach {
            it.sendMessage(l("log.competition", it, teamA.teamname, teamB.teamname,
              teamA.color.toString(), goalA, teamB.color.toString(), goalB))
            it.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg)
            it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            title(it, l("title.end.main", it), "")
            audience.leaveTeam(it)
          }
          removeBall()
          bar?.removeAll()
          bar = null
          started = false
          state = false
          server.scheduler.cancelTasks(this@Main)
          return
        }
        val time1 = formatter.format(now - time)
        val time2 = formatter.format(endTime - now)
        teamA.players.forEach {
          it.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            TextComponent(l("actionBar", it, time1, time2, teamA.teamname))
          )
        }
        teamB.players.forEach {
          it.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            TextComponent(l("actionBar", it, time1, time2, teamA.teamname))
          )
        }
        audience.players.forEach {
          it.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            TextComponent(l("actionBar", it, time1, time2, teamA.teamname))
          )
        }
      }
    }.runTaskTimer(this, 0, 1)
  }

  fun removeBall() {
    entity?.remove()
    entity = null
    center.world
      .getNearbyEntities(center, 1.0, 1.0, 1.0)
      .filter { it.type == EntityType.ARMOR_STAND && it.customName == l("football") }
      .forEach { it.remove() }
  }

  fun firework(loc: Location) {
    val fw = loc.world.spawn(loc, Firework::class.java) as Firework
    val fm = fw.fireworkMeta
    fm
      .addEffect(FireworkEffect
        .builder()
        .flicker(false)
        .trail(false)
        .with(FireworkEffect.Type.BALL_LARGE)
        .withColor(Color.YELLOW)
        .withFade(Color.ORANGE)
        .build()
      )
    fm.power = 2
    fw.fireworkMeta = fm
    server.scheduler.scheduleSyncDelayedTask(this@Main, { fw.detonate() }, 5)
  }

  fun spawnBall(l: Location = center) {
    removeBall()
    (l
      .world
      .spawnEntity(l, EntityType.ARMOR_STAND) as ArmorStand)
      .apply {
        setBasePlate(false)
        isSmall = true
        isVisible = false
        isCustomNameVisible = true
        helmet = ItemStack(Material.SEA_LANTERN)
        customName = l("football")
        entity = this
      }
  }

  fun checkPlayers() {
    if (teamA.currentSize + teamB.currentSize < 1) {
      audience.players.forEach {
        it.sendMessage(l("log.notEnough", it))
        it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(""))
        audience.leaveTeam(it)
      }
      timer?.cancel()
      timer = null
      bar?.removeAll()
      bar = null
      removeBall()
      started = false
      matching = false
      state = false
      lock = false
      server.scheduler.cancelTasks(this)
    }
  }

  fun title(player: Player, main: String, sub: String) =
    player.sendTitle(main, sub, 10, 70, 20 )

  fun l(key: String, p: Player? = null, vararg args: Any?): String =
    ChatColor.translateAlternateColorCodes('&',
      if (args.isNotEmpty()) {
        if (p == null) this.i18n.format(key, *args) else this.i18n.format(p, key, *args)
      } else {
        if (p == null) this.i18n.localize(key) else this.i18n.localize(p, key)
      }
    )
}
