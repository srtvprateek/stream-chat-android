package io.getstream.chat.android.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

public class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("JcLog: MyReceiver is running")
    }
}