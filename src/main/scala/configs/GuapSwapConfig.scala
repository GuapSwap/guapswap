package configs

import configs.node.GuapSwapNodeConfig
import configs.parameters.GuapSwapParameters
import scala.util.{Try, Success, Failure}
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
//import java.nio.file.Paths

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
        File configFile = new File(configFilePath);
        FileReader configReader = new FileReader(configFile);
        Gson gson = new GsonBuilder().create();
        val config: GuapSwapConfig = gson.fromJson(configReader, classOf[GuapSwapConfig]);
        println(config)
        config
    }

}