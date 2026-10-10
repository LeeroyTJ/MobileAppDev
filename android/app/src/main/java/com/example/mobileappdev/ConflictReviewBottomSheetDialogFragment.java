package com.example.mobileappdev;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class ConflictReviewBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "ConflictReviewBottomSheet";

    public static ConflictReviewBottomSheetDialogFragment newInstance() {
        return new ConflictReviewBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_conflict_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvClientProposal = view.findViewById(R.id.tvClientProposal);
        TextView tvServerRecord = view.findViewById(R.id.tvServerRecord);
        MaterialButton btnKeepServer = view.findViewById(R.id.btnKeepServer);
        MaterialButton btnRetryMine = view.findViewById(R.id.btnRetryMine);

        if (tvClientProposal != null) tvClientProposal.setText("Thabo Jumbe (My Local Edit)");
        if (tvServerRecord != null) tvServerRecord.setText("Thabo Jumbe (Server Record v2)");

        if (btnKeepServer != null) {
            btnKeepServer.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Server record accepted", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }

        if (btnRetryMine != null) {
            btnRetryMine.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Local edit retried with updated base version", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }
    }
}
