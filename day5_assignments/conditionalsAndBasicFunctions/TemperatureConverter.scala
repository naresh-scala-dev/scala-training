class TemperatureConverter {

//convert method
  def convertTemp(value: Double, scale: String): Double = {
    if (scale == "C") {    //C to F
      value * 9 / 5 + 32
    } else if (scale == "F") {  //F to C
      (value - 32) * 5 / 9
    } else {    //none
      value
    }
  }
}
 object TemperatureApp {
    def main(args: Array[String]): Unit = {

    val obj=new TemperatureConverter()

    println(obj.convertTemp(0, "C"))     // 32.0
    println(obj.convertTemp(23, "F"))   // 100.0
    println(obj.convertTemp(50, "X"))    // 50.0
    println(obj.convertTemp(101, "j")) //101
    println(f"${obj.convertTemp(103, "F")}%.2f") //for rounded values.
    }
  }

