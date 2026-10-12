package com.example.mobileappdev;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SignOutConfirmationDialogFragment extends DialogFragment {

    public static final String TAG = "SignOutConfirmationDialog";

    public static SignOutConfirmationDialogFragment newInstance() {
        return new SignOutConfirmationDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign out?")
                .setMessage("Unsynced changes stay saved for this account. You can sign back in anytime.")
                .setPositiveButton("Sign out", (dialog, which) -> {
                    SessionManager.getInstance(requireContext()).clearSession();
                    Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(requireContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dismiss())
                .create();
    }
}
