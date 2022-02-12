package configs

import configs.node.GuapSwapNodeConfig
import configs.parameters.GuapSwapParameters
import scala.util.{Try, Success, Failure}
import com.google.gson.{Gson, GsonBuilder}
import java.io.File
import java.io.FileReader

/**
 * Class representing the configuation settings from guapswap_config.json
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
     * Loads the GuapSwapConfig from the guapswap_config.json file
     * 
     * @param configFileName
     * @return Try[GuapSwapConfig]
     */
    def load(configFilePath: String): Try[GuapSwapConfig] = Try {
        val configFile: File = new File(configFilePath);
        val configReader: FileReader = new FileReader(configFile);
        val gson: Gson = new GsonBuilder().create();
        val config: GuapSwapConfig = gson.fromJson(configReader, classOf[GuapSwapConfig]);
        config
    }

}