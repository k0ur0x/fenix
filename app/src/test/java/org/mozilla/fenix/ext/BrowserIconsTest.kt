package org.mozilla.fenix.ext

import android.widget.ImageView
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mozilla.components.browser.engine.gecko.fetch.GeckoViewFetchClient
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest
import mozilla.components.support.test.robolectric.testContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ObsoleteCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)

class BrowserIconsTest {
    @Test
    fun loadIntoViewTest() {
        val imageView = spyk(ImageView(testContext))
        val icons = spyk(BrowserIcons(testContext, httpClient = GeckoViewFetchClient(testContext)))
        val myUrl = "https://mozilla.com"
        val request = spyk(IconRequest(url = myUrl))
        icons.loadIntoView(imageView, myUrl)
        verify { icons.loadIntoView(imageView, request) }
    }
}
