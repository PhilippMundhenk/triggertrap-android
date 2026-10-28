package com.triggertrap

import android.Manifest
import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.triggertrap.TestSupport.assertTitle
import com.triggertrap.TestSupport.launch
import com.triggertrap.TestSupport.context
import com.triggertrap.TestSupport.openMode
import com.triggertrap.TestSupport.requireService
import com.triggertrap.TestSupport.waitFor
import com.triggertrap.TestSupport.string
import com.triggertrap.TestSupport.waitForUi
import com.triggertrap.activities.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every drawer entry opens its screen, the settings screen works, the last screen is remembered. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @Before
    fun setUp() = TestSupport.reset()

    @After
    fun tearDown() = TestSupport.reset()

    @Test
    fun firstStartShowsGettingStartedAndEveryModeOpens() {
        launch().run {
            waitForUi { assertTitle(string(R.string.getting_started)) }
            // the guide pages
            onView(withId(R.id.pager)).perform(swipeLeft())
            onView(withId(R.id.pager)).perform(swipeLeft())
            onView(withId(R.id.pager)).perform(swipeLeft())

            for (title in listOf(
                R.string.cable_selector, R.string.simple_cable_release, R.string.quick_release,
                R.string.press_and_hold, R.string.press_to_lock, R.string.timed_release, R.string.self_timer,
                R.string.timelapse, R.string.timewarp, R.string.distancelapse, R.string.star_trail, R.string.bramping,
                R.string.sound_sensor, R.string.le_hdr, R.string.le_hdr_timelapse, R.string.wifi_slave,
                R.string.sunrise_sunset, R.string.nd_calculator, R.string.wifi_master,
            )) {
                openMode(title)
            }
            // the cable selector names the cable for the chosen camera instead of linking to the shop
            openMode(R.string.cable_selector)
            waitForUi { onView(withId(R.id.buy_cable_btn)).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun settingsScreenChangesThePulseLength() {
        launch().run {
            waitForUi { assertTitle(string(R.string.getting_started)) }
            openActionBarOverflowOrOptionsMenu(context)
            onView(withText(R.string.action_settings)).perform(click())
            waitForUi { onView(withText(R.string.trigger_pulse_length)).check(matches(isDisplayed())) }
            val labels = context.resources.getStringArray(R.array.pref_shutter_setting_labels)
            val values = context.resources.getStringArray(R.array.pref_shutter_setting_values)
            onView(withText(R.string.trigger_pulse_length)).perform(click())
            onView(withText(labels[labels.size - 1])).perform(click())
            waitForUi { onView(withText(labels[labels.size - 1])).check(matches(isDisplayed())) }
            assertEquals(values[values.size - 1].toLong(), TestSupport.app().beepLength)
            // the other list preferences open and close
            for (title in listOf(R.string.sensor_delay, R.string.sensor_reset_delay, R.string.speed_unit, R.string.distance_unit_s_)) {
                onView(withText(title)).perform(click())
                pressBack()
            }
            scrollSettingsTo(R.string.version)
            onView(withText(R.string.version)).check(matches(isDisplayed()))
            pressBack()
            waitForUi { assertTitle(string(R.string.getting_started)) }
        }
    }

    /** The switch streams silence between pulses (for Bluetooth receivers) and stops when switched off again. */
    @Test
    fun keepAudioStreamOpenSettingStreamsSilenceBetweenPulses() {
        launch().run {
            waitForUi { assertTitle(string(R.string.getting_started)) }
            val beeper = requireService().outputDispatcher.audioBeeper
            assertFalse(beeper.isKeepingStreamOpen)
            val idle = beeper.silenceBlocksWritten
            openActionBarOverflowOrOptionsMenu(context)
            onView(withText(R.string.action_settings)).perform(click())
            waitForUi { scrollSettingsTo(R.string.keep_audio_stream_open) }
            onView(withText(R.string.keep_audio_stream_open)).perform(click())
            waitFor(what = "stream kept open") { beeper.isKeepingStreamOpen }
            waitFor(what = "silence streamed") { beeper.silenceBlocksWritten > idle + 3 }
            onView(withText(R.string.keep_audio_stream_open)).perform(click())
            waitFor(what = "stream released") { !beeper.isKeepingStreamOpen }
            SystemClock.sleep(200) // the block in flight
            val after = beeper.silenceBlocksWritten
            SystemClock.sleep(500)
            assertEquals(after, beeper.silenceBlocksWritten)
            pressBack()
        }
    }

    /** The settings screen is a RecyclerView: bring the row with [title] into view. */
    private fun scrollSettingsTo(title: Int) {
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.scrollTo<androidx.recyclerview.widget.RecyclerView.ViewHolder>(hasDescendant(withText(title))))
    }

    @Test
    fun lastScreenIsRememberedAcrossLaunches() {
        launch().run {
            waitForUi { assertTitle(string(R.string.getting_started)) }
            openMode(R.string.timelapse)
        }
        launch().run {
            waitForUi { assertTitle(string(R.string.timelapse)) }
        }
    }
}
