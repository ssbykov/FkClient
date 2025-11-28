package ru.faserkraft.client.utils


fun generatePassword(length: Int = 16): String {
    val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val special = "!@#\$%^&*()-_."
    val allChars = upper + lower + digits + special

    val secureRandom = java.security.SecureRandom()

    while (true) {
        val chars = mutableListOf<Char>()

        // Гарантируем обязательные классы
        chars += upper[secureRandom.nextInt(upper.length)]
        chars += lower[secureRandom.nextInt(lower.length)]
        chars += digits[secureRandom.nextInt(digits.length)]

        // Остальные символы — любые из разрешённых
        repeat(length - chars.size) {
            chars += allChars[secureRandom.nextInt(allChars.length)]
        }

        // Перемешиваем
        chars.shuffle(secureRandom)

        val password = chars.joinToString("")


        return password
    }
}