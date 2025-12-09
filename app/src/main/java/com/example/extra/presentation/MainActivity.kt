package com.example.extra.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.net.Uri
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.extra.presentation.theme.ExtraTheme
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(),
    CoroutineScope by MainScope(),
    SensorEventListener,
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    var activityContext: Context? = null

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor?=null
    private var sensorType=Sensor.TYPE_GYROSCOPE

    private var sensorReading = mutableStateOf("0.0")
    private var isSensorActive = mutableStateOf(false)

    private var nodeID: String = ""
    private val PAYLOAD = "/sensor_data"


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        activityContext = this

        setTheme(android.R.style.Theme_DeviceDefault)

        // Inicializar sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(sensorType)

        // Registrar listeners inmediatamente
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {
            Log.d("onCreate", e.toString())
        }

        // Obtener nodos conectados
        getNodes()

        setContent {
            WearApp()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.d("onPause", e.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {
            Log.d("onPause", e.toString())
        }
    }

    override fun onDataChanged(p0: DataEventBuffer) {

    }

    override fun onMessageReceived(p0: MessageEvent) {

    }

    override fun onCapabilityChanged(p0: CapabilityInfo) {

    }

    private fun startSensor() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1001)
            return
        }

        if(sensor!=null){
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(SE: SensorEvent?) {
        if (SE?.sensor?.type==sensorType){
            val lectura=SE.values[0]
            sensorReading.value = String.format("%.2f", lectura)
            Log.d("onSensorChanged", "lectura ${lectura}")
        }
    }

    private fun getNodes(){
        launch(Dispatchers.Default){
            val nodeList= Wearable.getNodeClient(activityContext!!).connectedNodes
            try {
                val nodes= Tasks.await(nodeList)
                for(node in nodes){
                    Log.d("NODO", node.toString())
                    Log.d("NODO", "El id del nodo es: ${node.id}")
                    nodeID = node.id
                }
            }catch (exception: Exception){
                Log.d("Error al obtener nodos", exception.toString())
            }
        }
    }

    private fun sendSensorData(){
        if(nodeID.isEmpty()){
            Log.d("sendSensorData", "No hay nodos conectados")
            return
        }
        
        Wearable.getMessageClient(activityContext!!)
            .sendMessage(nodeID, PAYLOAD, sensorReading.value.toByteArray())
            .addOnSuccessListener {
                Log.d("sendSensorData", "Mensaje enviado: ${sensorReading.value}")
            }
            .addOnFailureListener { exception ->
                Log.d("sendSensorData", "Error: ${exception.message}")
            }
    }

    @Composable
    fun WearApp() {
        ExtraTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                TimeText()
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Sensor Giroscopio",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = sensorReading.value,
                        style = MaterialTheme.typography.display1,
                        color = MaterialTheme.colors.secondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if(isSensorActive.value){
                                sensorManager.unregisterListener(this@MainActivity)
                                isSensorActive.value = false
                            } else {
                                startSensor()
                                isSensorActive.value = true
                            }
                        }
                    ) {
                        Text(
                            text = if(isSensorActive.value) "Detener" else "Iniciar",
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { sendSensorData() }
                    ) {
                        Text(
                            text = "Enviar",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}