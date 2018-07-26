package cn.apisium.football

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

class Events(private val p: Main): Listener {
  var maxAX = 0.0
  var maxAY = 0.0
  var maxAZ = 0.0
  var maxBX = 0.0
  var maxBY = 0.0
  var maxBZ = 0.0
  var maxPX = 0.0
  var maxPY = 0.0
  var maxPZ = 0.0
  var timeLock = 0L
  lateinit var centerP: Vector

  private var timer: BukkitTask? = null

  @EventHandler
  private fun onDrop(e: PlayerDropItemEvent) {
    val player = e.player
    if (p.state && player is Player && (p.teamA.containsPlayer(player) ||
        p.teamB.containsPlayer(player))) e.isCancelled = true
  }

  @EventHandler
  private fun onDamage(e: EntityDamageEvent) {
    val player = e.entity
    if (p.state && player is Player && (p.teamA.containsPlayer(player) ||
        p.teamB.containsPlayer(player))) e.isCancelled = true
  }

  @EventHandler
  private fun onPickupItem(e: EntityPickupItemEvent) {
    val player = e.entity
    if (p.state && player is Player && (p.teamA.containsPlayer(player) ||
        p.teamB.containsPlayer(player))) e.isCancelled = true
  }

  @EventHandler
  private fun onPlayerMove(e: PlayerMoveEvent) {
    val player = e.player
    if (p.lock && (p.teamA.containsPlayer(player) || p.teamB.containsPlayer(player))) e.isCancelled = true
  }

  @EventHandler
  private fun onFoodLevelChange(e: FoodLevelChangeEvent) {
    val player = e.entity
    if (p.state && player is Player && (p.teamA.containsPlayer(player) ||
        p.teamB.containsPlayer(player))) e.isCancelled = true
  }

  @EventHandler
  private fun onQuit(e: PlayerQuitEvent) = clear(e.player)

  @EventHandler
  private fun onDeath(e: PlayerDeathEvent) = clear(e.entity)

  private fun clear(player: Player) {
    when {
      !p.state -> return
      p.teamA.containsPlayer(player) -> {
        p.teamA.leaveTeam(player)
        player.inventory.clear()
        p.effects.forEach { player.removePotionEffect(it) }
        player.scoreboard = p.server.scoreboardManager.mainScoreboard
      }
      p.teamB.containsPlayer(player) -> {
        p.teamB.leaveTeam(player)
        player.inventory.clear()
        p.effects.forEach { player.removePotionEffect(it) }
        player.scoreboard = p.server.scoreboardManager.mainScoreboard
      }
      p.audience.containsPlayer(player) -> p.audience.leaveTeam(player)
      else -> return
    }
    p.checkPlayers()
    p.config.audienceRoom.apply { player.teleport(Location(p.world, x, y, z)) }
  }

