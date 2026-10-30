package com.douo.prayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { PrayerScreen() }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun PrayerScreen() {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    var status by remember { mutableStateOf("جاري تحديد الموقع…") }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (!hasPermission) {
            locationPermission.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) location = loc
                    else status = "تعذر تحديد الموقع، تأكد أن GPS مفعل"
                }
        } else {
            status = "نحتاج إذن الموقع لحساب المواقيت"
        }
    }

    val loc = location
    if (loc == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(status, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val prayers = remember(loc) {
        PrayerCalculator.computeForDay(loc.latitude, loc.longitude, Date())
    }
    LaunchedEffect(loc) {
        PrayerAlarmReceiver.scheduleAll(
            context, PrayerCalculator.upcoming(loc.latitude, loc.longitude)
        )
    }

    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) { now = Date(); delay(30_000) }
    }

    val next = prayers.firstOrNull { it.time.after(now) }
        ?: PrayerCalculator.upcoming(loc.latitude, loc.longitude, now).firstOrNull()
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale("ar")) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "المواقيت اليوم",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        next?.let {
            val mins = ((it.time.time - now.time) / 60_000).coerceAtLeast(0)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الصلاة القادمة: ${it.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "بعد ${mins / 60} ساعة و ${mins % 60} دقيقة",
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(prayers) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(p.name, fontSize = 18.sp)
                        Text(timeFmt.format(p.time), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
