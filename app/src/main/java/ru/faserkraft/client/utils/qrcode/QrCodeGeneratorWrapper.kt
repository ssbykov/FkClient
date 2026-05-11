package ru.faserkraft.client.utils.qrcode

import android.graphics.Bitmap

interface QrCodeGeneratorWrapper {
    fun generate(content: String, size: Int = 512): Bitmap
}