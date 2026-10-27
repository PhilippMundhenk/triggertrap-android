package com.triggertrap

import android.Manifest
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.triggertrap.TestSupport.assertTitle
import com.triggertrap.TestSupport.launch
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Timelapse, TimeWarp, star trail, bramping and the two HDR modes, plus the background service. */
@RunWith(AndroidJUnit4::class)
class SequenceModesTest {
    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Before
    fun setUp() {
        TestSupport.reset()
        val app = TestSupport.app()
        app.timeLapseInterval = 1000
        app.starTrailIterations = 2
        app.starTrailExposure = 300
        app.starTrailGap = 200
        app.brampingIterations = 3
        app.brampingInterval = 700
        app.brampingStartExposure = 100
        app.brampingEndExposure = 300
        // the HDR middle exposure must be one of the picker values: the smallest one at or above 100 ms
        val speeds = context.resources.getIntArray(R.array.shutter_speed_values)
        app.hdrMiddleExposure = speeds.first { it >= 100 }.toLong()
        app.hdrNumExposures = 3
        app.hdrEvStep = 0.5f
        app.hdrTimeLapseMiddleExposure = speeds.first { it >= 100 }.toLong()
        app.hdrTimeLapseInterval = 3000
        app.hdrTimeLapseEvStep = 0.5f
        app.timeWarpIterations = 3
        app.timewarpDuration = 3000
    }

    @After
    fun tearDown() = TestSupport.reset()

    private fun action() = requireService().onGoingAction
    private fun state() = requireService().state

    @Test
    fun timelapseRunsInTheBackgroundWithANotificationAndStopsFromIt() {
        val scenario = launch()
        scenario.run {
            waitForUi { assertTitle(string(R.string.getting_started)) }
            openMode(R.string.timelapse)
            tapButton(R.id.timelapseButton)
            waitFor(what = "timelapse running") { action() == TTApp.OnGoingAction.TIMELAPSE }
            waitForUi(10_000) { onView(withId(R.id.timeLapseCount)).check(matches(androidx.test.espresso.matcher.ViewMatchers.withText("2"))) }

            // the app goes to the background: the service becomes a foreground service with a notification
            scenario.moveToState(Lifecycle.State.CREATED)
            waitFor(what = "foreground notification") {
                NotificationManagerCompat.from(context).activeNotifications.any { it.id == TriggertrapService.NOTIFICATION_ID }
            }
            Thread.sleep(2500)
            assertEquals(TTApp.OnGoingAction.TIMELAPSE, action())

            // back in front: the notification goes, the sequence continues and the count has grown
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitFor(what = "notification removed") {
                NotificationManagerCompat.from(context).activeNotifications.none { it.id == TriggertrapService.NOTIFICATION_ID }
            }
            assertEquals(TTApp.OnGoingAction.TIMELAPSE, action())
            waitForUi(10_000) {
                onView(withId(R.id.timeLapseCount)).check { view, _ ->
                    val count = (view as android.widget.TextView).text.toString().toInt()
                    assertTrue("count $count", count >= 4)
                }
            }

            // the Stop action of the notification sends ACTION_STOP to the service
            context.startService(Intent(context, TriggertrapService::class.java).setAction(TriggertrapService.ACTION_STOP))
            waitFor(what = "stopped via notification action") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun timelapseStopsFromTheStatusBarOnAnotherScreen() {
        launch().run {
            openMode(R.string.timelapse)
            tapButton(R.id.timelapseButton)
            waitFor(what = "timelapse running") { action() == TTApp.OnGoingAction.TIMELAPSE }
            openMode(R.string.simple_cable_release)
            waitForUi { onView(withId(R.id.status)).check(matches(isDisplayed())) }
            onView(withId(R.id.stopRunningAction)).perform(androidx.test.espresso.action.ViewActions.click())
            waitFor(what = "stopped from status bar") { state() == TriggertrapService.State.IDLE }
            // back on the timelapse screen the button is idle again and a new run can start and stop
            openMode(R.string.timelapse)
            tapButton(R.id.timelapseButton)
            waitFor(what = "timelapse running again") { action() == TTApp.OnGoingAction.TIMELAPSE }
            tapButton(R.id.timelapseButton)
            waitFor(what = "timelapse stopped") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun starTrailFinishesAfterItsExposures() {
        launch().run {
            openMode(R.string.star_trail)
            tapButton(R.id.starTrailButton)
            waitFor(what = "star trail running") { action() == TTApp.OnGoingAction.STAR_TRAIL }
            // 2 x (300 + 200) ms
            waitFor(timeoutMs = 15_000, what = "star trail finished") { state() == TriggertrapService.State.IDLE }
            tapButton(R.id.starTrailButton)
            waitFor(what = "star trail running again") { action() == TTApp.OnGoingAction.STAR_TRAIL }
            tapButton(R.id.starTrailButton)
            waitFor(what = "star trail cancelled") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun brampingFinishesAfterItsExposures() {
        launch().run {
            openMode(R.string.bramping)
            tapButton(R.id.brampingButton)
            waitFor(what = "bramping running") { action() == TTApp.OnGoingAction.BRAMPING }
            waitFor(timeoutMs = 15_000, what = "bramping finished") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun hdrBracketFinishes() {
        launch().run {
            openMode(R.string.le_hdr)
            tapButton(R.id.hdrButton)
            waitFor(what = "hdr running") { action() == TTApp.OnGoingAction.HDR }
            waitFor(timeoutMs = 20_000, what = "hdr finished") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun hdrTimelapseRepeatsUntilStopped() {
        launch().run {
            openMode(R.string.le_hdr_timelapse)
            tapButton(R.id.hdrButton)
            waitFor(what = "hdr timelapse running") { action() == TTApp.OnGoingAction.HDR_TIMELAPSE }
            Thread.sleep(7000)
            assertEquals(TTApp.OnGoingAction.HDR_TIMELAPSE, action())
            tapButton(R.id.hdrButton)
            waitFor(what = "hdr timelapse stopped") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun timeWarpRunsAndStops() {
        launch().run {
            openMode(R.string.timewarp)
            tapButton(R.id.timeWarpButton)
            waitFor(what = "timewarp running") { action() == TTApp.OnGoingAction.TIMEWARP }
            waitFor(timeoutMs = 20_000, what = "timewarp finished") { state() == TriggertrapService.State.IDLE }
        }
    }
}
