package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.IllegalArgumentException
import java.util.*


class User private constructor (
    private val firstName: String,
    private val lastName: String?,
    email:String? = null,
    rawPhone:String? = null,
    meta: Map<String, Any>
) {

    val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().uppercaseChar() }
            .joinToString(" ")


    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }


    private var _login: String? = null
    internal var login: String
        set(value) {
            _login = value.lowercase(Locale.getDefault())
        }
        get() = _login!!
    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)

    }


    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary mail constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }
    //for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        _salt: String,
        hash: String,
        phone: String?
    ) : this(firstName, lastName, email = email, rawPhone = phone, meta = mapOf("src" to "csv")) {
        println("Secondary csv constructor")
        salt = _salt
        passwordHash = hash
    }

    init {
        println("First init block, primary constructor was called")
        check(firstName.isNotBlank()) { "FirstName must be not blank" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }
        phone = rawPhone
        login = email ?: phone!!
        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
            """.trimIndent()


    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcefgijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6) { (possible.indices).random().also { index -> append(possible[index]) } }
        }.toString()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash
    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
            passwordHash = encrypt(newPass)
            if (!accessCode.isNullOrEmpty()) accessCode = newPass
            println("Password $oldPass has been changed on new password $newPass")
        }
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    //private fun encrypt(password: String): String =
    //    salt.plus(password.md5()) //don't do that without salt
    private fun encrypt(password: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }
        println("Salt while encrypt $salt")
        return salt.plus(password).md5()
    }



    private fun String.md5(): String {
        val md: MessageDigest = MessageDigest.getInstance("MD5")
        val digest: ByteArray = md.digest(toByteArray())
        val hexString: String = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')


    }

    private fun String.frommd5(): String {
        val md: MessageDigest = MessageDigest.getInstance("MD5")
        val digest: ByteArray = md.digest(toByteArray())
        val hexString: String = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')


    }

    private fun sendAccessCodeToUser(phone: String?, code: String) {

        println(".......sending access code :$code on $phone")

    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ) : User {
            val (firstName, lastName) = fullName.fullNameToPair()
return when{
    !phone.isNullOrBlank() -> User(firstName, lastName, phone)
    !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
    else -> throw java.lang.IllegalArgumentException(" Email or phone  must be not null or blank")

}

        }

        fun importUser(
            fullName: String,
            email: String? = null,
            passwordHash: String? = null,
            salt: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return User(firstName, lastName, email, phone, mapOf("src" to "csv"))
                .apply {
                    this.salt = salt
                    this.passwordHash = passwordHash ?: ""
                }
        }
        fun parseCSV(csv: String): User {
            val user = csv.split(";", ":")
            val (firstName, lastName) = user[0].trim().fullNameToPair()
            return User(
                firstName,
                lastName,
                user[1].ifBlank{ null },
                user[2],
                user[3],
                user[4].ifBlank{ null }
            )
        }
        private fun String.fullNameToPair() : Pair<String, String?>{
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw java.lang.IllegalArgumentException(
                            "Fullname must contain only first " +
                                    "name and last name, current split result  ${this@fullNameToPair}"
                        )

                    }
                }

        }

    }


}







