package com.example.blockchain

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

data class OpClaimReceipt(
    val txHash: String,
    val amountGeot: Double,
    val network: OpNetwork,
    val recipientAddress: String,
    val blockNumber: Long,
    val explorerUrl: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class OpWalletStatus(
    val network: OpNetwork,
    val walletAddress: String,
    val ethBalance: Double,
    val geotBalance: Double,
    val isRpcConnected: Boolean,
    val tokenContractAddress: String
)

class OpChainService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Queries the OP Chain RPC to read the live ERC-20 $GEOT token balance of a given wallet.
     * Uses eth_call with balanceOf(address) signature 0x70a08231.
     */
    suspend fun getGeotTokenBalance(
        network: OpNetwork,
        contractAddress: String,
        walletAddress: String
    ): Double = withContext(Dispatchers.IO) {
        if (walletAddress.isBlank() || contractAddress.isBlank()) return@withContext 0.0

        try {
            // Function selector for balanceOf(address) is 0x70a08231
            val cleanAddr = walletAddress.removePrefix("0x").padStart(64, '0')
            val callData = "0x70a08231$cleanAddr"

            val callObject = JSONObject().apply {
                put("to", contractAddress)
                put("data", callData)
            }

            val params = JSONArray().apply {
                put(callObject)
                put("latest")
            }

            val rpcResponse = makeRpcCall(network.rpcUrl, "eth_call", params)
            if (rpcResponse != null && rpcResponse.has("result")) {
                val hexResult = rpcResponse.getString("result").removePrefix("0x")
                if (hexResult.isNotBlank() && hexResult != "0") {
                    val rawUnits = BigInteger(hexResult, 16)
                    val divisor = BigDecimal.TEN.pow(OpChainConfig.TOKEN_DECIMALS)
                    val tokens = BigDecimal(rawUnits).divide(divisor, 4, java.math.RoundingMode.HALF_UP)
                    return@withContext tokens.toDouble()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching GEOT token balance on OP Chain", e)
        }

        0.0
    }

    /**
     * Queries native ETH balance on Optimism for gas status.
     */
    suspend fun getEthBalance(network: OpNetwork, walletAddress: String): Double = withContext(Dispatchers.IO) {
        if (walletAddress.isBlank()) return@withContext 0.0
        try {
            val params = JSONArray().apply {
                put(walletAddress)
                put("latest")
            }
            val rpcResponse = makeRpcCall(network.rpcUrl, "eth_getBalance", params)
            if (rpcResponse != null && rpcResponse.has("result")) {
                val hexResult = rpcResponse.getString("result").removePrefix("0x")
                if (hexResult.isNotBlank()) {
                    val wei = BigInteger(hexResult, 16)
                    val eth = BigDecimal(wei).divide(BigDecimal.TEN.pow(18), 4, java.math.RoundingMode.HALF_UP)
                    return@withContext eth.toDouble()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ETH balance on OP Chain", e)
        }
        0.0
    }

    /**
     * Verifies OP Chain RPC connection.
     */
    suspend fun checkRpcConnection(network: OpNetwork): Boolean = withContext(Dispatchers.IO) {
        try {
            val rpcResponse = makeRpcCall(network.rpcUrl, "eth_chainId", JSONArray())
            return@withContext rpcResponse != null && rpcResponse.has("result")
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Simulates claiming / minting ${'$'}GEOT tokens onto the OP Chain for the user's GPS trace,
     * computing cryptographic hash and block explorer link.
     */
    suspend fun claimTokensToOp(
        network: OpNetwork,
        recipientAddress: String,
        amountGeot: Double,
        referenceTraceId: Long?
    ): OpClaimReceipt = withContext(Dispatchers.IO) {
        // Generate pseudo-deterministic keccak-like tx hash
        val seed = "$recipientAddress:$amountGeot:${System.currentTimeMillis()}:$referenceTraceId"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(seed.toByteArray())
        val hexHash = "0x" + digest.joinToString("") { "%02x".format(it) }

        // Simulated OP block height (e.g. 120,000,000 range on OP)
        val baseBlock = if (network == OpNetwork.OP_MAINNET) 124500000L else 18500000L
        val simulatedBlock = baseBlock + (System.currentTimeMillis() % 10000)

        OpClaimReceipt(
            txHash = hexHash,
            amountGeot = amountGeot,
            network = network,
            recipientAddress = recipientAddress,
            blockNumber = simulatedBlock,
            explorerUrl = OpChainConfig.getExplorerTxUrl(network, hexHash)
        )
    }

    private fun makeRpcCall(rpcUrl: String, method: String, params: JSONArray): JSONObject? {
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", method)
            put("params", params)
        }

        val request = Request.Builder()
            .url(rpcUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    return JSONObject(body)
                }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "OpChainService"
    }
}
