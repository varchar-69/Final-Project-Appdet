package com.example.spottermobile.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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

    private RecyclerView recyclerUsers;
    private EditText etSearchUsers;
    private TextView tvTotalMembers;
    private LinearLayout layoutEmptyUsers;

    private DatabaseHelper dbHelper;

    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filteredUsers = new ArrayList<>();

    private UsersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        recyclerUsers = findViewById(R.id.recyclerUsers);
        etSearchUsers = findViewById(R.id.etSearchUsers);
        tvTotalMembers = findViewById(R.id.tvTotalMembers);
        layoutEmptyUsers = findViewById(R.id.layoutEmptyUsers);

        dbHelper = new DatabaseHelper(this);

        recyclerUsers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new UsersAdapter(filteredUsers);
        recyclerUsers.setAdapter(adapter);

        loadAllUsers();
        setupSearch();
    }

    // ── LOAD USERS ─────────────────────────────────────────────────────────────

    private void loadAllUsers() {

        allUsers.clear();

        List<User> users = dbHelper.getAllUsers();

        if (users != null) {
            allUsers.addAll(users);
        }

        filteredUsers.clear();
        filteredUsers.addAll(allUsers);

        adapter.notifyDataSetChanged();

        updateUiState();
    }

    // ── SEARCH ─────────────────────────────────────────────────────────────────

    private void setupSearch() {

        etSearchUsers.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {

            }

            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {

                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void filterUsers(String query) {

        filteredUsers.clear();

        if (query.trim().isEmpty()) {

            filteredUsers.addAll(allUsers);

        } else {

            String searchText =
                    query.toLowerCase(Locale.getDefault()).trim();

            for (User user : allUsers) {

                String fullName =
                        user.getFullName() == null
                                ? ""
                                : user.getFullName().toLowerCase();

                String username =
                        user.getUsername() == null
                                ? ""
                                : user.getUsername().toLowerCase();

                String email =
                        user.getEmail() == null
                                ? ""
                                : user.getEmail().toLowerCase();

                if (fullName.contains(searchText)
                        || username.contains(searchText)
                        || email.contains(searchText)) {

                    filteredUsers.add(user);
                }
            }
        }

        adapter.notifyDataSetChanged();

        updateUiState();
    }

    // ── UI STATE ───────────────────────────────────────────────────────────────

    private void updateUiState() {

        tvTotalMembers.setText(
                filteredUsers.size() + " members"
        );

        if (filteredUsers.isEmpty()) {

            layoutEmptyUsers.setVisibility(LinearLayout.VISIBLE);
            recyclerUsers.setVisibility(RecyclerView.GONE);

        } else {

            layoutEmptyUsers.setVisibility(LinearLayout.GONE);
            recyclerUsers.setVisibility(RecyclerView.VISIBLE);
        }
    }

    // ── ADAPTER ────────────────────────────────────────────────────────────────

    private class UsersAdapter
            extends RecyclerView.Adapter<UserViewHolder> {

        private final List<User> users;

        public UsersAdapter(List<User> users) {
            this.users = users;
        }

        @Override
        public UserViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {

            LinearLayout container =
                    new LinearLayout(parent.getContext());

            container.setOrientation(LinearLayout.VERTICAL);

            container.setPadding(32, 28, 32, 28);

            RecyclerView.LayoutParams params =
                    new RecyclerView.LayoutParams(
                            RecyclerView.LayoutParams.MATCH_PARENT,
                            RecyclerView.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(16, 8, 16, 8);

            container.setLayoutParams(params);

            container.setBackgroundResource(android.R.color.white);

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(16);
            tvName.setTextColor(getResources().getColor(R.color.dark_gray));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvUsername = new TextView(parent.getContext());
            tvUsername.setTextSize(14);

            TextView tvEmail = new TextView(parent.getContext());
            tvEmail.setTextSize(13);

            TextView tvRole = new TextView(parent.getContext());
            tvRole.setTextSize(12);
            tvRole.setGravity(Gravity.END);

            container.addView(tvName);
            container.addView(tvUsername);
            container.addView(tvEmail);
            container.addView(tvRole);

            return new UserViewHolder(
                    container,
                    tvName,
                    tvUsername,
                    tvEmail,
                    tvRole
            );
        }

        @Override
        public void onBindViewHolder(
                UserViewHolder holder,
                int position
        ) {

            User user = users.get(position);

            holder.tvName.setText(user.getFullName());

            holder.tvUsername.setText(
                    "@" + user.getUsername()
            );

            holder.tvEmail.setText(user.getEmail());

            String role =
                    "user".equals(user.getRole())
                            ? "👤 USER"
                            : "👨‍💼 ADMIN";

            holder.tvRole.setText(role);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }

    // ── VIEW HOLDER ────────────────────────────────────────────────────────────

    static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvUsername;
        TextView tvEmail;
        TextView tvRole;

        public UserViewHolder(
                LinearLayout itemView,
                TextView tvName,
                TextView tvUsername,
                TextView tvEmail,
                TextView tvRole
        ) {

            super(itemView);

            this.tvName = tvName;
            this.tvUsername = tvUsername;
            this.tvEmail = tvEmail;
            this.tvRole = tvRole;
        }
    }
}