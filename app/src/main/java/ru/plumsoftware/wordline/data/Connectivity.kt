package ru.plumsoftware.wordline.data

import android.content.Context
import android.net.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun connectivity(context: Context)=callbackFlow {
    val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    fun publish() {val capabilities=manager.getNetworkCapabilities(manager.activeNetwork);trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true)}
    val listener=object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network)=publish()
        override fun onLost(network: Network)=publish()
        override fun onCapabilitiesChanged(network: Network,capabilities: NetworkCapabilities)=publish()
    }
    publish();manager.registerDefaultNetworkCallback(listener)
    awaitClose {manager.unregisterNetworkCallback(listener)}
}.distinctUntilChanged()
