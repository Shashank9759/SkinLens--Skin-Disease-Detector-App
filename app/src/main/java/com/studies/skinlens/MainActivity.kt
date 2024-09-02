package com.studies.skinlens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat
import com.studies.skinlens.Common.Device
import com.studies.skinlens.Domain.Models.Recognition
import com.studies.skinlens.Domain.Repository.Classifier
import com.studies.skinlens.Presentation.Screens.CameraScreen.CameraPreview
import com.studies.skinlens.Presentation.Screens.CameraScreen.DiseaseAnalyzer
import com.studies.skinlens.ui.theme.SkinLensTheme

class MainActivity : ComponentActivity() {

    private val cameraPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setCameraPreview()
            } else {
                setCameraPreview()
            }

        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                setCameraPreview()
            }
            else -> {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }


    }



    private fun setCameraPreview() {
        setContent {
            SkinLensTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                   // CameraPreviewScreen()

                    var classifications by remember { mutableStateOf(emptyList<Recognition>()) }


                    val classifier = Classifier.create(this, Device.CPU, 1)
                    val analyzer = remember {
                        DiseaseAnalyzer(
                            classifier =classifier,
                            onResults = {list->
                                classifications = list

                            }
                        )
                    }
                    val controller = remember {
                        LifecycleCameraController(applicationContext).apply {
                            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                            setImageAnalysisAnalyzer(
                                ContextCompat.getMainExecutor(applicationContext),
                                analyzer
                            )
                            imageAnalysisBackpressureStrategy=ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        }


                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {


                        CameraPreview(controller, Modifier.fillMaxSize()){

                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(4.dp)
                        ) {
                            classifications.forEach {
                                Row(

                                    modifier = Modifier

                                        .align(Alignment.CenterHorizontally)

                                        .padding(4.dp)

                                ){
                                    Text(
                                        text = it.title?.toUpperCase().toString(),

                                        textAlign = TextAlign.Start,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = String.format("%.2f%%", (it.confidence ?: 0f) * 100f),
                                         modifier = Modifier.padding(start=10.dp),
                                        textAlign = TextAlign.End,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                            }
                        }

                        val modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)

                            .background(MaterialTheme.colorScheme.onPrimaryContainer)
                            .padding(10.dp)
                        dropDownMenu(modifier)
                    }
                }
            }
        }
    }
}


@Composable
fun dropDownMenu(modifier: Modifier = Modifier) {
    // Boolean to store the expanded state of the dropdown
    var mExpanded by remember { mutableStateOf(false) }

    // List of options for the dropdown menu
    val mCities = listOf("CPU", "GPU")

    // String to store the selected text
    var mSelectedText by remember { mutableStateOf("") }

    // To store the size of the Text composable
    var mTextFieldSize by remember { mutableStateOf(Size.Zero) }

    // Icon that changes based on the expanded state
    val icon = if (mExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown

    Column(modifier = modifier,
        ) {



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically, // Vertically center the items in the Row
            horizontalArrangement = Arrangement.Center // Horizontally center items in the Row
        ) {
            Text(text = "Device",
                modifier=Modifier
                   .padding(end=4.dp)

                    .wrapContentSize(),
                textAlign = TextAlign.Center,

                color = Color.White
            )


            // Create a non-editable Box with Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        // Assign the width of the Text to the dropdown
                        mTextFieldSize = coordinates.size.toSize()
                    }
                    .clickable { mExpanded = !mExpanded }
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = if (mSelectedText.isEmpty()) "CPU" else mSelectedText,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                )

                // Trailing icon
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { mExpanded = !mExpanded }
                )
            }
        }


        // Create a DropdownMenu for the list of devices
        DropdownMenu(
            expanded = mExpanded,
            onDismissRequest = { mExpanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { mTextFieldSize.width.toDp() })
        ) {
            mCities.forEach { label ->
                DropdownMenuItem(onClick = {
                    mSelectedText = label
                    mExpanded = false
                }) {
                    Text(text = label)
                }
            }
        }
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
    SkinLensTheme {
        Greeting("Android")
    }
}


