package com.example.mobileappdev.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.mobileappdev.data.local.entity.StudentEntity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class RosterViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RosterViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new RosterViewModel();
    }

    @Test
    public void testFiltersAndSearch() {
        viewModel.setSearchQuery("Joyce");
        viewModel.setProgrammeFilter("CS");
        viewModel.setGroupFilter("G01");
        viewModel.setStatusFilter("Synced");

        assertEquals("Joyce", viewModel.getSearchQuery().getValue());
        assertEquals("CS", viewModel.getProgrammeFilter().getValue());
        assertEquals("G01", viewModel.getGroupFilter().getValue());
        assertEquals("Synced", viewModel.getStatusFilter().getValue());

        viewModel.clearAllFilters();
        assertEquals("", viewModel.getSearchQuery().getValue());
        assertEquals("", viewModel.getProgrammeFilter().getValue());
        assertEquals("", viewModel.getGroupFilter().getValue());
        assertEquals("", viewModel.getStatusFilter().getValue());
    }

    @Test
    public void testStudentsAndCount() {
        List<StudentEntity> list = new ArrayList<>();
        StudentEntity s = new StudentEntity();
        s.name = "Test";
        list.add(s);

        viewModel.setStudents(list);
        viewModel.setStudentsFound(1);

        assertNotNull(viewModel.getStudents().getValue());
        assertEquals(1, viewModel.getStudents().getValue().size());
        assertEquals(Integer.valueOf(1), viewModel.getStudentsFound().getValue());
    }
}
