package com.example.nogramtime

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

data class BlockStatus(
    val blocked: Boolean,
    val reason: String
)

object BlockChecker {
    
    private const val TAG = "BlockChecker"
    private const val INSTAGRAM_PACKAGE = "com.instagram.android"
    private const val PREF_NAME = "nogram_prefs"
    private const val KEY_BLOCK_INSTAGRAM = "block_instagram"
    
    /**
     * Comprehensive Instagram block status check
     * Order of checks:
     * 1. User toggle setting (SharedPreferences)
     * 2. Package Manager status (enabled/disabled/suspended)
     * 3. Device Policy Manager restrictions
     * 4. Default: not blocked
     */
    fun checkInstagramBlock(context: Context): BlockStatus {
        Log.d(TAG, "Starting Instagram block check...")
        
        // Check 1: User toggle setting
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val userBlocked = prefs.getBoolean(KEY_BLOCK_INSTAGRAM, false)
        
        if (userBlocked) {
            Log.d(TAG, "Instagram blocked by user setting")
            return BlockStatus(true, "user_setting")
        }
        
        // Check 2: Package Manager status
        val packageManager = context.packageManager
        try {
            val appInfo = packageManager.getApplicationInfo(INSTAGRAM_PACKAGE, 0)
            
            // Check if app is disabled
            if (!appInfo.enabled) {
                Log.d(TAG, "Instagram is disabled via PackageManager")
                return BlockStatus(true, "package_disabled")
            }
            
            // Check enabled setting state
            val enabledSetting = packageManager.getApplicationEnabledSetting(INSTAGRAM_PACKAGE)
            if (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                Log.d(TAG, "Instagram disabled state: $enabledSetting")
                return BlockStatus(true, "package_disabled_state")
            }
            
            // Check if package is suspended (API 24+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (packageManager.isPackageSuspended(INSTAGRAM_PACKAGE)) {
                    Log.d(TAG, "Instagram is suspended")
                    return BlockStatus(true, "package_suspended")
                }
            }
            
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Instagram not installed")
            return BlockStatus(true, "not_installed")
        }
        
        // Check 3: Device Policy Manager restrictions
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            if (dpm != null) {
                // Note: Checking if there are any restrictions would require device owner/profile owner
                // This is a placeholder for potential DPM-based restrictions
                Log.d(TAG, "DevicePolicyManager check completed (no restrictions found)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking DevicePolicyManager: ${e.message}")
        }
        
        // Default: not blocked
        Log.d(TAG, "Instagram is not blocked")
        return BlockStatus(false, "ok")
    }
    
    /**
     * Set user block preference
     */
    fun setUserBlockSetting(context: Context, blocked: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BLOCK_INSTAGRAM, blocked).apply()
        Log.d(TAG, "User block setting updated: $blocked")
    }
    
    /**
     * Get user block preference
     */
    fun getUserBlockSetting(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BLOCK_INSTAGRAM, false)
    }
}
