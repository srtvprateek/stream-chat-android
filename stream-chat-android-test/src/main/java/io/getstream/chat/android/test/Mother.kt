package io.getstream.chat.android.test

import java.io.File
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

private val charPool: CharArray = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toCharArray()

public fun positiveRandomInt(maxInt: Int = Int.MAX_VALUE - 1): Int =
    Random.nextInt(1, maxInt + 1)

public fun positiveRandomLong(maxLong: Long = Long.MAX_VALUE - 1): Long =
    Random.nextLong(1, maxLong + 1)

public fun randomInt(): Int = Random.nextInt()
public fun randomIntBetween(min: Int, max: Int): Int = Random.nextInt(min, max + 1)
public fun randomLong(): Long = Random.nextLong()
public fun randomLongBetween(min: Long, max: Long): Long = Random.nextLong(min, max + 1)
public fun randomBoolean(): Boolean = Random.nextBoolean()
public fun randomString(size: Int = 20): String = buildString(capacity = size) {
    repeat(size) {
        append(charPool.random())
    }
}
public fun randomCID(): String = "${randomString()}:${randomString()}"

public fun randomFile(extension: String = randomString(3)): File {
    return File("${randomString()}.$extension")
}

public fun randomImageFile(): File = randomFile(extension = "jpg")

public fun randomFiles(
    size: Int = positiveRandomInt(10),
    creationFunction: (Int) -> File = { randomFile() }
): List<File> = (1..size).map(creationFunction)

public fun randomDate(): Date = Date(randomLong())

public fun randomDateBefore(date: Long): Date = Date(date - positiveRandomInt())

public fun createDate(
    year: Int = positiveRandomInt(),
    month: Int = positiveRandomInt(),
    date: Int = positiveRandomInt(),
    hourOfDay: Int = 0,
    minute: Int = 0,
    seconds: Int = 0
): Date {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, date, hourOfDay, minute, seconds)
    return calendar.time
}
