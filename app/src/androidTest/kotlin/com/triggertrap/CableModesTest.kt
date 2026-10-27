package com.triggertrap

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.triggertrap.TestSupport.assertTitle
import com.triggertrap.TestSupport.launch
import com.triggertrap.TestSupport.openMode
import com.triggertrap.TestSupport.pressDown
import com.triggertrap.TestSupport.release
import com.triggertrap.TestSupport.requireService
import com.triggertrap.TestSupport.string
import com.triggertrap.TestSupport.tapButton
import com.triggertrap.TestSupport.waitFor
import com.triggertrap.TestSupport.waitForUi
import com.triggertrap.activities.MainActivity
import com.triggertrap.service.TriggertrapService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The six cable release modes against the real service and audio output. */
@RunWith(AndroidJUnit4::class)
class CableModesTest {
    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Before
    fun setUp() {
        TestSupport.reset()
        TestSupport.app().timedModeTime = 1500
        TestSupport.app().selfTimedModeTime = 1000
    }

    @After
    fun tearDown() = TestSupport.reset()

    private fun action() = requireService().onGoingAction
    private fun state() = requireService().state

    @Test
    fun simpleCableReleaseFiresOnePulse() {
        launch().run {
            waitForUi { assertTitle(string(R.string.getting_started)) }
            openMode(R.string.simple_cable_release)
            tapButton(R.id.shutterButton)
            // a single pulse does not count as a running action
            assertEquals(TriggertrapService.State.IDLE, state())
            assertEquals(TTApp.OnGoingAction.NONE, action())
            tapButton(R.id.shutterButton)
            Thread.sleep(300)
            assertEquals(TriggertrapService.State.IDLE, state())
        }
    }

    @Test
    fun quickReleaseFiresWhenTheButtonIsReleased() {
        launch().run {
            openMode(R.string.quick_release)
            onView(withId(R.id.quickReleaseButton)).perform(pressDown())
            waitFor(what = "quick release armed") { action() == TTApp.OnGoingAction.QUICK_RELEASE }
            assertEquals(TriggertrapService.State.IN_PROGRESS, state())
            Thread.sleep(400)
            onView(withId(R.id.quickReleaseButton)).perform(release())
            waitFor(what = "quick release fired") { state() == TriggertrapService.State.IDLE }
            assertEquals(TTApp.OnGoingAction.NONE, action())
        }
    }

    @Test
    fun pressAndHoldKeepsTheShutterOpenWhilePressed() {
        launch().run {
            openMode(R.string.press_and_hold)
            onView(withId(R.id.pressHoldButton)).perform(pressDown())
            waitFor(what = "press and hold running") { action() == TTApp.OnGoingAction.PRESS_AND_HOLD }
            Thread.sleep(600)
            onView(withId(R.id.pressHoldButton)).perform(release())
            waitFor(what = "press and hold stopped") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun pressToLockTogglesTheShutter() {
        launch().run {
            openMode(R.string.press_to_lock)
            tapButton(R.id.pressStartButton)
            waitFor(what = "stopwatch running") { action() == TTApp.OnGoingAction.PRESS_START_STOP }
            Thread.sleep(700)
            // the running action survives a visit to another screen and shows in the status bar
            openMode(R.string.simple_cable_release)
            waitForUi { onView(withId(R.id.status)).check(matches(isDisplayed())) }
            openMode(R.string.press_to_lock)
            assertEquals(TTApp.OnGoingAction.PRESS_START_STOP, action())
            tapButton(R.id.pressStartButton)
            waitFor(what = "stopwatch stopped") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun timedReleaseRunsForTheSetTimeThenStops() {
        launch().run {
            openMode(R.string.timed_release)
            tapButton(R.id.timedButton)
            waitFor(what = "timed running") { action() == TTApp.OnGoingAction.TIMED }
            waitFor(timeoutMs = 10_000, what = "timed finished") { state() == TriggertrapService.State.IDLE }
            // start again and cancel by hand
            tapButton(R.id.timedButton)
            waitFor(what = "timed running again") { action() == TTApp.OnGoingAction.TIMED }
            tapButton(R.id.timedButton)
            waitFor(what = "timed cancelled") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun selfTimerCountsDownThenFires() {
        launch().run {
            openMode(R.string.self_timer)
            tapButton(R.id.timedButton)
            waitFor(what = "self timer running") { action() == TTApp.OnGoingAction.SELF_TIMER }
            waitFor(timeoutMs = 10_000, what = "self timer fired") { state() == TriggertrapService.State.IDLE }
            tapButton(R.id.timedButton)
            waitFor(what = "self timer running again") { action() == TTApp.OnGoingAction.SELF_TIMER }
            tapButton(R.id.timedButton)
            waitFor(what = "self timer cancelled") { state() == TriggertrapService.State.IDLE }
        }
    }

    @Test
    fun startingASecondModeWhileOneRunsAsksToStopIt() {
        launch().run {
            openMode(R.string.press_to_lock)
            tapButton(R.id.pressStartButton)
            waitFor(what = "stopwatch running") { action() == TTApp.OnGoingAction.PRESS_START_STOP }
            openMode(R.string.timed_release)
            tapButton(R.id.timedButton)
            // the "stop the running mode?" dialog: yes
            waitForUi { onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.yes)).check(matches(isDisplayed())) }
            onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.yes)).perform(androidx.test.espresso.action.ViewActions.click())
            waitFor(what = "stopwatch stopped by dialog") { state() == TriggertrapService.State.IDLE }
        }
    }
}
