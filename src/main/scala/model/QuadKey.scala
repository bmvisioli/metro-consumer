package model

object QuadKey {

  val defaultLevel = 11

  def clip(number: Double, minValue: Double, maxValue: Double) = Math.min(Math.max(number, minValue), maxValue)

  // Step 1
  def coordinatesToTiles(latitude: Double, longitude: Double, level: Int) = {

    val x = (longitude + 180) / 360
    val sinLatitude = Math.sin(latitude * Math.PI / 180)
    val y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)

    val mapSize = 256L << level
    val pixelX = clip(x * mapSize + 0.5, 0, mapSize - 1)
    val pixelY = clip(y * mapSize + 0.5, 0, mapSize - 1)

    ((pixelX / 256).toInt, (pixelY / 256).toInt)
  }

  // Step 2
  def tileToQuadKey(tileX: Int, tileY: Int, level: Int) = {

    def getKey(currentLevel: Int) = {
      var digit = 0
      val mask = 1 << (currentLevel - 1)
      if ((tileX & mask) != 0) {
        digit = digit + 1
      }
      if ((tileY & mask) != 0) {
        digit = digit + 2
      }
      digit
    }

    Range.inclusive(level,1,-1).map(getKey).mkString
  }

  def coordinatesToQuadKey(latitude: Double, longitude: Double, level: Int = defaultLevel) = {
    val (tileX, tileY) = coordinatesToTiles(latitude, longitude, level)
    tileToQuadKey(tileX, tileY, level)
  }
}