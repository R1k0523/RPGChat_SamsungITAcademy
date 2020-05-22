package com.boringowl.rpgchat;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class CharacterActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView characterRecycler, featuresRecycler, skillsRecycler;
    private EditText nameText, levelText;
    private TextView ownerText;
    private Button saveButton, deleteButton;
    private DatabaseReference characterRef, UsersRef;
    private String currentUserID;
    private String ownerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character);

        String groupID = getIntent().getExtras().get("groupID").toString();
        String characterName = getIntent().getExtras().get("characterName").toString();

        Toolbar settingsToolBar = findViewById(R.id.character_toolbar);
        setSupportActionBar(settingsToolBar);
        getSupportActionBar().setTitle("Character changing");


        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        characterRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(groupID).child("Characters").child(characterName);

        deleteButton = findViewById(R.id.delete_button);
        Button characterButton = findViewById(R.id.character_button);
        Button featuresButton = findViewById(R.id.features_button);
        Button minorButton = findViewById(R.id.skills_button);
        saveButton = findViewById(R.id.save_button);

        nameText = findViewById(R.id.name_text);
        levelText = findViewById(R.id.level_text);
        ownerText = findViewById(R.id.owner_text);

        characterRecycler = findViewById(R.id.character_recycler);
        featuresRecycler = findViewById(R.id.features_recycler);
        skillsRecycler = findViewById(R.id.minor_recycler);

        characterRecycler.setLayoutManager(new LinearLayoutManager(this));
        featuresRecycler.setLayoutManager(new LinearLayoutManager(this));
        skillsRecycler.setLayoutManager(new LinearLayoutManager(this));

        deleteButton.setOnClickListener(this);
        characterButton.setOnClickListener(this);
        featuresButton.setOnClickListener(this);
        minorButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        displayInfo();
    }

    private void closeForGuest() {
        saveButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        nameText.setFocusable(false);
        nameText.setFocusableInTouchMode(false);
        nameText.setClickable(false);
        levelText.setFocusable(false);
        levelText.setFocusableInTouchMode(false);
        levelText.setClickable(false);
    }

    private void displayInfo() {
        characterRef.child("info").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    nameText.setText(dataSnapshot.child("name").getValue().toString());
                    levelText.setText(dataSnapshot.child("level").getValue().toString());
                    ownerID = dataSnapshot.child("owner").getValue().toString();
                    if (!ownerID.equals(currentUserID))
                        closeForGuest();
                    UsersRef.child(ownerID).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists())
                                ownerText.setText(dataSnapshot.getValue().toString());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void UpdateCharacter() {
        String setName = nameText.getText().toString();
        String setLevel = levelText.getText().toString();

        HashMap<String, Object> characterMap = new HashMap<>();
        characterMap.put("name", setName);
        characterMap.put("level", setLevel);

        characterRef.child("info").updateChildren(characterMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(CharacterActivity.this, "Character Updated Successfully...", Toast.LENGTH_SHORT).show();
                } else {
                    String message = task.getException().toString();
                    Toast.makeText(CharacterActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_button:
                UpdateCharacter();
                break;
            case R.id.delete_button:
                characterRef.removeValue();
                finish();
                break;

            case R.id.character_button:
                if (characterRecycler.getVisibility() == View.VISIBLE)
                    characterRecycler.setVisibility(View.GONE);
                else
                    characterRecycler.setVisibility(View.VISIBLE);
                showRecycler(characterRecycler, "Character");
                break;

            case R.id.features_button:
                if (featuresRecycler.getVisibility() == View.VISIBLE)
                    featuresRecycler.setVisibility(View.GONE);
                else
                    featuresRecycler.setVisibility(View.VISIBLE);
                showRecycler(featuresRecycler, "Features");
                break;

            case R.id.skills_button:
                if (skillsRecycler.getVisibility() == View.VISIBLE)
                    skillsRecycler.setVisibility(View.GONE);
                else
                    skillsRecycler.setVisibility(View.VISIBLE);
                showRecycler(skillsRecycler, "Skills");
                break;
        }

    }

    private void showRecycler(final RecyclerView recycler, final String statType) {
        DatabaseReference recyclerRef = characterRef.child(statType);
        FirebaseRecyclerOptions<String> options =
                new FirebaseRecyclerOptions.Builder<String>()
                        .setQuery(recyclerRef, String.class)
                        .build();

        FirebaseRecyclerAdapter<String, CharViewHolder> adapter =
                new FirebaseRecyclerAdapter<String, CharViewHolder>(options) {
                    @NonNull
                    @Override
                    public CharViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_character_num, viewGroup, false);
                        if (recycler == characterRecycler)
                            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_character_text, viewGroup, false);
                        return new CharViewHolder(view);
                    }

                    @Override
                    protected void onBindViewHolder(@NonNull CharViewHolder holder, int position, @NonNull final String value) {
                        final String key = getRef(position).getKey();
                        holder.keyText.setText(key);
                        holder.valueText.setText(value);

                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (currentUserID.equals(ownerID)) {
                                    RequestChange(statType, key, value);
                                }
                            }
                        });
                    }
                };

        recycler.setAdapter(adapter);
        adapter.startListening();

    }


    private void RequestChange(final String statType, final String key, final String oldValue) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        final EditText valueText = dialogView.findViewById(R.id.value);

        builder.setTitle("Change Stat");
        builder.setView(dialogView);

        valueText.setText(oldValue);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String value = valueText.getText().toString();
                characterRef.child(statType).child(key).setValue(value);
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    public static class CharViewHolder extends RecyclerView.ViewHolder {
        TextView keyText, valueText;

        CharViewHolder(@NonNull View itemView) {
            super(itemView);

            keyText = itemView.findViewById(R.id.name);
            valueText = itemView.findViewById(R.id.value);
        }
    }
}
