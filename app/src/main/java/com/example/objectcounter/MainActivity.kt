package com.example.objectcounter

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212), primary = Color(0xFF4CAF50))) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var savedScans by remember { mutableStateOf<List<SavedScan>>(emptyList()) }
    var selectedScanForDetail by remember { mutableStateOf<SavedScan?>(null) }
    val context = LocalContext.current

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            savedScans = ScanStorage.loadScans(context)
        }
    }

    if (selectedScanForDetail != null) {
        ScanDetailScreen(
            scan = selectedScanForDetail!!,
            onBack = { selectedScanForDetail = null }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E1E1E)) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Сканер") },
                    label = { Text("Сканирование") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Сканы") },
                    label = { Text("Сохраненные") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (selectedTab == 0) {
                CameraScreen()
            } else {
                GalleryScreen(
                    scans = savedScans,
                    onScanClick = { selectedScanForDetail = it },
                    onDeleteClick = { scan ->
                        ScanStorage.deleteScan(scan)
                        savedScans = ScanStorage.loadScans(context)
                    }
                )
            }
        }
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
        var currentProxy by remember { mutableStateOf<ImageProxy?>(null) }
        var latestBitmap by remember { mutableStateOf<Bitmap?>(null) } // Храним копию кадра
        var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
        val detectorHelper = remember { ObjectDetectorHelper(context) }

        Column(modifier = Modifier.fillMaxSize()) {
            // ВЕРХ: Камера
            Box(modifier = Modifier.fillMaxWidth().weight(0.6f).padding(16.dp).clip(RoundedCornerShape(24.dp))) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).also { previewViewRef = it }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                        currentProxy = proxy
                                        // Сохраняем копию ДО закрытия прокси
                                        latestBitmap = proxy.toBitmap()
                                        detections = detectorHelper.detect(proxy)
                                    }
                                }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                            } catch (e: Exception) { e.printStackTrace() }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DetectionOverlay(
                    detections = detections,
                    imageProxy = currentProxy,
                    previewView = previewViewRef,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // НИЗ: Кнопка и Счетчик
            Column(modifier = Modifier.fillMaxWidth().weight(0.4f).background(Color(0xFF1E1E1E)).padding(16.dp)) {
                Text("Всего объектов: ${detections.size}", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    onClick = {
                        val bitmapToSave = latestBitmap
                        if (bitmapToSave != null && detections.isNotEmpty()) {
                            try {
                                // 1. Рисуем квадратики на оригинальном кадре (где они идеально совпадают)
                                val bitmapWithBoxes = drawDetectionsOnBitmap(bitmapToSave, detections)

                                // 2. Теперь поворачиваем ВСЮ картинку с уже нарисованными квадратиками
                                // Если фото окажется перевернутым вверх ногами, поменяйте 90f на 270f
                                val finalBitmap = rotateBitmap(bitmapWithBoxes, 90f)

                                // 3. Сохраняем
                                val summary = detectorHelper.getObjectsSummary(detections)
                                ScanStorage.saveScan(context, finalBitmap, summary)

                                android.widget.Toast.makeText(context, "Скан сохранен!", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Нет объектов в кадре", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Сделать фото и сохранить", fontSize = 16.sp, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // СПИСОК ОБЪЕКТОВ ВОЗВРАЩЕН СЮДА
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detections.groupBy { it.categories[0].label }.toList()) { (label, list) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label.replaceFirstChar { it.uppercase() }, color = Color.White)
                            Text("${list.size} шт.", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нужно разрешение на камеру", color = Color.White)
        }
    }
}

// Вспомогательная функция для рисования квадратиков
fun drawDetectionsOnBitmap(bitmap: Bitmap, detections: List<Detection>): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    val paint = Paint().apply {
        color = AndroidColor.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    detections.forEach {
        val box = it.boundingBox
        canvas.drawRect(RectF(box.left, box.top, box.right, box.bottom), paint)
    }
    return mutableBitmap
}

@Composable
fun GalleryScreen(scans: List<SavedScan>, onScanClick: (SavedScan) -> Unit, onDeleteClick: (SavedScan) -> Unit) {
    if (scans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет сохраненных сканов", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(scans, key = { it.imageFile.absolutePath }) { scan ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onScanClick(scan) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bitmap = remember(scan.imageFile) { BitmapFactory.decodeFile(scan.imageFile.absolutePath) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(scan.imageFile.lastModified()))
                        Text("Скан от $date", color = Color.LightGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(scan.objectSummary, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                    }

                    IconButton(onClick = { onDeleteClick(scan) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(scan: SavedScan, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали скана", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            val bitmap = remember(scan.imageFile) { BitmapFactory.decodeFile(scan.imageFile.absolutePath) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Full Image",
                    modifier = Modifier.fillMaxWidth().weight(0.6f).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Найдено на фото:", color = Color.LightGray, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(scan.objectSummary, color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
    if (angle == 0f) return source
    val matrix = android.graphics.Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}