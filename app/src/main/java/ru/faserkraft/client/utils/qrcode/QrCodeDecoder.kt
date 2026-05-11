package ru.faserkraft.client.utils.qrcode

fun isUfCode(str: String): Boolean {
    val pattern = Regex("^uf-\\d{7,9}$")
    return pattern.matches(str)
}

fun isUfPkgCode(str: String): Boolean {
    val pattern = Regex("^PKG-\\d{7}$")
    return pattern.matches(str)
}
