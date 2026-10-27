package com.triggertrap

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.triggertrap.activities.MainActivity
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.triggertrap.service.TriggertrapService
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object TestSupport {
    val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** The activity under test; closed by [reset] after the running mode has been stopped. */
    var scenario: ActivityScenario<MainActivity>? = null

    /** Launch the main activity (the previous one, if any, is closed first). */
    fun launch(): ActivityScenario<MainActivity> {
        scenario?.close()
        return ActivityScenario.launch(MainActivity::class.java).also { scenario = it }
    }

    /**
     * Fresh preferences (the app singleton included), the service stopped, the activity closed,
     * no system dialog in the way. The mode is stopped before the activity is closed: closing
     * an activity while a mode runs waits for an idle main thread that a running mode never
     * provides on a slow emulator.
     */
    fun reset() {
        clearScreen()
        service()?.let { s ->
            // the service talks to the activity's views: main thread only
            onMain {
                s.stopCurrentAction()
                if (s.isWifiMasterOn) s.unRegsiterMaster()
                s.goTobackground()
            }
        }
        Thread.sleep(300)
        scenario?.let { s ->
            // a dialog left open by a failed test keeps the activity from finishing cleanly
            try {
                s.onActivity { a ->
                    a.supportFragmentManager.fragments.filterIsInstance<androidx.fragment.app.DialogFragment>()
                        .forEach { it.dismissAllowingStateLoss() }
                }
            } catch (e: Throwable) {
                Log.w("UITest", "dismissing dialogs: $e")
            }
            try {
                s.close()
            } catch (e: Throwable) {
                Log.w("UITest", "closing the activity: $e")
            }
        }
        scenario = null
        context.getSharedPreferences("triggertrap_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        // the settings screen defaults (pulse length etc.), as the first start writes them
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true)
        // TTApp caches every preference; drop the singleton so the next launch reads the cleared values
        TTApp::class.java.getDeclaredField("mInstance").apply { isAccessible = true }.set(null, null)
        Thread.sleep(50)
    }

    /** The app settings singleton, re-created from the (cleared) preferences. */
    fun app(): TTApp = TTApp.getInstance(context)

    /** Run on the main thread and wait; service callbacks update the views. */
    fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    /**
     * CI emulators sometimes boot with the keyguard, an ANR dialog or a permission prompt on top;
     * Espresso then never sees a focused window. Dismiss what is there and log the focus.
     */
    fun clearScreen() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            // the running-mode button animates forever (repeating ObjectAnimators): with
            // animations on, the main thread never idles and every onView() blocks
            for (setting in listOf("animator_duration_scale", "transition_animation_scale", "window_animation_scale")) {
                device.executeShellCommand("settings put global $setting 0")
            }
            device.executeShellCommand("wm dismiss-keyguard")
            device.wakeUp()
            for (label in listOf("Wait", "Close app", "OK", "Allow", "While using the app", "Got it")) {
                device.findObject(By.text(label))?.let { Log.i("UITest", "dismissing $label"); it.click() }
            }
            val focus = device.executeShellCommand("dumpsys window").lineSequence()
                .filter { "mCurrentFocus" in it }.joinToString(" | ") { it.trim() }
            Log.i("UITest", "focus before test: $focus")
        } catch (e: Exception) {
            Log.w("UITest", "clearScreen: $e")
        }
    }

    // ---- the service ---------------------------------------------------------------------

    private var bound: TriggertrapService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            bound = (binder as TriggertrapService.TiggertrapServiceBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = null
        }
    }

    /** The (local, in-process) service instance the activity talks to; bound once per test process. */
    fun service(): TriggertrapService? {
        if (bound == null) {
            val latch = CountDownLatch(1)
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    bound = (binder as TriggertrapService.TiggertrapServiceBinder).service
                    latch.countDown()
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    bound = null
                }
            }
            context.bindService(Intent(context, TriggertrapService::class.java), conn, Context.BIND_AUTO_CREATE)
            latch.await(10, TimeUnit.SECONDS)
        }
        return bound
    }

    fun requireService(): TriggertrapService = service() ?: throw AssertionError("service not bound")

    // ---- waiting ------------------------------------------------------------------------

    /** Retry an Espresso assertion until it passes; the UI follows the service asynchronously. */
    fun waitForUi(timeoutMs: Long = 15_000, assertion: () -> Unit) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            try {
                assertion(); return
            } catch (e: Throwable) {
                if (System.currentTimeMillis() > deadline) throw e
                Thread.sleep(200)
            }
        }
    }

    fun waitFor(timeoutMs: Long = 30_000, what: String = "condition", condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("timeout waiting for $what")
            Thread.sleep(100)
        }
    }

    // ---- touching the custom buttons ------------------------------------------------------

    /**
     * The round mode buttons (OngoingButton) react in onTouchEvent, not to click listeners, and
     * Espresso's injected touches become long presses on slow emulators. Dispatch the motion
     * events to the view directly instead; the view code under test is unchanged.
     */
    private fun touch(action: Int): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun getDescription() = "dispatch MotionEvent $action at the view centre"
        override fun perform(uiController: UiController, view: View) {
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(now, now, action, view.width / 2f, view.height / 2f, 0)
            view.dispatchTouchEvent(event)
            event.recycle()
            // no loopMainThreadUntilIdle() here: while a mode runs the service posts UI ticks and
            // looping the main thread from inside the action would never return
        }
    }

    fun pressDown(): ViewAction = touch(MotionEvent.ACTION_DOWN)
    fun release(): ViewAction = touch(MotionEvent.ACTION_UP)

    /**
     * A tap on a mode button: down and up in one action, because the down may open a dialog
     * (the "stop the running mode?" question) and a second onView() would then look at the
     * dialog window instead of the button.
     */
    private fun tap(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun getDescription() = "dispatch ACTION_DOWN and ACTION_UP at the view centre"
        override fun perform(uiController: UiController, view: View) {
            val down = SystemClock.uptimeMillis()
            val x = view.width / 2f
            val y = view.height / 2f
            MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0).also { view.dispatchTouchEvent(it); it.recycle() }
            MotionEvent.obtain(down, down + 60, MotionEvent.ACTION_UP, x, y, 0).also { view.dispatchTouchEvent(it); it.recycle() }
        }
    }

    fun tapButton(id: Int) {
        onView(withId(id)).perform(tap())
    }

    // ---- navigation -----------------------------------------------------------------------

    /** Open the drawer and tap the mode with this title; waits until the toolbar shows it. */
    fun openMode(title: String) {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onData(allOf(instanceOf(String::class.java), equalTo(title)))
            .inAdapterView(withId(R.id.left_drawer)).perform(click())
        waitForUi { onView(withId(R.id.drawer_layout)).check(matches(isDisplayed())) }
        waitForUi { assertTitle(title) }
    }

    fun openMode(titleRes: Int) = openMode(context.getString(titleRes))

    fun assertTitle(title: String) {
        onView(allOf(withParent(withId(R.id.toolbar)), withText(title))).check(matches(isDisplayed()))
    }

    fun string(res: Int): String = context.getString(res)
}
