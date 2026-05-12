package com.example.spottermobile.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.User;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersActivity extends AppCompatActivity {
    private ListView listViewUsers;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        listViewUsers = findViewById(R.id.listViewUsers);
        dbHelper = new DatabaseHelper(this);

        loadAllUsers();
    }

    private void loadAllUsers() {
        List<User> users = dbHelper.getAllUsers();
        List<String> userList = new ArrayList<>();

        for (User user : users) {
            String role = "user".equals(user.getRole()) ? "👤 User" : "👨‍💼 Admin";
            userList.add(user.getFullName() + " | " + user.getUsername() + " | " +
                    user.getEmail() + " | " + role);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, userList);
        listViewUsers.setAdapter(adapter);
    }
}