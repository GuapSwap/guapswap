import com.omnivore.dedcline._
import protocol.GuapSwapUtils
import configs.GuapSwapConfig
import scala.util.{Try, Success, Failure}


object GuapSwapApp extends App {

    override def main(args: Array[String]): Unit = {
        // Setting up node configurations
        val configFilePath: String = GuapSwapUtils.GUAPSWAP_CONFIG_FILE_PATH
        
        val result = GuapSwapConfig.load(configFilePath) match {
            case Success(config) => {

                // GuapSwap program logic goes here
                println(config)

                
            }

            case Failure(exception) => exception
        }


    }

}