package com.example.mobileappdev;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SessionExpiredDialogFragment extends DialogFragment {

    public static final String TAG = "SessionExpiredDialog";

    public static SessionExpiredDialogFragment newInstance() {
        return new SessionExpiredDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        setCancelable(false);
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Session Expired")
                .setMessage("Your session has expired or is invalid. Please sign in again.")
                .setPositiveButton("OK", (dialog, which) -> {
                    SessionManager.getInstance(requireContext()).clearSession();

                    Intent intent = new Intent(requireContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .create();
    }
}
