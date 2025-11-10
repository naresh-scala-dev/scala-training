case class Vec2D(x: Double, y: Double) {
  def +(that: Vec2D): Vec2D = Vec2D(this.x + that.x, this.y + that.y)
  def -(that: Vec2D): Vec2D = Vec2D(this.x - that.x, this.y - that.y)
  def *(scalar: Double): Vec2D = Vec2D(this.x * scalar, this.y * scalar)

  override def toString: String = s"Vec2D($x, $y)"
}

object Vec2D {

  implicit class ScalarMultiplier(val scalar: Double) extends AnyVal {
    def *(v: Vec2D): Vec2D = Vec2D(v.x * scalar, v.y * scalar)
  }

  def main(args: Array[String]): Unit = {
    val v1 = Vec2D(2, 3)
    val v2 = Vec2D(4, 1)

    println(v1 + v2)
    println(v1 - v2)
    println(v1 * 3)
    println(3 * v1)
  }
}
