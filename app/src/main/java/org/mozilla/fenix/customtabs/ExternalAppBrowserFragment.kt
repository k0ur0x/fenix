/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.customtabs

import android.view.Gravity
import android.view.View
import kotlinx.android.synthetic.main.component_search.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mozilla.components.browser.session.Session
import mozilla.components.feature.pwa.ext.trustedOrigins
import mozilla.components.feature.pwa.feature.WebAppHideToolbarFeature
import mozilla.components.feature.sitepermissions.SitePermissions
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BaseBrowserFragment
import org.mozilla.fenix.components.toolbar.BrowserToolbarController
import org.mozilla.fenix.components.toolbar.BrowserToolbarInteractor
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents

/**
 * Fragment used for browsing the web within external apps.
 */
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class ExternalAppBrowserFragment : BaseBrowserFragment(), BackHandler {

    private val customTabsIntegration = ViewBoundFeatureWrapper<CustomTabsIntegration>()
    private val hideToolbarFeature = ViewBoundFeatureWrapper<WebAppHideToolbarFeature>()

    override fun initializeUI(view: View): Session? {
        return super.initializeUI(view)?.also {
            val activity = requireActivity()
            val components = activity.components

            customTabSessionId?.let { customTabSessionId ->
                customTabsIntegration.set(
                    feature = CustomTabsIntegration(
                        requireComponents.core.sessionManager,
                        toolbar,
                        customTabSessionId,
                        activity,
                        view.nestedScrollQuickAction,
                        view.swipeRefresh,
                        onItemTapped = { browserInteractor.onBrowserToolbarMenuItemTapped(it) }
                    ),
                    owner = this,
                    view = view)

                hideToolbarFeature.set(
                    feature = WebAppHideToolbarFeature(
                        requireComponents.core.sessionManager,
                        toolbar,
                        customTabSessionId,
                        emptyList()
                    ) { toolbarVisible ->
                        updateLayoutMargins(inFullScreen = !toolbarVisible)
                    },
                    owner = this,
                    view = toolbar)
            }

            consumeFrom(browserStore) {
                browserToolbarView.update(it)
            }

            consumeFrom(components.core.customTabsStore) { state ->
                getSessionById()
                    ?.let { session -> session.customTabConfig?.sessionToken }
                    ?.let { token -> state.tabs[token] }
                    ?.let { tabState ->
                        hideToolbarFeature.withFeature {
                            it.onTrustedScopesChange(tabState.trustedOrigins)
                        }
                    }
            }

            updateLayoutMargins(false)
        }
    }

    override fun removeSessionIfNeeded(): Boolean {
        return customTabsIntegration.onBackPressed() || super.removeSessionIfNeeded()
    }

    override fun createBrowserToolbarViewInteractor(
        browserToolbarController: BrowserToolbarController,
        session: Session?
    ) = BrowserToolbarInteractor(browserToolbarController)

    override fun navToQuickSettingsSheet(session: Session, sitePermissions: SitePermissions?) {
        val directions = ExternalAppBrowserFragmentDirections
            .actionExternalAppBrowserFragmentToQuickSettingsSheetDialogFragment(
                sessionId = session.id,
                url = session.url,
                isSecured = session.securityInfo.secure,
                isTrackingProtectionOn = session.trackerBlockingEnabled,
                sitePermissions = sitePermissions,
                gravity = getAppropriateLayoutGravity()
            )
        nav(R.id.externalAppBrowserFragment, directions)
    }

    override fun navToTrackingProtectionPanel(session: Session) {
        val directions =
            ExternalAppBrowserFragmentDirections
                .actionExternalAppBrowserFragmentToTrackingProtectionPanelDialogFragment(
                sessionId = session.id,
                url = session.url,
                trackingProtectionEnabled = session.trackerBlockingEnabled,
                gravity = getAppropriateLayoutGravity()
            )
        nav(R.id.externalAppBrowserFragment, directions)
    }

    override fun getEngineMargins(): Pair<Int, Int> {
        val toolbarSize = resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)
        return toolbarSize to 0
    }

    override fun getAppropriateLayoutGravity() = Gravity.TOP
}
