package si.blarc.entity

import java.io.Serializable
import java.util.*

data class Player (
        val nickname: String,
        val isAdmin: Boolean,
        val generated: Boolean,
        val uuid: String = UUID.randomUUID().toString()
) : Serializable {
    val points = 0
}