package com.example.mobileappdev.viewmodel;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ProfileViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ProfileViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new ProfileViewModel();
    }

    @Test
    public void testProfileFields() {
        viewModel.setStudentName("Joyce Gondwe");
        viewModel.setStudentNumber("202410082");
        viewModel.setProgrammeCode("CS");
        viewModel.setLabGroupCode("G01");

        assertEquals("Joyce Gondwe", viewModel.getStudentName().getValue());
        assertEquals("202410082", viewModel.getStudentNumber().getValue());
        assertEquals("CS", viewModel.getProgrammeCode().getValue());
        assertEquals("G01", viewModel.getLabGroupCode().getValue());
    }
}
