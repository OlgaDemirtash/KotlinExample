package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()



        fun registerUser(
            fullName: String,
            email: String,
            password: String
        ): User = User.makeUser(fullName, email = email, password = password)
            .also { user ->
                if (map[user.login] == null) map[user.login] = user
                else throw IllegalArgumentException("A user with this email already exists")
            }

        fun registerUserByPhone(
            fullName: String,
            rawPhone: String
        ): User {
            val pattern = """^\+(.*\d){11}""".toRegex()
            if (!pattern.matches(rawPhone)) {
                throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
            }

            return User.makeUser(fullName, phone = rawPhone)
                .also { user ->
                    if (map[rawPhone] == null) map[rawPhone] = user
                    else throw IllegalArgumentException("A user with this phone already exists")
                }
        }


        fun loginUser(login:String, password: String) : String? {
            return map [login.trim()]?.run {
                if(checkPassword(password)) this.userInfo
                else null
            }
        }

        fun requestAccessCode(login: String) : Unit {
            map[login.trim()]?.let {
                if (it.accessCode != null) it.changePassword(it.accessCode!!, it.generateAccessCode())
            }
        }




    fun importUsers(list: List<String>): List<User> {
        var userList = mutableListOf<User>()

        list.forEach {
            val csv = it.split(";")
            if (csv.size > 4) {
                val fullName = csv[0].trim()
                val email = if (csv[1].isBlank()) null else csv[1].trim()
                val phone = if (csv[3].isBlank()) null else csv[3].trim()
                val salt: String?
                val passwordHash: String?
                csv[2].split(":")
                    .run {
                        salt = if (first().isNullOrBlank()) null else first().trim()
                        passwordHash = if (last().isNullOrBlank()) null else last().trim()
                    }

                if (!fullName.isNullOrBlank()) {
                    userList.add(User.importUser(fullName, email, passwordHash, salt, phone))
                }
            }
        }

        return userList
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}

