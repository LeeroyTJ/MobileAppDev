package com.example.mobileappdev;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseNavActivity extends AppCompatActivity {

    protected void setupBaseBottomNav(BottomNavigationView bottomNav, int selectedItemId) {
        if (bottomNav == null) return;

        SessionManager sessionManager = SessionManager.getInstance(this);
        boolean isLecturer = sessionManager.isLecturer();

        Menu menu = bottomNav.getMenu();
        MenuItem navHome = menu.findItem(R.id.navHome);
        MenuItem navGroup = menu.findItem(R.id.navGroup);
        MenuItem navSync = menu.findItem(R.id.navSync);
        MenuItem navStudents = menu.findItem(R.id.nav_students);
        MenuItem navUnassigned = menu.findItem(R.id.nav_unassigned);
        MenuItem navMore = menu.findItem(R.id.nav_more);

        if (isLecturer) {
            if (navHome != null) navHome.setVisible(false);
            if (navGroup != null) navGroup.setVisible(false);
            if (navSync != null) navSync.setVisible(false);
            if (navStudents != null) navStudents.setVisible(true);
            if (navUnassigned != null) navUnassigned.setVisible(true);
            if (navMore != null) navMore.setVisible(true);
        } else {
            if (navHome != null) navHome.setVisible(true);
            if (navGroup != null) navGroup.setVisible(true);
            if (navSync != null) navSync.setVisible(true);
            if (navStudents != null) navStudents.setVisible(false);
            if (navUnassigned != null) navUnassigned.setVisible(false);
            if (navMore != null) navMore.setVisible(false);
        }

        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) {
                return true;
            }

            Intent intent = null;
            if (id == R.id.navHome) {
                intent = new Intent(this, ProfileActivity.class);
            } else if (id == R.id.navGroup) {
                intent = new Intent(this, StudentGroupActivity.class);
            } else if (id == R.id.navSync) {
                intent = new Intent(this, SyncActivity.class);
            } else if (id == R.id.nav_students) {
                intent = new Intent(this, RosterActivity.class);
            } else if (id == R.id.nav_unassigned) {
                intent = new Intent(this, UnassignedActivity.class);
            } else if (id == R.id.nav_more) {
                MoreBottomSheetDialogFragment sheet = new MoreBottomSheetDialogFragment();
                sheet.show(getSupportFragmentManager(), MoreBottomSheetDialogFragment.TAG);
                return true;
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}
