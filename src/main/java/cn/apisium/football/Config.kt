package cn.apisium.football

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.util.Vector

class Config {
  @JvmField
  var time = 900
  @JvmField
  var minPlayer = 2
  @JvmField
  var maxPlayer = 2
  @JvmField
  var prepareTime = 120L
  @JvmField
  var world = "world"
  @JvmField
  var teamAColor = "b"
  @JvmField
  var teamBColor = "c"
  @JvmField
  var audienceColor = "a"
  @JvmField
  var teamARoom = Vector(0, 0, 0)
  @JvmField
  var teamBRoom = Vector(0, 0, 0)
  @JvmField
  var audienceRoom = Vector(0, 0, 0)
  @JvmField
  var center = Vector(0, 0, 0)
  @JvmField
  var playgroundStart = Vector(0, 0, 0)
  @JvmField
  var playgroundEnd = Vector(0, 0, 0)
  @JvmField
  var teamAGoalStart = Vector(0, 0, 0)
  @JvmField
  var teamAGoalEnd = Vector(0, 0, 0)
  @JvmField
  var teamBGoalStart = Vector(0, 0, 0)
  @JvmField
  var teamBGoalEnd = Vector(0, 0, 0)
  @JvmField
  var angleA1 = Vector(0, 0, 0)
  @JvmField
  var angleA2 = Vector(0, 0, 0)
  @JvmField
  var angleB1 = Vector(0, 0, 0)
  @JvmField
  var angleB2 = Vector(0, 0, 0)

  fun load(config: FileConfiguration) {
    Config::class.java.fields.forEach {
      val value = config.get(it.name)
      if (value != null) it.set(this, value)
    }
  }
}