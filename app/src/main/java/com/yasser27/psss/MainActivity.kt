package com.yasser27.psss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

// ألوان Neumorphism الداكنة
val DarkBackground = Color(0xFF1E1E1E)
val LightShadow = Color(0xFF2A2A2A)
val DarkShadow = Color(0xFF121212)
val AccentColor = Color(0xFF00BFA5)

class MainActivity : ComponentActivity() {
    private var serverThread: Thread? = null
    private val serverPort = 8080
    private var isRunning by mutableStateOf(false)
    private var currentIp by mutableStateOf("Checking IP...")

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentIp = getIpAddress()

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(2000)
                showSplash = false
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }, label = ""
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen()
                        } else {
                            MainContent(
                                ip = currentIp,
                                port = serverPort,
                                isRunning = isRunning,
                                onStartClick = { 
                                    currentIp = getIpAddress() // تحديث الـ IP عند البدء
                                    startServer() 
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startServer() {
        if (isRunning) return
        isRunning = true
        serverThread = thread {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(serverPort)
                while (isRunning) {
                    val socket = try { serverSocket.accept() } catch (e: Exception) { null }
                    if (socket != null) {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    private fun handleClient(socket: Socket) {
        thread {
            try {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = DataOutputStream(socket.getOutputStream())
                val requestLine = input.readLine() ?: return@thread
                val path = requestLine.split(" ").getOrNull(1)?.let {
                    if (it == "/") "/index.html" else it
                } ?: "/index.html"

                val cleanPath = path.substringBefore("?")
                val assetPath = "here" + cleanPath
                
                try {
                    val inputStream = assets.open(assetPath)
                    val bytes = inputStream.readBytes()
                    val contentType = when {
                        cleanPath.endsWith(".html") -> "text/html"
                        cleanPath.endsWith(".p3t") -> "application/octet-stream"
                        else -> "application/octet-stream"
                    }
                    output.writeBytes("HTTP/1.1 200 OK\r\n")
                    output.writeBytes("Content-Type: $contentType\r\n")
                    output.writeBytes("Content-Length: ${bytes.size}\r\n")
                    output.writeBytes("Access-Control-Allow-Origin: *\r\n")
                    output.writeBytes("\r\n")
                    output.write(bytes)
                    inputStream.close()
                } catch (e: IOException) {
                    val response = "404 Not Found".toByteArray()
                    output.writeBytes("HTTP/1.1 404 Not Found\r\n")
                    output.writeBytes("\r\n")
                    output.write(response)
                }
                output.flush()
                socket.close()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serverThread?.interrupt()
    }

    private fun getIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            // البحث عن واجهات الواي فاي أو نقطة الاتصال أولاً
            for (ni in interfaces) {
                if (!ni.isUp || ni.isLoopback) continue
                val addresses = ni.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: ""
                        // تجاهل العناوين الوهمية إذا وجدت
                        if (host.isNotEmpty() && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        return "Connect to Wi-Fi or Hotspot"
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ps3 hen",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "by yasser-27 github",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MainContent(ip: String, port: Int, isRunning: Boolean, onStartClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(25.dp), ambientColor = DarkShadow, spotColor = DarkShadow)
                .background(DarkBackground, RoundedCornerShape(25.dp))
                .padding(25.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isRunning) "SERVER ACTIVE" else "SERVER READY",
                    color = if (isRunning) AccentColor else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                if (isRunning) {
                    SelectionContainer {
                        Text(
                            text = "http://$ip:$port",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(ip, color = Color.DarkGray, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = onStartClick,
            modifier = Modifier
                .size(150.dp)
                .shadow(15.dp, RoundedCornerShape(75.dp), ambientColor = DarkShadow, spotColor = DarkShadow)
                .clip(RoundedCornerShape(75.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) AccentColor else DarkBackground,
                contentColor = if (isRunning) Color.Black else AccentColor
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isRunning) "RUNNING" else "START",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Ensure PS3 is on the same network",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
