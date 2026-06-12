package com.focusguard.utils

import android.content.Context
import android.provider.ContactsContract

data class PhoneContact(
    val id: String,
    val name: String,
    val phone: String
)

object ContactsHelper {

    // Lee contactos con número de teléfono. Requiere READ_CONTACTS.
    fun getContactsWithPhone(context: Context): List<PhoneContact> {
        val resolver = context.contentResolver
        val out = LinkedHashMap<String, PhoneContact>()  // dedup por id, mantiene orden

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val name = cursor.getString(nameIdx) ?: continue
                val phone = cursor.getString(phoneIdx) ?: continue
                // Quedarse con el primer número de cada contacto
                if (!out.containsKey(id)) {
                    out[id] = PhoneContact(id, name.trim(), normalizePhone(phone))
                }
            }
        }
        return out.values.toList()
    }

    // Normaliza para comparar: deja solo dígitos y un + inicial opcional.
    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() || it == '+' }
        return digits
    }

    // Compara dos números ignorando formato. Match si los últimos 8 dígitos coinciden
    // (evita falsos negativos por prefijos de país/área).
    fun phonesMatch(a: String, b: String): Boolean {
        val da = a.filter { it.isDigit() }
        val db = b.filter { it.isDigit() }
        if (da.isEmpty() || db.isEmpty()) return false
        val tailA = da.takeLast(8)
        val tailB = db.takeLast(8)
        return tailA == tailB
    }
}
