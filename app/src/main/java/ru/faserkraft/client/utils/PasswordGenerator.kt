package ru.faserkraft.client.utils


fun generatePassword(length: Int = 16): String {
    require(length >= 4) {
        "Password length must be at least 3"
    }

    val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val special = $$"!@#$%^&*()-_."
    val allChars = upper + lower + digits + special

    val secureRandom = java.security.SecureRandom()
    val chars = mutableListOf<Char>()

    // Гарантируем обязательные классы
    chars += upper[secureRandom.nextInt(upper.length)]
    chars += lower[secureRandom.nextInt(lower.length)]
    chars += digits[secureRandom.nextInt(digits.length)]
    chars += special[secureRandom.nextInt(special.length)]

    // Остальные символы — любые из разрешённых
    repeat(length - chars.size) {
        chars += allChars[secureRandom.nextInt(allChars.length)]
    }

    chars.shuffle(secureRandom)
    return chars.joinToString("")
}