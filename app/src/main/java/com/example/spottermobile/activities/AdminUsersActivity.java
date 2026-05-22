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
import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerUsers;
    private EditText etSearchUsers;
    private TextView tvTotalMembers;
    private LinearLayout layoutEmptyUsers;

    private FirestoreHelper firestoreHelper;

    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filteredUsers = new ArrayList<>();

    private UsersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerUsers = findViewById(R.id.recyclerUsers);
        etSearchUsers = findViewById(R.id.etSearchUsers);
        tvTotalMembers = findViewById(R.id.tvTotalMembers);
        layoutEmptyUsers = findViewById(R.id.layoutEmptyUsers);

        firestoreHelper = new FirestoreHelper();

        recyclerUsers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new UsersAdapter(filteredUsers);
        recyclerUsers.setAdapter(adapter);

        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllUsers();
    }

    // ---------------- LOAD USERS ----------------

    private void loadAllUsers() {

        firestoreHelper.getAllUsers(
                new FirestoreHelper.FirestoreCallback<List<User>>() {

                    @Override
                    public void onSuccess(List<User> users) {

                        allUsers.clear();

                        if (users != null) {
                            allUsers.addAll(users);
                        }

                        String query = etSearchUsers.getText() != null
                                ? etSearchUsers.getText().toString()
                                : "";

                        filterUsers(query);
                    }

                    @Override
                    public void onFailure(String errorMessage) {

                        updateUiState();
                    }
                });
    }

    // ---------------- SEARCH ----------------

    private void setupSearch() {

        etSearchUsers.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {}

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterUsers(String query) {

        filteredUsers.clear();

        if (query == null || query.trim().isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {

            String q = query.toLowerCase(Locale.getDefault()).trim();

            for (User u : allUsers) {

                String name = safeLower(u.getFullName());
                String username = safeLower(u.getUsername());
                String email = safeLower(u.getEmail());

                if (name.contains(q)
                        || username.contains(q)
                        || email.contains(q)) {

                    filteredUsers.add(u);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateUiState();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    // ---------------- UI STATE ----------------

    private void updateUiState() {

        tvTotalMembers.setText(
                String.format(Locale.getDefault(),
                        "%d members",
                        filteredUsers.size())
        );

        boolean empty = filteredUsers.isEmpty();

        layoutEmptyUsers.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );

        recyclerUsers.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );
    }

    // ---------------- ADAPTER ----------------

    private class UsersAdapter
            extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

        private final List<User> users;

        UsersAdapter(List<User> users) {
            this.users = users;
        }

        @Override
        public UserViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {

            View view = LayoutInflater.from(
                    parent.getContext()
            ).inflate(
                    R.layout.item_user,
                    parent,
                    false
            );

            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                UserViewHolder holder,
                int position
        ) {

            User user = users.get(position);

            holder.tvInitials.setText(
                    getInitials(user.getFullName())
            );

            holder.tvInitials.setBackgroundColor(
                    user.isSuspended()
                            ? Color.parseColor("#EF4444")
                            : Color.parseColor("#1E3A8A")
            );

            holder.tvFullName.setText(user.getFullName());
            holder.tvUsername.setText(
                    "@" + user.getUsername()
            );

            holder.tvEmail.setText(user.getEmail());

            if (user.isSuspended()) {

                holder.tvStatus.setText("SUSPENDED");
                holder.tvStatus.setTextColor(
                        Color.parseColor("#EF4444")
                );
                holder.tvStatus.setBackgroundColor(
                        Color.parseColor("#FEE2E2")
                );

            } else {

                holder.tvStatus.setText("ACTIVE");
                holder.tvStatus.setTextColor(
                        Color.parseColor("#10B981")
                );
                holder.tvStatus.setBackgroundColor(
                        Color.parseColor("#D1FAE5")
                );
            }

            holder.itemView.setOnClickListener(v -> {

                Intent intent = new Intent(
                        AdminUsersActivity.this,
                        MemberDetailActivity.class
                );

                intent.putExtra(
                        MemberDetailActivity.EXTRA_USER_ID,
                        user.getFirestoreId()
                );

                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        private String getInitials(String fullName) {

            if (fullName == null || fullName.trim().isEmpty()) {
                return "?";
            }

            String[] parts = fullName.trim().split("\\s+");

            if (parts.length == 1) {
                return parts[0]
                        .substring(0, 1)
                        .toUpperCase();
            }

            return (parts[0].substring(0, 1)
                    + parts[parts.length - 1]
                    .substring(0, 1))
                    .toUpperCase();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {

            TextView tvInitials;
            TextView tvFullName;
            TextView tvUsername;
            TextView tvEmail;
            TextView tvStatus;

            UserViewHolder(View itemView) {
                super(itemView);

                tvInitials = itemView.findViewById(
                        R.id.tvUserInitials
                );

                tvFullName = itemView.findViewById(
                        R.id.tvUserFullName
                );

                tvUsername = itemView.findViewById(
                        R.id.tvUserUsername
                );

                tvEmail = itemView.findViewById(
                        R.id.tvUserEmail
                );

                tvStatus = itemView.findViewById(
                        R.id.tvUserStatus
                );
            }
        }
    }

    // ---------------- UTILS ----------------

    private String getInitials(String fullName) {

        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }

        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (parts[0].substring(0, 1)
                + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }
}