# guapswap
GuapSwap CLI for the Ergo miner.

## KYA

1. Use at your own risk.
2. Your funds could be lost and stuck in smart-contract limbo until the heat-death of the Universe, please only spend what you are willing to lose.
3. We are not responsible for anything **bad** that happens to you while using this program, please go complain to your mommy instead.

## Usage

### Usage Steps

#### Installing

1. Download the latest release to minimize your own risk, or clone/download repository if you are adventurous. 
2. Install Java (JRE, JDK, or OpenJDK).
3. If you would like to compile the jar yourself, download sbt and run `sbt assembly` within the repository/source folder.

#### Configurations

1. Modify the settings in the `guapswap_config.json` file
2. Change the `apiUrl` to `http://127.0.0.1:9053/` if you are running your own node. This will run GuapSwap CLI in ronin mode.
3. Add your PK to the `userAddress` field.
4. Increase the `swapIntervalInHours` field if you want to increase the time between swaps when running GuapSwap CLI in automatic mode.
5. Inser a valid ErgoDex asset ticker, from the list of available tickers below, into the `swapAssetTicker` field.
6. If you so choose, feel free to modify the dex settings at your own risk.
7. Run `java -jar guapswap-<version>.jar --help` to get command usage directions. Use the `--help` flag after any command to get usage directions.

#### GuapSwap CLI Command Usage

1. Run `java -jar guapswap-<version>.jar --help` to get command usage directions. Use the `--help` flag after any command to get usage directions.

##### Generate Proxy Address

1. Run `java -jar guapswap-<version>.jar generate` to generate a proxy address.
2. Use the generated proxy address instead of your PK to receive your mining payout rewards.

##### Onetime Swap

1. Run `java -jar guapswap-<version>.jar swap --onetime <proxy_address>` to make a onetime swap with the given proxy address.

##### Automatic Swap

1. Run `java -jar guapswap-<version>.jar swap <proxy_address>` to run GuapSwap CLI in automatic mode. The swaps will occur according to the `swapIntervalInHours` set in the `guapswap_config.json` file.

##### Refund

1. Run `java -jar guapswap-<version>.jar refund <proxy_address>` to return all funds locked at the given proxy address to your PK wallet.

##### List

1. Run `java -jar guapswap-<version>.jar list <proxy_address>` to list all eUTXO boxes at the given proxy address.

### Usage WARNING

Please have at least 0.1 ERG at the proxy address before deciding to initiate a swap, otherwise you will need to fund it with more ERG. This is to ensure that there are enough funds to pay for the protocol fee and all of the dex fees. The value may also need to be higher depending on the token to be swapped, since some assets require a higher minimum amount of ERG to swap for them.

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
5. "Paideia"
6. "COMET"
7. "Erdoge"
8. "LunaDog"

## Pool Compatibility

### Known Compatible Pools

- [getblok.io](https://getblok.io)
- [nanopool.org](https://nanopool.org)
- [herominers.com](https://herominers.com)
- [666pool.com](https://666pool.com)
- [woolypooly.com](https://woolypooly.com)
- [k1pool.com](https://k1pool.com)
- [rkdn.app](https://rkdn.app)
- [solopool.org](https://solopool.org)

### Known Incompatible Pools (i.e. they do not accept P2S proxy addresses)

- [flypool.org](https://flypool.org)
- [2miners.com](https://2miners.com)
- [f2pool.com](https://f2pool.com)
- [leafpool.com](https://leafpool.com)
- [enigmapool.com](https://enigmapool.com)
- [cruxpool.com](https://cruxpool.com)
- [fairhash.org](https://fairhash.org)

## Walkthrough

If you would like to watch a walkthrough for GuapSwap CLI, consider subscribing to our YouTube channel [here](https://www.youtube.com/channel/UC9vdWQ_lLb41BO2hG3SvZOA). If a tutorial video is not already out, then it may be added soon.

## Reporting Issues

1. Join our [Discord](https://discord.com/invite/EfXsE4v2NM) channel, we welcome any and all feedback. 
2. Join the `#beta-testers` channel if you would like to try out pre-release versions of GuapSwap CLI.
3. Feel free to create a GitHub issue or fill out the Bug Report issue to let us know of any errors you encounter while using GuapSwap CLI.

## Support

If you would like to support the team and the project, please consider sending some ERG or SigUSD our way at the following address: 
`9gspiMa13K91MAVEhDM5iyjRePZSc8K42jJFTm8RkwyQBUPZ6BU`
