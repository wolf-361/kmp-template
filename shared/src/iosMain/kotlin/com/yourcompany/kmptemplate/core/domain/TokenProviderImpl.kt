package com.yourcompany.kmptemplate.core.domain

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
actual class TokenProviderImpl actual constructor() : TokenProvider {

    override suspend fun getAccessToken(): String? = keychainGet(KEY_ACCESS)
    override suspend fun getRefreshToken(): String? = keychainGet(KEY_REFRESH)
    override suspend fun getExpiresAt(): Long = keychainGet(KEY_EXPIRES)?.toLongOrNull() ?: 0L

    override suspend fun saveTokens(access: String, refresh: String, expiresAt: Long) {
        keychainSet(KEY_ACCESS, access)
        keychainSet(KEY_REFRESH, refresh)
        keychainSet(KEY_EXPIRES, expiresAt.toString())
    }

    override suspend fun clearTokens() {
        keychainDelete(KEY_ACCESS)
        keychainDelete(KEY_REFRESH)
        keychainDelete(KEY_EXPIRES)
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildQuery(key: String): NSMutableDictionary = NSMutableDictionary().also { d ->
        d.setObject(kSecClassGenericPassword!!, forKey = kSecClass as Any)
        d.setObject(SERVICE, forKey = kSecAttrService as Any)
        d.setObject(key, forKey = kSecAttrAccount as Any)
    }

    @Suppress("UNCHECKED_CAST")
    private fun keychainGet(key: String): String? = memScoped {
        val query = buildQuery(key).also { d ->
            d.setObject(NSNumber(bool = true), forKey = kSecReturnData as Any)
            d.setObject(kSecMatchLimitOne!!, forKey = kSecMatchLimit as Any)
        }
        val result = alloc<ObjCObjectVar<Any?>>()
        val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr.reinterpret())
        if (status != errSecSuccess) return null
        val data = result.value as? NSData ?: return null
        NSString.create(data, NSUTF8StringEncoding) as? String
    }

    @Suppress("UNCHECKED_CAST")
    private fun keychainSet(key: String, value: String) {
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val searchQuery = buildQuery(key)
        if (SecItemCopyMatching(searchQuery as CFDictionaryRef, null) == errSecSuccess) {
            val update = NSMutableDictionary().also { d -> d.setObject(data, forKey = kSecValueData as Any) }
            SecItemUpdate(searchQuery as CFDictionaryRef, update as CFDictionaryRef)
        } else {
            searchQuery.setObject(data, forKey = kSecValueData as Any)
            SecItemAdd(searchQuery as CFDictionaryRef, null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun keychainDelete(key: String) = SecItemDelete(buildQuery(key) as CFDictionaryRef)

    private companion object {
        const val SERVICE = "kmptemplate.auth"
        const val KEY_ACCESS = "auth_access_token"
        const val KEY_REFRESH = "auth_refresh_token"
        const val KEY_EXPIRES = "auth_expires_at"
    }
}
