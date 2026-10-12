package com.example.mobileappdev;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.mobileappdev.session.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RotationTest {

    @Before
    public void signOut() {
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).clearSession();
    }

    @Test
    public void registrationName_survivesScreenRecreation() {
        try (ActivityScenario<RegistrationActivity> scenario = ActivityScenario.launch(RegistrationActivity.class)) {
            onView(withId(R.id.etFullName)).perform(typeText("Rotation Test Name"), closeSoftKeyboard());
            scenario.recreate(); // Simulates screen rotation / configuration change
            onView(withId(R.id.etFullName)).check(matches(withText("Rotation Test Name")));
        }
    }
}
