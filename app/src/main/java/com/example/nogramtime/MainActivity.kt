package com.example.nogramtime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.nogramtime.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        updateBlockStatus()
    }
    
    override fun onResume() {
        super.onResume()
        updateBlockStatus()
    }
    
    private fun setupUI() {
        // Set initial toggle state
        val currentSetting = BlockChecker.getUserBlockSetting(this)
        binding.blockToggle.isChecked = currentSetting
        
        // Toggle listener
        binding.blockToggle.setOnCheckedChangeListener { _, isChecked ->
            BlockChecker.setUserBlockSetting(this, isChecked)
            Log.d(TAG, "Block toggle changed to: $isChecked")
            updateBlockStatus()
            
            // Show feedback
            val message = if (isChecked) {
                "Instagram blocking enabled"
            } else {
                "Instagram blocking disabled"
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
        
        // Diagnostics button
        binding.diagnosticsButton.setOnClickListener {
            showDiagnostics()
        }
    }
    
    private fun updateBlockStatus() {
        val status = BlockChecker.checkInstagramBlock(this)
        
        if (status.blocked) {
            // Show block status banner
            binding.statusBanner.visibility = View.VISIBLE
            binding.statusText.text = getString(R.string.instagram_blocked_message, status.reason)
            binding.statusBanner.setBackgroundColor(getColor(android.R.color.holo_red_light))
            
            // Show action button for certain block reasons
            when (status.reason) {
                "package_disabled", "package_disabled_state", "package_suspended" -> {
                    binding.actionButton.visibility = View.VISIBLE
                    binding.actionButton.text = getString(R.string.open_settings)
                    binding.actionButton.setOnClickListener {
                        openAppSettings()
                    }
                }
                "not_installed" -> {
                    binding.actionButton.visibility = View.VISIBLE
                    binding.actionButton.text = getString(R.string.install_instagram)
                    binding.actionButton.setOnClickListener {
                        openPlayStore()
                    }
                }
                else -> {
                    binding.actionButton.visibility = View.GONE
                }
            }
        } else {
            // Hide block status banner
            binding.statusBanner.visibility = View.GONE
        }
    }
    
    private fun showDiagnostics() {
        val status = BlockChecker.checkInstagramBlock(this)
        
        val dialogMessage = buildString {
            appendLine("Instagram Block Diagnostics")
            appendLine("=" .repeat(30))
            appendLine()
            appendLine("Status: ${if (status.blocked) "BLOCKED" else "NOT BLOCKED"}")
            appendLine("Reason: ${status.reason}")
            appendLine()
            appendLine("Reason Codes:")
            appendLine("• user_setting: Blocked via app toggle")
            appendLine("• package_disabled: App is disabled")
            appendLine("• package_suspended: App is suspended")
            appendLine("• not_installed: Instagram not found")
            appendLine("• ok: Not blocked")
        }
        
        Log.d(TAG, "Diagnostics: $dialogMessage")
        
        AlertDialog.Builder(this)
            .setTitle("Block Diagnostics")
            .setMessage(dialogMessage)
            .setPositiveButton("OK", null)
            .setNeutralButton("View Logs") { _, _ ->
                Snackbar.make(
                    binding.root,
                    "Check Logcat for detailed logs (tag: BlockChecker)",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            .show()
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", "com.instagram.android", null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app settings: ${e.message}")
            Snackbar.make(binding.root, "Cannot open settings", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=com.instagram.android")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.instagram.android")
            }
            startActivity(webIntent)
        }
    }
}
