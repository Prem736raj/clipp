package com.example.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

enum class SubscriptionTier(val title: String) {
    FREE("Free"),
    PRO("Pro"),
    PRO_PLUS("Pro+")
}

data class SubscriptionDetails(
    val tier: SubscriptionTier,
    val isAutoRenewing: Boolean = false,
    val renewalDate: String = "",
    val paymentMethod: String = "",
    val isGracePeriod: Boolean = false,
    val isHold: Boolean = false,
    val isExpired: Boolean = false
)

class BillingManager(private val context: Context) : PurchasesUpdatedListener {
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _currentTier = MutableStateFlow(SubscriptionTier.FREE)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    private val _subscriptionDetails = MutableStateFlow<SubscriptionDetails?>(null)
    val subscriptionDetails: StateFlow<SubscriptionDetails?> = _subscriptionDetails.asStateFlow()

    init {
        // In a real app we would query purchases here.
        // For demonstration, we check a mock shared prefs.
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        val savedTier = prefs.getString("tier", "FREE") ?: "FREE"
        
        // Mock checking expiration for Grace Period logic.
        val expired = prefs.getBoolean("is_expired", false)
        val autoRenewing = prefs.getBoolean("auto_renew", false)
        val renewalDate = prefs.getString("renewal_date", "2026-07-12") ?: "2026-07-12"
        
        if (savedTier != "FREE") {
            _subscriptionDetails.value = SubscriptionDetails(
                tier = SubscriptionTier.valueOf(savedTier),
                isAutoRenewing = autoRenewing,
                renewalDate = renewalDate,
                paymentMethod = "Visa ending in 4242",
                isGracePeriod = expired,
                isExpired = expired
            )
        }
        
        _currentTier.value = try { 
            SubscriptionTier.valueOf(savedTier) 
        } catch (e: Exception) { 
            SubscriptionTier.FREE 
        }
        
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Ready to query products
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
            }
        })
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE || billingResult.responseCode == BillingClient.BillingResponseCode.ERROR) {
            android.widget.Toast.makeText(context, "Cannot connect to Google Play. Please check your connection.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            setTier(SubscriptionTier.PRO)
            val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("past_purchase", SubscriptionTier.PRO.name)
                .putBoolean("auto_renew", true)
                .putBoolean("is_expired", false)
                .apply()
                
             _subscriptionDetails.value = SubscriptionDetails(
                tier = SubscriptionTier.PRO,
                isAutoRenewing = true,
                renewalDate = "2026-07-12",
                paymentMethod = "Visa ending in 4242",
                isGracePeriod = false,
                isExpired = false
            )
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            android.widget.Toast.makeText(context, "Purchase is pending. Pro will unlock once payment completes.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    fun restorePurchases(activity: Activity) {
        if (!billingClient.isReady) {
            android.widget.Toast.makeText(activity, "Cannot connect to Google Play. Please check your connection.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        
        // Simulate checking if there are old purchases on Google Play, even if app data is cleared.
        android.widget.Toast.makeText(activity, "Restoring purchases from Google Play...", android.widget.Toast.LENGTH_SHORT).show()
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        val restoredTier = prefs.getString("past_purchase", "FREE")
        val isHold = prefs.getBoolean("is_hold", false)
        
        if (isHold) {
            android.widget.Toast.makeText(activity, "Account is on hold due to payment issue. Please update payment method.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        
        if (restoredTier != "FREE") {
            setTier(SubscriptionTier.valueOf(restoredTier ?: "FREE"))
            android.widget.Toast.makeText(activity, "${restoredTier} was restored successfully.", android.widget.Toast.LENGTH_LONG).show()
        } else {
            android.widget.Toast.makeText(activity, "No active subscriptions found on this account.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    fun manageSubscriptionInPlayStore(activity: Activity) {
        // Google Play Subscription URL format
        val url = "https://play.google.com/store/account/subscriptions"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        activity.startActivity(intent)
    }

    fun cancelSubscription(activity: Activity) {
        // In real app, they do this via Google Play but we can mock acknowledging it visually
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_renew", false).apply()
        
        _subscriptionDetails.value = _subscriptionDetails.value?.copy(isAutoRenewing = false)
        
        // Let user know they keep Pro until the end date
        android.widget.Toast.makeText(
            activity, 
            "Subscription canceled. You will keep ${_currentTier.value.title} access until the current billing period ends.", 
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    fun simulateSubscriptionExpiration() {
        // Mock method to let us test Downgrading logic easily.
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_expired", true).apply()
        
        _subscriptionDetails.value = _subscriptionDetails.value?.copy(isExpired = true, isGracePeriod = true, isAutoRenewing = false)
        _currentTier.value = SubscriptionTier.FREE // Current features locked, but old projects logic kicks in where `isGracePeriod = true`
    }

    fun trackProFeatureTry(featureName: String) {
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        val tries = prefs.getInt("try_$featureName", 0)
        prefs.edit().putInt("try_$featureName", tries + 1).apply()
    }

    // AI Daily Limiter
    private val MAX_AI_DAILY = 5

    fun canUseAIFeature(): Boolean {
        if (_currentTier.value != SubscriptionTier.FREE) return true
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDay = prefs.getString("ai_last_day", "")
        if (today != lastDay) return true // reset
        val used = prefs.getInt("ai_used_today", 0)
        return used < MAX_AI_DAILY
    }

    fun useAIFeature() {
        if (_currentTier.value != SubscriptionTier.FREE) return
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDay = prefs.getString("ai_last_day", "")
        val used = if (today != lastDay) 0 else prefs.getInt("ai_used_today", 0)
        prefs.edit()
            .putString("ai_last_day", today)
            .putInt("ai_used_today", used + 1)
            .apply()
    }

    fun getAIUsageString(): String {
        if (_currentTier.value != SubscriptionTier.FREE) return "Unlimited"
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDay = prefs.getString("ai_last_day", "")
        val used = if (today != lastDay) 0 else prefs.getInt("ai_used_today", 0)
        return "$used/$MAX_AI_DAILY AI features used today"
    }

    fun launchBillingFlow(activity: Activity, tier: SubscriptionTier, monthly: Boolean) {
        // For the sake of this prototype, we'll simulate the success since we don't have real SKUs.
        setTier(tier)
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("past_purchase", tier.name)
            .putBoolean("auto_renew", true)
            .putBoolean("is_expired", false)
            .apply()
        
        _subscriptionDetails.value = SubscriptionDetails(
            tier = tier,
            isAutoRenewing = true,
            renewalDate = "2026-07-12",
            paymentMethod = "Visa ending in 4242",
            isGracePeriod = false,
            isExpired = false
        )
            
        android.widget.Toast.makeText(activity, "Welcome to $tier!", android.widget.Toast.LENGTH_LONG).show()
    }

    private fun setTier(tier: SubscriptionTier) {
        _currentTier.value = tier
        val prefs = context.getSharedPreferences("clipp_billing", Context.MODE_PRIVATE)
        prefs.edit().putString("tier", tier.name).apply()
    }
}
