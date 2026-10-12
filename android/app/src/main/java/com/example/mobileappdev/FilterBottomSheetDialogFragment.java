package com.example.mobileappdev;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobileappdev.viewmodel.RosterViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class FilterBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "FilterBottomSheet";

    private RosterViewModel viewModel;

    private MaterialAutoCompleteTextView dropdownProgramme;
    private MaterialAutoCompleteTextView dropdownGroup;
    private MaterialAutoCompleteTextView dropdownStatus;
    private TextView filterResultCount;

    private String selectedProgramme = "";
    private String selectedGroup = "";
    private String selectedStatus = "";

    public static FilterBottomSheetDialogFragment newInstance() {
        return new FilterBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(RosterViewModel.class);

        MaterialButton btnClear = view.findViewById(R.id.sheet_button_clear);
        MaterialButton btnApply = view.findViewById(R.id.sheet_button_apply);
        dropdownProgramme = view.findViewById(R.id.dropdown_programme);
        dropdownGroup = view.findViewById(R.id.dropdown_group);
        dropdownStatus = view.findViewById(R.id.dropdown_status);
        filterResultCount = view.findViewById(R.id.filter_result_count);

        setupDropdowns();
        restoreCurrentSelections();
        observeViewModel();

        btnClear.setOnClickListener(v -> clearFilters());
        btnApply.setOnClickListener(v -> applyFiltersAndDismiss());
    }

    private void setupDropdowns() {
        String[] programmeOptions = getResources().getStringArray(R.array.filter_programme_options);
        ArrayAdapter<String> programmeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, programmeOptions);
        dropdownProgramme.setAdapter(programmeAdapter);
        dropdownProgramme.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                selectedProgramme = "";
            } else {
                selectedProgramme = programmeOptions[position];
            }
            updateResultPreview();
        });

        String[] groupOptions = getResources().getStringArray(R.array.filter_group_options);
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, groupOptions);
        dropdownGroup.setAdapter(groupAdapter);
        dropdownGroup.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                selectedGroup = "";
            } else {
                selectedGroup = groupOptions[position];
            }
            updateResultPreview();
        });

        String[] statusOptions = getResources().getStringArray(R.array.filter_status_options);
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, statusOptions);
        dropdownStatus.setAdapter(statusAdapter);
        dropdownStatus.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                selectedStatus = "";
            } else {
                selectedStatus = statusOptions[position];
            }
            updateResultPreview();
        });
    }

    private void restoreCurrentSelections() {
        String currentProg = viewModel.getProgrammeFilter().getValue();
        String currentGroup = viewModel.getGroupFilter().getValue();
        String currentStatus = viewModel.getStatusFilter().getValue();

        selectedProgramme = currentProg != null ? currentProg : "";
        selectedGroup = currentGroup != null ? currentGroup : "";
        selectedStatus = currentStatus != null ? currentStatus : "";

        dropdownProgramme.setText(TextUtils.isEmpty(selectedProgramme) ? getString(R.string.filter_all) : selectedProgramme, false);
        dropdownGroup.setText(TextUtils.isEmpty(selectedGroup) ? getString(R.string.filter_all) : selectedGroup, false);
        dropdownStatus.setText(TextUtils.isEmpty(selectedStatus) ? getString(R.string.filter_all) : selectedStatus, false);
    }

    private void observeViewModel() {
        viewModel.getStudentsFound().observe(getViewLifecycleOwner(), count -> {
            int studentCount = count != null ? count : 0;
            filterResultCount.setText(getResources().getQuantityString(R.plurals.filter_result_count, studentCount, studentCount));
        });
    }

    private void updateResultPreview() {
        viewModel.setProgrammeFilter(selectedProgramme);
        viewModel.setGroupFilter(selectedGroup);
        viewModel.setStatusFilter(selectedStatus);
    }

    private void clearFilters() {
        selectedProgramme = "";
        selectedGroup = "";
        selectedStatus = "";

        dropdownProgramme.setText(getString(R.string.filter_all), false);
        dropdownGroup.setText(getString(R.string.filter_all), false);
        dropdownStatus.setText(getString(R.string.filter_all), false);

        viewModel.clearAllFilters();
    }

    private void applyFiltersAndDismiss() {
        viewModel.setProgrammeFilter(selectedProgramme);
        viewModel.setGroupFilter(selectedGroup);
        viewModel.setStatusFilter(selectedStatus);
        dismiss();
    }
}
