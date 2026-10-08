package com.example.mobileappdev.filter;

import android.content.Context;
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

import com.example.mobileappdev.R;
import com.example.mobileappdev.viewmodel.RosterViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

/**
 * FilterBottomSheetDialogFragment
 * <p>
 * Implements advanced roster filtering for Programme, Lab Group, and Sync Status.
 * Inflates {@code bottom_sheet_filters.xml} and connects directly with {@link RosterViewModel}.
 * Also supports optional {@link FilterListener} callback and Fragment Result API.
 * </p>
 *
 * Authored for CohortHub Roster Management.
 */
public class FilterBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "FilterBottomSheetDialogFragment";
    public static final String REQUEST_KEY = "filter_request_key";
    public static final String EXTRA_PROGRAMME = "extra_programme";
    public static final String EXTRA_GROUP = "extra_group";
    public static final String EXTRA_STATUS = "extra_status";

    public interface FilterListener {
        void onFiltersApplied(String programme, String group, String status);
        void onFiltersCleared();
    }

    private RosterViewModel rosterViewModel;
    private FilterListener filterListener;

    private MaterialButton buttonClear;
    private MaterialButton buttonApply;

    private MaterialAutoCompleteTextView dropdownProgramme;
    private MaterialAutoCompleteTextView dropdownGroup;
    private MaterialAutoCompleteTextView dropdownStatus;

    private TextView resultCountText;

    private String[] programmeOptions;
    private String[] groupOptions;
    private String[] statusOptions;

    private int selectedProgrammeIndex = 0;
    private int selectedGroupIndex = 0;
    private int selectedStatusIndex = 0;

    /**
     * Factory method to create a new instance of {@link FilterBottomSheetDialogFragment}.
     *
     * @return A new instance of FilterBottomSheetDialogFragment.
     */
    @NonNull
    public static FilterBottomSheetDialogFragment newInstance() {
        return new FilterBottomSheetDialogFragment();
    }

    /**
     * Factory method to create a new instance with initial filter parameters.
     */
    @NonNull
    public static FilterBottomSheetDialogFragment newInstance(@Nullable String programme,
                                                               @Nullable String group,
                                                               @Nullable String status) {
        FilterBottomSheetDialogFragment fragment = new FilterBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_PROGRAMME, programme);
        args.putString(EXTRA_GROUP, group);
        args.putString(EXTRA_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FilterListener) {
            filterListener = (FilterListener) context;
        } else if (getParentFragment() instanceof FilterListener) {
            filterListener = (FilterListener) getParentFragment();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        filterListener = null;
    }

    public void setFilterListener(@Nullable FilterListener listener) {
        this.filterListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadOptions();
        setupViewModel();
        setupDropdownAdapters();
        restoreInitialSelections();
        setupListeners();
        observeViewModel();
    }

    private void initViews(@NonNull View view) {
        buttonClear = view.findViewById(R.id.sheet_button_clear);
        buttonApply = view.findViewById(R.id.sheet_button_apply);

        dropdownProgramme = view.findViewById(R.id.dropdown_programme);
        dropdownGroup = view.findViewById(R.id.dropdown_group);
        dropdownStatus = view.findViewById(R.id.dropdown_status);

        resultCountText = view.findViewById(R.id.filter_result_count);
    }

    private void loadOptions() {
        programmeOptions = getResources().getStringArray(R.array.filter_programme_options);
        groupOptions = getResources().getStringArray(R.array.filter_group_options);
        statusOptions = getResources().getStringArray(R.array.filter_status_options);
    }

    private void setupViewModel() {
        if (getActivity() != null) {
            rosterViewModel = new ViewModelProvider(requireActivity()).get(RosterViewModel.class);
        }
    }

    private void setupDropdownAdapters() {
        if (getContext() == null) return;

        ArrayAdapter<String> programmeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                programmeOptions
        );
        dropdownProgramme.setAdapter(programmeAdapter);

        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                groupOptions
        );
        dropdownGroup.setAdapter(groupAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                statusOptions
        );
        dropdownStatus.setAdapter(statusAdapter);
    }

    private void restoreInitialSelections() {
        String initialProgramme = "";
        String initialGroup = "";
        String initialStatus = "";

        if (getArguments() != null) {
            initialProgramme = getArguments().getString(EXTRA_PROGRAMME, "");
            initialGroup = getArguments().getString(EXTRA_GROUP, "");
            initialStatus = getArguments().getString(EXTRA_STATUS, "");
        }

        if (rosterViewModel != null) {
            if (rosterViewModel.getProgrammeFilter().getValue() != null) {
                initialProgramme = rosterViewModel.getProgrammeFilter().getValue();
            }
            if (rosterViewModel.getGroupFilter().getValue() != null) {
                initialGroup = rosterViewModel.getGroupFilter().getValue();
            }
            if (rosterViewModel.getStatusFilter().getValue() != null) {
                initialStatus = rosterViewModel.getStatusFilter().getValue();
            }
        }

        selectedProgrammeIndex = findIndexForValue(programmeOptions, initialProgramme);
        selectedGroupIndex = findIndexForValue(groupOptions, initialGroup);
        selectedStatusIndex = findIndexForValue(statusOptions, initialStatus);

        setDropdownSelection(dropdownProgramme, programmeOptions, selectedProgrammeIndex);
        setDropdownSelection(dropdownGroup, groupOptions, selectedGroupIndex);
        setDropdownSelection(dropdownStatus, statusOptions, selectedStatusIndex);
    }

    private void setupListeners() {
        dropdownProgramme.setOnItemClickListener((parent, view, position, id) -> selectedProgrammeIndex = position);
        dropdownGroup.setOnItemClickListener((parent, view, position, id) -> selectedGroupIndex = position);
        dropdownStatus.setOnItemClickListener((parent, view, position, id) -> selectedStatusIndex = position);

        buttonClear.setOnClickListener(v -> clearAllFilters());
        buttonApply.setOnClickListener(v -> applyFilters());
    }

    private void observeViewModel() {
        if (rosterViewModel == null) return;

        rosterViewModel.getStudentsFound().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                updateResultCount(count);
            }
        });
    }

    private void updateResultCount(int count) {
        if (resultCountText != null) {
            String countText = getResources().getQuantityString(
                    R.plurals.filter_result_count,
                    count,
                    count
            );
            resultCountText.setText(countText);
        }
    }

    private void clearAllFilters() {
        selectedProgrammeIndex = 0;
        selectedGroupIndex = 0;
        selectedStatusIndex = 0;

        setDropdownSelection(dropdownProgramme, programmeOptions, 0);
        setDropdownSelection(dropdownGroup, groupOptions, 0);
        setDropdownSelection(dropdownStatus, statusOptions, 0);

        if (rosterViewModel != null) {
            rosterViewModel.setProgrammeFilter("");
            rosterViewModel.setGroupFilter("");
            rosterViewModel.setStatusFilter("");
        }

        if (filterListener != null) {
            filterListener.onFiltersCleared();
        }
    }

    private void applyFilters() {
        String selectedProgramme = getSelectedValue(programmeOptions, selectedProgrammeIndex);
        String selectedGroup = getSelectedValue(groupOptions, selectedGroupIndex);
        String selectedStatus = getSelectedValue(statusOptions, selectedStatusIndex);

        if (rosterViewModel != null) {
            rosterViewModel.setProgrammeFilter(selectedProgramme);
            rosterViewModel.setGroupFilter(selectedGroup);
            rosterViewModel.setStatusFilter(selectedStatus);
        }

        if (filterListener != null) {
            filterListener.onFiltersApplied(selectedProgramme, selectedGroup, selectedStatus);
        }

        Bundle result = new Bundle();
        result.putString(EXTRA_PROGRAMME, selectedProgramme);
        result.putString(EXTRA_GROUP, selectedGroup);
        result.putString(EXTRA_STATUS, selectedStatus);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);

        dismiss();
    }

    private void setDropdownSelection(MaterialAutoCompleteTextView dropdown, String[] options, int index) {
        if (dropdown != null && options != null && index >= 0 && index < options.length) {
            dropdown.setText(options[index], false);
        }
    }

    private int findIndexForValue(String[] options, String value) {
        if (TextUtils.isEmpty(value) || options == null) {
            return 0; // Position 0 = "All"
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    private String getSelectedValue(String[] options, int index) {
        if (options == null || index <= 0 || index >= options.length) {
            return ""; // Position 0 ("All") corresponds to no filter ("")
        }
        return options[index];
    }
}
