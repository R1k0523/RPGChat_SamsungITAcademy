package com.boringowl.rpgchat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.adapters.FindFriendsAdapter;
import com.boringowl.rpgchat.models.Contacts;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FindFriendsActivity extends AppCompatActivity {
    private EditText searchText;
    private RecyclerView FindFriendsRecyclerList;
    private DatabaseReference UsersRef;
    private List<Contacts> list_of_people;
    private FindFriendsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        FindFriendsRecyclerList = findViewById(R.id.find_friends_recycler_list);
        searchText = findViewById(R.id.search_chats);
        FindFriendsRecyclerList.setLayoutManager(new LinearLayoutManager(this));

        Toolbar mToolbar = findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(mToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find Friends");

        list_of_people = new ArrayList<>();

        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchChats(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();


        RetrieveAndDisplayGroups();
    }

    private void RetrieveAndDisplayGroups() {

        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (searchText.getText().toString().equals("")) {
                    list_of_people.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Contacts contact = snapshot.getValue(Contacts.class);
                        if (snapshot.hasChild("name"))
                            list_of_people.add(contact);
                    }
                    adapter = new FindFriendsAdapter(list_of_people, getSupportFragmentManager());
                    FindFriendsRecyclerList.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchChats(String s) {
        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("searchname")
                .startAt(s)
                .endAt(s + "\uf8ff");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list_of_people.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Contacts contact = snapshot.getValue(Contacts.class);
                    list_of_people.add(contact);
                }
                adapter = new FindFriendsAdapter(list_of_people, getSupportFragmentManager());
                FindFriendsRecyclerList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
