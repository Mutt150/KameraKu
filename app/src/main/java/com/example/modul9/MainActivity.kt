package com.example.modul9

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.modul9.ui.theme.Modul9Theme
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Modul9Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        KameraKuScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun KameraKuScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    // Auto-hide thumbnail
    LaunchedEffect(lastPhotoUri) {
        if (lastPhotoUri != null) {
            kotlinx.coroutines.delay(3000)
            lastPhotoUri = null
        }
    }

    var selector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var torchOn by remember { mutableStateOf(false) }

    var hasCameraPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    // CAMERA BINDING
    LaunchedEffect(previewView, selector, flashMode, hasCameraPermission) {
        val view = previewView ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        val provider = getCameraProvider(context)
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(view.surfaceProvider)
        }

        val ic = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val vc = buildVideoCapture()

        provider.unbindAll()
        val cam = provider.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview,
            ic,
            vc
        )

        camera = cam
        imageCapture = ic
        videoCapture = vc

        setTargetRotation(ic, view)
    }

    // UI
    if (!hasCameraPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Aplikasi membutuhkan izin kamera")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // PREVIEW AREA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView = it }
            )
        }

        // CARD ATAS (SWITCH + FLASH + TORCH)
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.CardDefaults.shape,
            elevation = androidx.compose.material3.CardDefaults.cardElevation(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { selector = toggleCameraSelector(selector) }) {
                    Text("Switch")
                }

                Button(onClick = {
                    flashMode =
                        if (flashMode == ImageCapture.FLASH_MODE_OFF)
                            ImageCapture.FLASH_MODE_ON
                        else
                            ImageCapture.FLASH_MODE_OFF
                }) {
                    Text(if (flashMode == ImageCapture.FLASH_MODE_OFF) "Flash OFF" else "Flash ON")
                }

                Button(onClick = {
                    torchOn = !torchOn
                    setTorch(camera, torchOn)
                }) {
                    Text(if (torchOn) "Torch ON" else "Torch OFF")
                }
            }
        }

        // CARD BAWAH (THUMBNAIL + AMBIL FOTO + REKAM)
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.CardDefaults.shape,
            elevation = androidx.compose.material3.CardDefaults.cardElevation(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // THUMBNAIL
                if (lastPhotoUri != null) {
                    AndroidView(
                        modifier = Modifier.size(64.dp),
                        factory = { ctx ->
                            android.widget.ImageView(ctx).apply {
                                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { it.setImageURI(lastPhotoUri) }
                    )
                } else {
                    Spacer(modifier = Modifier.size(64.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // TOMBOL FOTO
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val ic = imageCapture ?: return@Button
                        takePhoto(context, ic) { uri ->
                            lastPhotoUri = uri
                        }
                    }
                ) {
                    Text("Ambil Foto")
                }

                Spacer(modifier = Modifier.width(12.dp))

                // TOMBOL REKAM
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val vc = videoCapture ?: return@Button
                        toggleRecording(context, vc, recording) { recording = it }
                    }
                ) {
                    Text(if (recording == null) "Rekam" else "Stop")
                }
            }
        }
    }
}

suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val f = ProcessCameraProvider.getInstance(context)
        f.addListener({
            cont.resume(f.get())
        }, ContextCompat.getMainExecutor(context))
    }

fun outputOptions(ctx: Context, name: String): ImageCapture.OutputFileOptions {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
        }
    }

    return ImageCapture.OutputFileOptions.Builder(
        ctx.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()
}

fun takePhoto(
    ctx: Context,
    ic: ImageCapture,
    onSaved: (Uri) -> Unit
) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val outputOptions = outputOptions(ctx, "IMG_$name")

    ic.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(ctx),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(ctx, "Gagal foto: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                output.savedUri?.let(onSaved)
                Toast.makeText(ctx, "Foto tersimpan", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

fun buildVideoCapture(): VideoCapture<Recorder> {
    val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()
    return VideoCapture.withOutput(recorder)
}

fun setTargetRotation(imageCapture: ImageCapture, view: PreviewView) {
    view.display?.let {
        imageCapture.targetRotation = it.rotation
    }
}

fun toggleCameraSelector(current: CameraSelector): CameraSelector {
    return if (current == CameraSelector.DEFAULT_BACK_CAMERA)
        CameraSelector.DEFAULT_FRONT_CAMERA
    else
        CameraSelector.DEFAULT_BACK_CAMERA
}

fun setTorch(camera: Camera?, on: Boolean) {
    camera?.cameraControl?.enableTorch(on)
}

fun toggleRecording(
    ctx: Context,
    videoCapture: VideoCapture<Recorder>,
    currentRecording: Recording?,
    onRecordingChanged: (Recording?) -> Unit
) {
    if (currentRecording != null) {
        currentRecording.stop()
        onRecordingChanged(null)
        return
    }

    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_$name")
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/KameraKu")
        }
    }

    val outputOptions = MediaStoreOutputOptions.Builder(
        ctx.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        val recording = videoCapture.output
            .prepareRecording(ctx, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(ctx)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (!event.hasError()) {
                        Toast.makeText(ctx, "Video disimpan", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "Gagal rekam: ${event.error}", Toast.LENGTH_SHORT).show()
                    }
                    onRecordingChanged(null)
                }
            }
        onRecordingChanged(recording)
    } else {
        val recording = videoCapture.output
            .prepareRecording(ctx, outputOptions)
            .start(ContextCompat.getMainExecutor(ctx)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    onRecordingChanged(null)
                    Toast.makeText(ctx, "Video disimpan (Tanpa Suara)", Toast.LENGTH_SHORT).show()
                }
            }
        onRecordingChanged(recording)
    }
}

@Composable
fun CameraPreview(onPreviewReady: (PreviewView) -> Unit) {
    AndroidView(factory = { c ->
        PreviewView(c).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            post { onPreviewReady(this) }
        }
    })
}