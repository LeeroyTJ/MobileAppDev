package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MoreBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "MoreBottomSheet";

    public static MoreBottomSheetDialogFragment newInstance() {
        return new MoreBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnClose = view.findViewById(R.id.btnCloseMore);
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        View groupsOverview = view.findViewById(R.id.optionGroupsOverview);
        View unassigned = view.findViewById(R.id.optionUnassigned);
        View sync = view.findViewById(R.id.optionSync);
        View signOut = view.findViewById(R.id.optionSignOut);

        if (groupsOverview != null) {
            groupsOverview.setOnClickListener(v -> {
                dismiss();
                GroupPickerBottomSheetDialogFragment dialog = new GroupPickerBottomSheetDialogFragment();
                dialog.show(getParentFragmentManager(), GroupPickerBottomSheetDialogFragment.TAG);
            });
        }

        if (unassigned != null) {
            unassigned.setOnClickListener(v -> {
                dismiss();
                startActivity(new Intent(requireContext(), UnassignedActivity.class));
            });
        }

        if (sync != null) {
            sync.setOnClickListener(v -> {
                dismiss();
                startActivity(new Intent(requireContext(), SyncActivity.class));
            });
        }

        if (signOut != null) {
            signOut.setOnClickListener(v -> {
                dismiss();
                new SignOutConfirmationDialogFragment().show(getParentFragmentManager(), SignOutConfirmationDialogFragment.TAG);
            });
        }
    }
}
