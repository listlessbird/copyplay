package com.copyplay.data.device

import android.content.Context
import com.copyplay.domain.server.TailscaleDetector
import com.copyplay.domain.server.TailscalePolicy
import com.copyplay.domain.server.TailscaleStatus
import java.net.NetworkInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AndroidTailscaleDetector(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TailscaleDetector {
    private val mutex = Mutex()
    private var cachedStatus: TailscaleStatus? = null

    override suspend fun detect(): TailscaleStatus {
        mutex.withLock {
            cachedStatus?.let { return it }
            val status = withContext(ioDispatcher) { detectUncached() }
            cachedStatus = status
            return status
        }
    }

    private fun detectUncached(): TailscaleStatus {
        val isInstalled = runCatching {
            context.packageManager.getPackageInfo(TailscalePolicy.PACKAGE_NAME, 0)
        }.isSuccess

        val hasTailnetAddress = runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { interfaceEntry ->
                    runCatching {
                        interfaceEntry.inetAddresses.asSequence()
                    }.getOrDefault(emptySequence())
                }
                .any(TailscalePolicy::isTailnetAddress)
        }.getOrDefault(false)

        return TailscalePolicy.resolve(isInstalled, hasTailnetAddress)
    }
}
