package config

import com.typesafe.config.{Config, ConfigFactory}

object AppConfig {

  private val root: Config = ConfigFactory.load()

  object paths {
    private val p = root.getConfig("paths")

    val transactions: String = p.getString("transactions")
    val exchangeRates: String = p.getString("exchangeRates")
    val output: String = p.getString("output")

    val sales: String = p.getString("sales")
    val customerOutput: String = p.getString("customerOutput")
    val productOutput: String = p.getString("productOutput")
    val customerOutputCached: String = p.getString("customerOutputCached")
    val productOutputCached: String = p.getString("productOutputCached")


    // Transaction accumulator project
    val accum_transactions: String = p.getString("accum_transactions")
    val accum_output: String = p.getString("accum_output")


    val logCoalesce_input: String = p.getString("logCoalesce_input")
    val logCoalesce_outputBefore: String = p.getString("logCoalesce_outputBefore")
    val logCoalesce_outputAfter: String = p.getString("logCoalesce_outputAfter")
  }
}
