package com.studies.skinlens

sealed class Screen(val route: String) {
    object CameraScreen: Screen("Camera_Screen")

}