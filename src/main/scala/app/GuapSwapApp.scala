import configs.GuapSwapConfig
import scala.util.{Try, Success, Failure}

object GuapSwapApp extends App{

    override def main(args: Array[String]): Unit = {
        // Setting up node configurations
        val configFilePath: String = "storage/guapswap_config.json"
        
        val result = GuapSwapConfig.load(configFilePath) match {
            case Success(config) => {

                // AppKit logic goes here
                println(config)
            }

            case Failure(exception) => exception
        }


    }

}