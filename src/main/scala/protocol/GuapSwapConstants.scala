package protocol

import org.ergoplatform.ErgoAddress
import org.ergoplatform.appkit.Address

object GuapSwapConstants {

    object PossibleDexAssets {

        // MAIN Assets
        final val ERG: DexAsset = new DexAsset("ERG", "0", 9)
        final val SIG_USD: DexAsset = new DexAsset("SigUSD", "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", 2)
        
        // MEME COINS
        final val ERDOGE: DexAsset = new DexAsset("Erdoge", "36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", 0)

        // TEST Assets
        final val WT_ERG: DexAsset = new DexAsset("WT_ERG", "ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b", 9)
        
    }

    // Will make this the actual service fee contract P2S address in the future, for now just my TESTNET test_wallet P2PK address
    final val serviceFeeContract: String = "9ej8AEGCpNxPaqfgisJTU2RmYG91bWfK1hu2xT34i5Xdw4czidX"
}
