package com.example.extra

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.extra.ui.theme.ExtraTheme
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
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
    private var nodeID: String = ""
    private val PAYLOAD = "/sensor_data"
    private var sensorReading = mutableStateOf("Esperando datos...")
    private var isConnected = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityContext = this

        // Registrar listeners inmediatamente
        try{
            Wearable.getDataClient(this).addListener(this)
            Log.d("CEL_INIT", "DataClient listener registrado")
            Wearable.getMessageClient(this).addListener(this)
            Log.d("CEL_INIT", "MessageClient listener registrado")
            Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
            Log.d("CEL_INIT", "CapabilityClient listener registrado")
        }catch (e: Exception){
            Log.e("CEL_INIT", "Error registrando listeners: ${e.message}", e)
        }

        setContent {
            ExtraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartphoneApp()
                }
            }
        }
    }

    private fun getNodes(){
        launch(Dispatchers.Default){
            val nodeList= Wearable.getNodeClient(activityContext!!).connectedNodes
            try {
                val nodes= Tasks.await(nodeList)
                if(nodes.isNotEmpty()){
                    for(node in nodes){
                        Log.d("NODO", node.toString())
                        Log.d("NODO", "El id del nodo es: ${node.id}")
                        nodeID = node.id
                    }
                    isConnected.value = true
                } else {
                    isConnected.value = false
                    Log.d("NODO", "No hay nodos conectados")
                }
            }catch (exception: Exception){
                isConnected.value = false
                Log.d("Error al obtener nodos", exception.toString())
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
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        }catch (e: Exception){
            Log.d("onPause", e.toString())
        }
    }


    @Composable
    fun SmartphoneApp() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Receptor de Sensor",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Estado:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if(isConnected.value) "Conectado" else "Desconectado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if(isConnected.value) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lectura del Sensor:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sensorReading.value,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { getNodes() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Conectar con Reloj",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }


    override fun onMessageReceived(ME: MessageEvent) {
        Log.d("onMessageReceived", ME.toString())
        Log.d("onMessageReceived", "nodo ${ME.sourceNodeId}")
        Log.d("onMessageReceived", "Payload ${ME.path}")
        val message=String(ME.data, StandardCharsets.UTF_8)
        Log.d("onMessageReceived", "Mensaje: ${message}")
        
        // Actualizar UI con la lectura del sensor
        sensorReading.value = message
    }

    override fun onDataChanged(p0: DataEventBuffer) {

    }

    override fun onCapabilityChanged(p0: CapabilityInfo) {
    }
}