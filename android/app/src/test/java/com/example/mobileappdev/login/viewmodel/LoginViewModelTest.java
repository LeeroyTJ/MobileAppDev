package com.example.mobileappdev.login.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new LoginViewModel();
    }

    @Test
    public void testStudentNumberAndPassUpdates() {
        viewModel.setStudentNumber("202401120");
        viewModel.setPassword("secret123");

        assertEquals("202401120", viewModel.getStudentNumber().getValue());
        assertEquals("secret123", viewModel.getPassword().getValue());
    }

    @Test
    public void testLoginStateFlow() {
        viewModel.setLoggingIn(true);
        assertTrue(viewModel.getIsLoggingIn().getValue());

        viewModel.setLoginSuccessful(true);
        assertTrue(viewModel.getLoginSuccessful().getValue());
    }
}
