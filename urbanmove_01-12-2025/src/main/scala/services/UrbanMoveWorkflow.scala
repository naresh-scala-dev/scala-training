package services

object UrbanMoveWorkflow {
  def main(args: Array[String]): Unit = {
    val inputCsv = "urbanmove_trips.csv"

    println("Running Pipeline 1...")
    Pipeline1_RddLoad.main(Array(inputCsv, "output/pipeline1"))

    println("Running Pipeline 2...")
    Pipeline2_FilterAuto.main(Array("output/pipeline1", "output/pipeline2"))

    println("Running Pipeline 3...")
    Pipeline3_AboveAverageDistance.main(Array("output/pipeline2", "output/pipeline3"))

    println("Running Pipeline 4...")
    Pipeline4_RandomSample.main(Array("output/pipeline3", "output/pipeline4"))

    println("Running Pipeline 5...")
    Pipeline5_VehicleCount.main(Array("output/pipeline4", "output/pipeline5"))

    println("Running Pipeline 6...")
    Pipeline6_AvgFareByVehicle.main(Array(inputCsv, "output/pipeline6"))

    println("Running Pipeline 7...")
    Pipeline7_HighFareTrips.main(Array(inputCsv, "output/pipeline7"))

    println("Running Pipeline 8...")
    Pipeline8_PaymentMethodStats.main(Array(inputCsv, "output/pipeline8"))

    println("Running Pipeline 9...")
    Pipeline9_DistanceBuckets.main(Array(inputCsv, "output/pipeline9"))

    println("Running Pipeline 10...")
    Pipeline10_TopDrivers.main(Array(inputCsv, "output/pipeline10"))

    println("All pipelines completed!")
  }
}
