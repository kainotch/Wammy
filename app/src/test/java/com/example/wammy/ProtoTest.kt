package com.example.wammy

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.decodeFromByteArray
import org.junit.Test
import kotlinx.serialization.ExperimentalSerializationApi

@Serializable
data class DummyBackup(
    @ProtoNumber(1) val backupManga: List<String>
)

@OptIn(ExperimentalSerializationApi::class)
class ProtoTest {
    @Test
    fun testEmpty() {
        try {
            // Decode an empty byte array
            val backup = ProtoBuf.decodeFromByteArray<DummyBackup>(ByteArray(0))
            println("SUCCESS: size=" + backup.backupManga.size)
        } catch (e: Exception) {
            println("ERROR: " + e.message)
        }
    }
}
