object ShapeAnalyzer {

  sealed trait Shape
  case class Circle(radius: Double) extends Shape
  case class Rectangle(width: Double, height: Double) extends Shape

  def area(shape: Shape): Double = shape match {
    case Circle(r)       => math.Pi * r * r
    case Rectangle(w, h) => w * h
  }

  def main(args: Array[String]): Unit = {
    val circleShape = Circle(3)
    val rectangleShape = Rectangle(4, 5)

    println(area(circleShape))
    println(area(rectangleShape))
  }
}