  @EventHandler
  private fun onDamage(e: EntityDamageByEntityEvent) {
    val en = e.entity
    val player = e.damager
    if (en != p.entity || e.cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK || player !is Player) return
    val t1 = p.teamA.containsPlayer(player)
    val t2 = p.teamB.containsPlayer(player)
    if (!(t1 || t2) || (t1 && p.onlyKick == 2) || (t2 && p.onlyKick == 1)) return
    if (!p.started) p.startGame()
    p.onlyKick = 0
    val yaw = (player.location.yaw.toDouble() / 180 + 0.5) * Math.PI

    var displayName = player.inventory.itemInMainHand?.itemMeta?.displayName ?: ""
    displayName = if (displayName == "") "" else displayName.substring(displayName.length - 1)
    var power = 1.3
    var powerY = 0.6
    when (displayName) {
      "2" -> power = 1.5
      "3" -> power = 1.8
      "4" -> {
        power = 1.5
        powerY = 1.3
      }
    }
    val v = Vector(Math.cos(yaw) * power, powerY, Math.sin(yaw) * power)
    en.velocity = v

    en.world.spawnParticle(Particle.LAVA, en.location, 10)
    timer?.cancel()
    timer = object: BukkitRunnable() {
      override fun run () {
        val d = en.location
        val team: String
        val name: String
        if (Math.abs(p.centerA.x - d.x) <= maxAX && Math.abs(p.centerA.y - d.y) <= maxAY &&
          Math.abs(p.centerA.z - d.z) <= maxAZ) {
          val now = System.currentTimeMillis()
          if (now - timeLock < 3000) {
            this.cancel()
            timer = null
            return
          }
          timeLock = now
          p.goalB++
          p.onlyKick = 2
          team = p.teamB.teamname
          name = (if (t1) p.teamA.color else p.teamB.color).toString() + player.name
          if (t2) p.boardB.setScore(name, p.boardB.getScore(name) + 1)
        } else if (Math.abs(p.centerB.x - d.x) <= maxBX && Math.abs(p.centerB.y - d.y) <= maxBY &&
          Math.abs(p.centerB.z - d.z) <= maxBZ) {
          val now = System.currentTimeMillis()
          if (now - timeLock < 3000) {
            this.cancel()
            timer = null
            return
          }
          timeLock = now
          p.goalA++
          p.onlyKick = 1
          team = p.teamA.teamname
          name = (if (t1) p.teamA.color else p.teamB.color).toString() + player.name
          if (t1) p.boardA.setScore(name, p.boardA.getScore(name) + 1)
        } else if (Math.abs(centerP.x - d.x) > maxPX || Math.abs(centerP.y - d.y) > maxPY ||
          Math.abs(centerP.z - d.z) > maxPZ) {
          val now = System.currentTimeMillis()
          if (now - timeLock < 3000) {
            this.cancel()
            timer = null
            return
          }
          timeLock = now
          en.world.spawnParticle(Particle.EXPLOSION_HUGE, d, 1)
          p.removeBall()
          val point: Location
          if (t1) {
            p.onlyKick = 2
            team = p.teamB.teamname
            name = p.teamA.color.toString() + player.name
            point = getPoint(d, p.config.angleA1, p.config.angleA2)
          } else {
            p.onlyKick = 1
            team = p.teamA.teamname
            name = p.teamB.color.toString() + player.name
            point = getPoint(d, p.config.angleB1, p.config.angleB2)
          }

          p.teamA.players.forEach {
            p.title(it, p.l("title.out.main", it), p.l("title.out.sub", it, name))
            it.sendMessage(p.l("log.out", it, team))
            it.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
          }
          p.teamB.players.forEach {
            p.title(it, p.l("title.out.main", it), p.l("title.out.sub", it, name))
            it.sendMessage(p.l("log.out", it, team))
            it.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
          }
          p.audience.players.forEach {
            p.title(it, p.l("title.out.main", it), p.l("title.out.sub", it, name))
            it.sendMessage(p.l("log.out", it, team))
            it.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
          }

          p.server.scheduler.runTaskLater(p, {
            p.spawnBall(point)
            p.firework(point)
          }, 100)
          this.cancel()
          timer = null
          return
        } else {
          (en as ArmorStand).headPose = en.headPose.add(30.0, 30.0, 30.0)
          if (en.isOnGround) {
            p.server.scheduler.runTaskLater(p, {
              this.cancel()
              timer = null
            }, 40)
          }
          return
        }
        p.updateBoosBar()
        en.world.spawnParticle(Particle.EXPLOSION_HUGE, d, 1)

        p.teamA.players.forEach {
          p.title(it, p.l("title.goal.main", it, team), p.l("title.goal.sub", it, name))
          it.sendMessage(p.l("log.goal", it, team))
          it.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        }
        p.teamB.players.forEach {
          p.title(it, p.l("title.goal.main", it, team), p.l("title.goal.sub", it, name))
          it.sendMessage(p.l("log.goal", it, team))
          it.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        }
        p.audience.players.forEach {
          p.title(it, p.l("title.goal.main", it, team), p.l("title.goal.sub", it, name))
          it.sendMessage(p.l("log.goal", it, team))
          it.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        }
        p.firework(player.location)
        p.removeBall()
        p.server.scheduler.runTaskLater(p, {
          p.spawnBall()
          p.firework(p.center)
        }, 100)
        this.cancel()
        timer = null
      }
    }.runTaskTimer(p, 0, 1)
  }

  private fun getPoint(l: Location, p1: Vector, p2: Vector): Location {
    val v = l.toVector()
    return if (p1.distance(v) > p2.distance(v)) Location(l.world, p2.x, p2.y, p2.z)
      else Location(l.world, p1.x, p1.y, p1.z)
  }
}
