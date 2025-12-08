package com.example.Extra

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.extra.ui.theme.ExtraTheme
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableStatusCodes
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity(),
    CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener{

    var activityContext: Context?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityContext = this

        /*enableEdgeToEdge()
        setContent {
            ExtraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        */
    }

    private fun getNodes(context: Context){
            launch(context = Dispatchers.Default){
                val nodeList= Wearable.getNodeClient(context).connectedNodes
                try {
                    val nodes= Tasks.await(task = nodeList)
                    for(node in nodes){
                        Log.d(tag = "NODO", msg = node.toString())
                        Log.d(tag = "NODO", msg = "El id del nodo es: ${node.id}")
                    }
                }catch (exception: Exception){
                    Log.d(tag= "Error al obtener nodos", msg = exception.toString())
                }
            }
        }
    }


    private fun sendMessage(){
        val sendMessage= Wearable.getMessageClient(activityContext!!)
            .sendMessage(nodeID, PAYLOAD, "mensaje enviar".toByteArray())
            .addOnSuccessListener {
                Log.d("sendMessage", "Mensaje enviado con exito")
            }
            .addOnFailureListener { exception ->
                Log.d("sendMessage", "Error al enviar mensaje ${exception.message}")
            }
    }

    override fun onPause() {
        super.onPause()
        try{
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
        }catch (e: Exception){
            Log.d("onPause", e.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        try{
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        }catch (e: Exception){
            Log.d("onPause", e.toString())
        }
    }


    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        ExtraTheme {
            Greeting("Android")
        }
    }


    override fun onMessageReceived(ME: MessageEvent) {
        Log.d("onMessageReceived", Me.toString())
        Log.d("onMessageReceived", "nodo ${ME.sourceNodeId}"
                Log.d("onMessageReceived", "Payload ${ME.path}
            val message=String(ME.data, StandardCharsets.UTF_8)
        Log.d("onMessageReceived", "Mensaje: ${message}")
    }

    override fun onDataChanged(p0: DataEventBuffer) {

    }

    override fun onCapabilityChanged(p0: CapabilityInfo) {
    }