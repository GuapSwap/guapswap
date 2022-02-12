package configs

import configs.node.GuapSwapNodeConfig
import configs.parameters.GuapSwapParameters
import scala.util.{Try, Success, Failure}
import com.google.gson.{Gson, GsonBuilder}
import java.io.File
import java.io.FileReader

/**
 * Class representing the configuation settings.
 * 
 * @param node
 * @param parameters
 */
case class GuapSwapConfig(
    val node: GuapSwapNodeConfig,
    val parameters: GuapSwapParameters
  )

object GuapSwapConfig {
    
    /**
     * Loads the GuapSwapConfig from the configuration file
     * 
     * @param configFileName
     * @return Try[GuapSwapConfig]
     */
    def load(configFilePath: String): Try[GuapSwapConfig] = Try {
        val configFile: File = new File(configFilePath); // Load the file
        val configReader: FileReader = new FileReader(configFile); // Read the file
        val gson: Gson = new GsonBuilder().create(); // Create the Gson object to parse json
        val config: GuapSwapConfig = gson.fromJson(configReader, classOf[GuapSwapConfig]); // Parse the json and create the GuapSwapConfig object
        config
    }

}