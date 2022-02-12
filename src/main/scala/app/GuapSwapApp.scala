object GuapSwapApp extends App {

    override def main(args: Array[String]): Unit = 
        
        // Setting up node configurations
        val configFilePath: String = "storage/guapswap_config.json"
        val config: GuapSwapConfig = GuapSwapConfig.load(configFilePath) match {
            case Some(config) => config
            case None => throw new Exception(s"Failed to load config file: $configFilePath")
        }

}