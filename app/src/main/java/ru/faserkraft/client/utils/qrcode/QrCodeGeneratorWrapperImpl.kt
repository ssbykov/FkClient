package ru.faserkraft.client.utils.qrcode

import android.graphics.Bitmap
import javax.inject.Inject

class QrCodeGeneratorWrapperImpl @Inject constructor() : QrCodeGeneratorWrapper {
    override fun generate(content: String, size: Int): Bitmap =
        QrCodeGenerator.generate(content, size)
}