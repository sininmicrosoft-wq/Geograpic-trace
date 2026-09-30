package com.example.blockchain

enum class OpNetwork(
    val id: String,
    val displayName: String,
    val chainId: Long,
    val rpcUrl: String,
    val explorerUrl: String,
    val isTestnet: Boolean
) {
    OP_SEPOLIA(
        id = "OP_SEPOLIA",
        displayName = "OP Sepolia Testnet",
        chainId = 11155420L,
        rpcUrl = "https://sepolia.optimism.io",
        explorerUrl = "https://sepolia-optimism.etherscan.io",
        isTestnet = true
    ),
    OP_MAINNET(
        id = "OP_MAINNET",
        displayName = "OP Mainnet",
        chainId = 10L,
        rpcUrl = "https://mainnet.optimism.io",
        explorerUrl = "https://optimistic.etherscan.io",
        isTestnet = false
    );

    companion object {
        fun fromId(id: String): OpNetwork {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: OP_SEPOLIA
        }
    }
}

object OpChainConfig {
    const val TOKEN_NAME = "GeoTrace Token"
    const val TOKEN_SYMBOL = "\$GEOT"
    const val TOKEN_DECIMALS = 18

    // Default deployed demo token address on Optimism Sepolia
    const val DEFAULT_OP_SEPOLIA_CONTRACT = "0x8B32e91244031665aEaE1FE5b9A547f3b8a688b1"
    const val DEFAULT_OP_MAINNET_CONTRACT = "0x6e07F0c89A127392659e97c9B3E2a31E0170560a"

    // 100 in-app GeoPoints = 1.0 $GEOT on OP Chain
    const val GEOPOINTS_PER_GEOT = 100.0

    fun formatAddress(address: String): String {
        if (address.isBlank()) return "Not Connected"
        if (address.length <= 10) return address
        return "${address.take(6)}...${address.takeLast(4)}"
    }

    fun getExplorerTxUrl(network: OpNetwork, txHash: String): String {
        return "${network.explorerUrl}/tx/$txHash"
    }

    fun getExplorerAddressUrl(network: OpNetwork, address: String): String {
        return "${network.explorerUrl}/address/$address"
    }
}
