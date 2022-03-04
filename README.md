# guapswap
GuapSwap CLI for the everyday Ergo miner.

## KYA

1. Use at your own risk.
2. Your funds could be lost and stuck in smart-contract limbo until the heat-death of the Universe, please only spend what you are willing to lose.
3. We are not responsible for anything **bad** that happens to you while using this program, please go complain to your mommy instead.

## Usage

### Usage Steps

1. Download the latest release to minimize your own risk, or clone/download repository if you are adventurous. 
2. Install Java (JRE, JDK, or OpenJDK).
3. Modify the settings in the config file, and insert one of the available token tickers.
5. Run `java -jar guapswap-1.0.0-beta.jar --help` to get command usage directions. Use the `--help` flag after any command to get usage directions.
6. If you would like to compile the jar yourself, download sbt and run `sbt assembly` within the repository/source folder.

### Usage WARNING

1. Please have at least 0.1 ERG at the proxy address before deciding to initiate a onetime swap, otherwise you will need to fund it with more ERG. This is to ensure that there are enough funds to pay for the protocol fee and all of the ErgoDex fees.

### Available Commands
1. `generate`
2. `swap [--onetime] <proxy_address>`
3. `refund <proxy_address>`
4. `list <proxy_address>`

### Available Swap Token Tickers
1. "SigUSD"
2. "SigRSV"
3. "NETA"
4. "ergopad"
5. "Erdoge"
6. "LunaDog"

## Pool Compatibility

### Known compatible pools

- getblok.io
- nanopool.org
- herominers.com
- 666pool.com
- woolypooly.com
- k1pool.com
- rkdn.app
- solopool.org

### Known incompatible pools (do not accept proxy address)

- flypool.org
- 2miners.com
- f2pool.com
- leafpool.com
- enigmapool.com
- cruxpool.com
- fairhash.org