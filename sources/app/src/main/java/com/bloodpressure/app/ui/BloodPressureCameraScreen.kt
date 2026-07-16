package com.bloodpressure.app.ui

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

@Composable
fun BloodPressureCameraScreen(
    onCaptured: (List<Bitmap>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusRequest by remember { mutableIntStateOf(0) }
    val captureScope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val bindingActive = remember { AtomicBoolean(true) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) cameraError = "需要相机权限才能拍照识别"
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(focusRequest) {
        if (focusRequest > 0) {
            delay(1_000)
            focusPoint = null
        }
    }

    DisposableEffect(cameraProviderFuture) {
        bindingActive.set(true)
        onDispose {
            bindingActive.set(false)
            boundCamera = null
            if (cameraProviderFuture.isDone) runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }
    BackHandler { onBack() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                factory = { previewContext ->
                    PreviewView(previewContext).apply {
                        previewView = this
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                boundCamera?.let { camera ->
                                    val point = meteringPointFactory.createPoint(event.x, event.y)
                                    val action = FocusMeteringAction.Builder(
                                        point,
                                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                    )
                                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                        .build()
                                    camera.cameraControl.startFocusAndMetering(action)
                                    focusPoint = Offset(event.x, event.y)
                                    focusRequest++
                                }
                                view.performClick()
                            }
                            true
                        }
                        cameraProviderFuture.addListener({
                            if (!bindingActive.get()) return@addListener
                            runCatching {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }
                                cameraProvider.unbindAll()
                                boundCamera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview
                                )
                            }.onFailure { cameraError = "相机启动失败，请返回重试" }
                        }, ContextCompat.getMainExecutor(previewContext))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            focusPoint?.let { point ->
                drawCircle(
                    color = Color(0xFF5FE1FF),
                    radius = 34.dp.toPx(),
                    center = point,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Column {
                    Text("拍摄血压计", color = Color.White, fontSize = 18.sp)
                    Text("让发光屏幕填满大框，三行数字完整落入小框", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val frameWidth = maxWidth * 0.84f
            val frameHeight = frameWidth / 0.78f
            Box(
                Modifier
                    .size(frameWidth, frameHeight)
                    .align(Alignment.Center)
                    .border(3.dp, Color(0xFF5FE1FF), RoundedCornerShape(24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .width(frameWidth * 0.54f)
                        .height(frameHeight * 0.28f)
                        .align(Alignment.TopStart)
                        .offset(x = frameWidth * 0.39f, y = frameHeight * 0.14f)
                        .border(2.dp, Color(0xFF5FE1FF), RoundedCornerShape(10.dp))
                )
                Box(
                    modifier = Modifier
                        .width(frameWidth * 0.54f)
                        .height(frameHeight * 0.23f)
                        .align(Alignment.TopStart)
                        .offset(x = frameWidth * 0.39f, y = frameHeight * 0.44f)
                        .border(2.dp, Color(0xFF5FE1FF), RoundedCornerShape(10.dp))
                )
                Box(
                    modifier = Modifier
                        .width(frameWidth * 0.54f)
                        .height(frameHeight * 0.16f)
                        .align(Alignment.TopStart)
                        .offset(x = frameWidth * 0.39f, y = frameHeight * 0.68f)
                        .border(2.dp, Color(0xFF5FE1FF), RoundedCornerShape(10.dp))
                )
                Text(
                    "收缩压",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = frameWidth * 0.06f, y = frameHeight * 0.26f)
                )
                Text(
                    "舒张压",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = frameWidth * 0.06f, y = frameHeight * 0.54f)
                )
                Text(
                    "心率",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = frameWidth * 0.06f, y = frameHeight * 0.73f)
                )
                Text(
                    "数字明显小于框时，请再靠近",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }

        cameraError?.let {
            Text(
                it,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            )
        }

        FilledIconButton(
            onClick = {
                if (isCapturing) return@FilledIconButton
                isCapturing = true
                captureScope.launch {
                    val frames = mutableListOf<Bitmap>()
                    var handedOff = false
                    try {
                        repeat(7) {
                            previewView?.bitmap?.let { frame ->
                                frames += cropToGuideFrame(frame)
                            }
                            delay(120)
                        }
                        if (frames.size == 7) {
                            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                                withContext(Dispatchers.IO) { saveDebugFrames(context.cacheDir, frames) }
                            }
                            handedOff = true
                            onCaptured(frames)
                        } else {
                            cameraError = "画面尚未稳定，请重试"
                        }
                    } catch (_: Throwable) {
                        cameraError = "拍摄失败，请重试"
                    } finally {
                        if (!handedOff) frames.forEach { if (!it.isRecycled) it.recycle() }
                        isCapturing = false
                    }
                }
            },
            enabled = hasPermission && !isCapturing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
        ) {
            if (isCapturing) {
                CircularProgressIndicator(Modifier.size(28.dp), color = Color.Black, strokeWidth = 3.dp)
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = "拍照", tint = Color.Black, modifier = Modifier.size(32.dp))
            }
        }
    }
}

private fun saveDebugFrames(cacheDir: File, frames: List<Bitmap>) {
    val root = File(cacheDir, "recognition_debug").apply { mkdirs() }
    val directory = File(root, "attempt_${System.currentTimeMillis()}").apply { mkdirs() }
    frames.forEachIndexed { index, bitmap ->
        FileOutputStream(File(directory, "frame_$index.jpg")).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
    }
    root.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith("attempt_") }
        ?.sortedByDescending { it.name }
        ?.drop(5)
        ?.forEach { it.deleteRecursively() }
    root.listFiles()
        ?.filter { it.isFile && it.name.startsWith("frame_") }
        ?.forEach { it.delete() }
}

private fun cropToGuideFrame(bitmap: Bitmap): Bitmap {
    val cropWidth = (bitmap.width * 0.84f).toInt().coerceAtLeast(1)
    val cropHeight = (cropWidth / 0.78f).toInt().coerceAtMost(bitmap.height)
    val left = (bitmap.width - cropWidth) / 2
    val top = (bitmap.height - cropHeight) / 2
    val cropped = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    val scaled = Bitmap.createScaledBitmap(cropped, 600, 770, true)
    if (cropped !== bitmap && cropped !== scaled) cropped.recycle()
    if (bitmap !== scaled && !bitmap.isRecycled) bitmap.recycle()
    return scaled
}
