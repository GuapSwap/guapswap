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

        // Load the file
        val configFile: File = new File(configFilePath);

        // Read the file
        val configReader: FileReader = new FileReader(configFile);

        // Create Gson object to parse json
        val gson: Gson = new GsonBuilder().create();

        // Parse the json and create the GuapSwapConfig object
        val config: GuapSwapConfig = gson.fromJson(configReader, classOf[GuapSwapConfig]);
        config
    }

}