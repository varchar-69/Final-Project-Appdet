package com.example.spottermobile.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUsersActivity extends AppCompatActivity {

    private RecyclerView    recyclerUsers;
    private EditText        etSearchUsers;
    private TextView        tvTotalMembers;
    private LinearLayout    layoutEmptyUsers;
    private DatabaseHelper  dbHelper;

    private final List<User> allUsers      = new ArrayList<>();
    private final List<User> filteredUsers = new ArrayList<>();
    private UsersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerUsers    = findViewById(R.id.recyclerUsers);
        etSearchUsers    = findViewById(R.id.etSearchUsers);
        tvTotalMembers   = findViewById(R.id.tvTotalMembers);
        layoutEmptyUsers = findViewById(R.id.layoutEmptyUsers);

        dbHelper = new DatabaseHelper(this);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UsersAdapter(filteredUsers);
        recyclerUsers.setAdapter(adapter);

        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload on every resume so suspended status reflects immediately after returning
        // from MemberDetailActivity
        loadAllUsers();
    }

    // ── LOAD ───────────────────────────────────────────────────────────────────

    private void loadAllUsers() {
        allUsers.clear();
        List<User> users = dbHelper.getAllUsers();
        if (users != null) allUsers.addAll(users);

        filteredUsers.clear();
        filteredUsers.addAll(allUsers);
        adapter.notifyDataSetChanged();
        updateUiState();
    }

    // ── SEARCH ─────────────────────────────────────────────────────────────────

    private void setupSearch() {
        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterUsers(s.toString());
            }
        });
    }

    private void filterUsers(String query) {
        filteredUsers.clear();
        if (query.trim().isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {
            String q = query.toLowerCase(Locale.getDefault()).trim();
            for (User u : allUsers) {
                String name  = u.getFullName()  == null ? "" : u.getFullName().toLowerCase();
                String uname = u.getUsername()   == null ? "" : u.getUsername().toLowerCase();
                String email = u.getEmail()      == null ? "" : u.getEmail().toLowerCase();
                if (name.contains(q) || uname.contains(q) || email.contains(q))
                    filteredUsers.add(u);
            }
        }
        adapter.notifyDataSetChanged();
        updateUiState();
    }

    // ── UI STATE ───────────────────────────────────────────────────────────────

    private void updateUiState() {
        tvTotalMembers.setText(filteredUsers.size() + " members");
        boolean empty = filteredUsers.isEmpty();
        layoutEmptyUsers.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── ADAPTER ────────────────────────────────────────────────────────────────

    private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

        private final List<User> users;

        UsersAdapter(List<User> users) { this.users = users; }

        @Override
        public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Proper XML inflation — replaces the old programmatic ViewHolder
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            User user = users.get(position);

            // Initials avatar
            String initials = getInitials(user.getFullName());
            holder.tvInitials.setText(initials);
            holder.tvInitials.setBackgroundColor(
                    user.isSuspended()
                            ? Color.parseColor("#EF4444")   // red for suspended
                            : Color.parseColor("#1E3A8A")); // primary blue for active

            holder.tvFullName.setText(user.getFullName());
            holder.tvUsername.setText("@" + user.getUsername());
            holder.tvEmail.setText(user.getEmail());

            // Suspended / Active badge
            if (user.isSuspended()) {
                holder.tvStatus.setText("SUSPENDED");
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FEE2E2"));
            } else {
                holder.tvStatus.setText("ACTIVE");
                holder.tvStatus.setTextColor(Color.parseColor("#10B981"));
                holder.tvStatus.setBackgroundColor(Color.parseColor("#D1FAE5"));
            }

            // Tap → open MemberDetailActivity
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(AdminUsersActivity.this, MemberDetailActivity.class);
                intent.putExtra(MemberDetailActivity.EXTRA_USER_ID, user.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return users.size(); }

        private String getInitials(String fullName) {
            if (fullName == null || fullName.trim().isEmpty()) return "?";
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                    .toUpperCase();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvInitials, tvFullName, tvUsername, tvEmail, tvStatus;

            UserViewHolder(View itemView) {
                super(itemView);
                tvInitials = itemView.findViewById(R.id.tvUserInitials);
                tvFullName = itemView.findViewById(R.id.tvUserFullName);
                tvUsername = itemView.findViewById(R.id.tvUserUsername);
                tvEmail    = itemView.findViewById(R.id.tvUserEmail);
                tvStatus   = itemView.findViewById(R.id.tvUserStatus);
            }
        }
    }

    // ── UTILS ──────────────────────────────────────────────────────────────────

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
