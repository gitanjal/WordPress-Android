@file:JvmName("ZendeskHelper")

package org.wordpress.android.support

import android.content.Context
import com.zendesk.sdk.feedback.BaseZendeskFeedbackConfiguration
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity
import com.zendesk.sdk.model.access.AnonymousIdentity
import com.zendesk.sdk.model.access.Identity
import com.zendesk.sdk.model.request.CustomField
import com.zendesk.sdk.network.impl.ZendeskConfig
import com.zendesk.sdk.support.SupportActivity
import com.zendesk.sdk.util.NetworkUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PackageUtils
import org.wordpress.android.util.logInformation
import java.util.Locale

private val zendeskInstance: ZendeskConfig
    get() = ZendeskConfig.INSTANCE

val isZendeskEnabled: Boolean
    get() = zendeskInstance.isInitialized

private const val zendeskNeedsToBeEnabledError = "Zendesk needs to be setup before this method can be called"

fun setupZendesk(
    context: Context,
    zendeskUrl: String,
    applicationId: String,
    oauthClientId: String,
    deviceLocale: Locale
) {
    require(!isZendeskEnabled) {
        "Zendesk shouldn't be initialized more than once!"
    }
    if (zendeskUrl.isEmpty() || applicationId.isEmpty() || oauthClientId.isEmpty()) {
        return
    }
    zendeskInstance.init(context, zendeskUrl, applicationId, oauthClientId)
    updateZendeskDeviceLocale(deviceLocale)
}

// TODO("Make sure changing the language of the app updates the locale for Zendesk")
fun updateZendeskDeviceLocale(deviceLocale: Locale) {
    require(isZendeskEnabled) {
        zendeskNeedsToBeEnabledError
    }
    zendeskInstance.setDeviceLocale(deviceLocale)
}

fun showZendeskHelpCenter(context: Context, email: String, name: String) {
    require(isZendeskEnabled) {
        zendeskNeedsToBeEnabledError
    }
    zendeskInstance.setIdentity(zendeskIdentity(email, name))
    SupportActivity.Builder()
            .withArticlesForCategoryIds(ZendeskConstants.mobileCategoryId)
            .withLabelNames(ZendeskConstants.articleLabel)
            .show(context)
}

fun createAndShowRequest(
    context: Context,
    email: String,
    name: String,
    allSites: List<SiteModel>,
    username: String?
) {
    require(isZendeskEnabled) {
        zendeskNeedsToBeEnabledError
    }
    zendeskInstance.setIdentity(zendeskIdentity(email, name))
    zendeskInstance.ticketFormId = TicketFieldIds.form
    zendeskInstance.customFields = listOf(
            CustomField(TicketFieldIds.appVersion, PackageUtils.getVersionCode(context).toString()),
            CustomField(TicketFieldIds.blogList, blogInformation(allSites, username)),
            CustomField(TicketFieldIds.networkInformation, NetworkUtils.getActiveNetworkInfo(context).toString()),
            CustomField(TicketFieldIds.logs, AppLog.toPlainText(context))
    )
    val configuration = object : BaseZendeskFeedbackConfiguration() {
        override fun getRequestSubject(): String {
            return ZendeskConstants.ticketSubject
        }

        // TODO("implement tags")
    }
    ContactZendeskActivity.startActivity(context, configuration)
}

// Helpers

private fun zendeskIdentity(email: String, name: String): Identity =
        AnonymousIdentity.Builder().withEmailIdentifier(email).withNameIdentifier(name).build()

private fun blogInformation(allSites: List<SiteModel>, username: String?): String {
    return allSites.joinToString(separator = ZendeskConstants.blogSeparator) { it.logInformation(username) }
}

private object ZendeskConstants {
    const val mobileCategoryId = 360000041586
    const val articleLabel = "Android"
    const val ticketSubject = "WordPress for Android Support"
    const val blogSeparator = "\n----------\n"
}

private object TicketFieldIds {
    const val form = 360000010286L
    const val appVersion = 360000086866L
    const val blogList = 360000087183L
    const val deviceFreeSpace = 360000089123L // TODO("implement free space")
    const val networkInformation = 360000086966L
    const val logs = 22871957L
}
