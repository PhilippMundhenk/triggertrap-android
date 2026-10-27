package com.triggertrap

import android.Manifest
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.triggertrap.TestSupport.assertTitle
import com.triggertrap.TestSupport.launch
import com.triggertrap.TestSupport.onMain
import com.triggertrap.TestSupport.context
import com.triggertrap.TestSupport.openMode
import com.triggertrap.TestSupport.requireService
import com.triggertrap.TestSupport.string
import com.triggertrap.TestSupport.tapButton
import com.triggertrap.TestSupport.waitFor
import com.triggertrap.TestSupport.waitForUi
import com.triggertrap.activities.MainActivity
import com.triggertrap.service.TriggertrapService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sound sensor, distance lapse, the solar and ND calculators, Wi-Fi master and slave screens. */
@RunWith(AndroidJUnit4::class)
class SensorsAndCalculatorsTest {
    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @Before
    fun setUp() = TestSupport.reset()

    @After
    fun tearDown() = TestSupport.reset()

    private fun action() = requireService().onGoingAction
    private fun state() = requireService().state

    @Test
    fun soundSensorArmsAndDisarms() {
        launch().run {
            waitForUi { assertTitle(string(R.string.getting_started)) }
            openMode(R.string.sound_sensor)
            // the microphone is open while the screen is shown
            waitFor(what = "microphone open") { requireService().isSoundSensorOpen }
            tapButton(R.id.bangButton)
            waitFor(what = "sensor armed") { action() == TTApp.OnGoingAction.BANG }
            // moving the sensitivity slider while armed
            onView(withId(R.id.bangSensitivity)).perform(androidx.test.espresso.action.ViewActions.swipeRight())
            Thread.sleep(500)
            assertEquals(TTApp.OnGoingAction.BANG, action())
            // leave and come back: still armed, shown in the status bar elsewhere
            openMode(R.string.nd_calculator)
            waitForUi { onView(withId(R.id.status)).check(matches(isDisplayed())) }
            openMode(R.string.sound_sensor)
            tapButton(R.id.bangButton)
            waitFor(what = "sensor disarmed") { state() == TriggertrapService.State.IDLE }
            // a loud sample through the service fires one pulse
            onMain { requireService().onExceedThreshold(90) }
            Thread.sleep(300)
            assertEquals(TriggertrapService.State.IDLE, state())
            openMode(R.string.simple_cable_release)
            waitFor(what = "microphone closed") { !requireService().isSoundSensorOpen }
        }
    }

    @Test
    fun ndCalculatorShowsTheResult() {
        launch().run {
            openMode(R.string.nd_calculator)
            waitForUi {
                onView(withId(R.id.resultTextView)).check { view, _ ->
                    assertTrue((view as TextView).text.isNotEmpty())
                }
            }
            // 1/8000 behind one stop is 1/4000 (the picker lists third stops: 1/8000, 1/6400, 1/5000, 1/4000)
            val speeds = context.resources.getStringArray(R.array.base_shutter_speeds)
            onView(withId(R.id.resultTextView)).check(matches(withText(speeds[3])))
        }
    }

    @Test
    fun solarCalculatorShowsTimesForTheCurrentLocation() {
        launch().run {
            openMode(R.string.sunrise_sunset)
            waitForUi(20_000) {
                onView(withId(R.id.locationTextView)).check { view, _ ->
                    val text = (view as TextView).text.toString()
                    assertNotEquals(string(R.string.permission_location_needed), text)
                }
            }
            onView(withId(R.id.solarArcView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun distanceLapseCountsTheDistanceFromTheService() {
        launch().run {
            openMode(R.string.distancelapse)
            tapButton(R.id.distanceLapseButton)
            // without GPS on the emulator the app offers to open the location settings: decline
            try {
                waitForUi(3000) { onView(withText(R.string.no)).check(matches(isDisplayed())) }
                onView(withText(R.string.no)).perform(click())
                tapButton(R.id.distanceLapseButton)
            } catch (e: Throwable) {
                // GPS is enabled, the mode started directly
            }
            waitFor(what = "distance lapse running") { action() == TTApp.OnGoingAction.DISTANCE_LAPSE }
            // feed movement the way the location listener would: 25 m default trigger distance
            val service = requireService()
            onMain { service.onDistanceChanged(10f, 2f) }
            onMain { service.onDistanceChanged(20f, 2f) }
            waitForUi {
                onView(withId(R.id.distanceCovered)).check { view, _ ->
                    assertTrue((view as TextView).text.toString().startsWith("5"))
                }
            }
            tapButton(R.id.distanceLapseButton)
            waitFor(what = "distance lapse stopped") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun wifiSlaveWatchesForMastersAndWifiMasterToggles() {
        launch().run {
            openMode(R.string.wifi_slave)
            tapButton(R.id.slaveButton)
            waitFor(what = "watching for masters") { action() == TTApp.OnGoingAction.WI_FI_SLAVE }
            tapButton(R.id.slaveButton)
            waitFor(what = "stopped watching") { state() == TriggertrapService.State.IDLE }

            openMode(R.string.wifi_master)
            onView(withId(R.id.masterOnOff)).perform(click())
            waitFor(timeoutMs = 30_000, what = "master registered") { requireService().isWifiMasterOn }
            onView(withId(R.id.masterOnOff)).perform(click())
            waitFor(timeoutMs = 30_000, what = "master unregistered") { !requireService().isWifiMasterOn }
        }
    }
}
