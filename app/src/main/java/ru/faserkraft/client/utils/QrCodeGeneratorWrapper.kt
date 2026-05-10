package ru.faserkraft.client.utils

import android.graphics.Bitmap

interface QrCodeGeneratorWrapper {
    fun generate(content: String, size: Int = 512): Bitmap
}