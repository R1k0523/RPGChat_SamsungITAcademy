package com.boringowl.rpgchat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.adapters.CharactersAdapter;
import com.boringowl.rpgchat.adapters.MembersAdapter;
import com.boringowl.rpgchat.tools.TimeHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class GroupSettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int GALLERY_PICk = 1;
    private DatabaseReference GroupRef;
    private String groupID, currentUserID, chatCreatorID;
    private EditText groupPassword, groupDescription, groupName;
    private CircleImageView groupImage;
    private Switch inSearch;
    private RecyclerView membersRecycler, charactersRecycler;
    private StorageReference groupImagesRef;
    private ProgressDialog loadingBar;
    private List<String> membersList, charactersList;
    private MembersAdapter adapter;
    private CharactersAdapter charAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);

        groupID = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("groupID")).toString();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(groupID);
        groupImagesRef = FirebaseStorage.getInstance().getReference().child("Group Images");

        final Button updateGroupSettings = findViewById(R.id.update_settings_button);
        final Button deleteGroup = findViewById(R.id.delete_button);
        Button addCharacter = findViewById(R.id.add_character_button);
        inSearch = findViewById(R.id.in_search);
        groupName = findViewById(R.id.set_chat_name);
        groupPassword = findViewById(R.id.set_chat_password);
        groupDescription = findViewById(R.id.set_chat_description);
        groupImage = findViewById(R.id.set_chat_image);
        membersRecycler = findViewById(R.id.recycler_members);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        membersRecycler.setLayoutManager(linearLayoutManager);
        membersRecycler.setAdapter(adapter);
        charactersRecycler = findViewById(R.id.recycler_characters);
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this);
        charactersRecycler.setLayoutManager(linearLayoutManager2);
        charactersRecycler.setAdapter(charAdapter);

        loadingBar = new ProgressDialog(this);

        membersList = new ArrayList<>();
        charactersList = new ArrayList<>();

        Toolbar settingsToolBar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(settingsToolBar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Group Settings");

        addCharacter.setOnClickListener(this);
        updateGroupSettings.setOnClickListener(this);
        deleteGroup.setOnClickListener(this);
        inSearch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    GroupRef.child("info").child("insearch").setValue("opened");
                } else {
                    GroupRef.child("info").child("insearch").setValue("closed");
                }
            }
        });

        GroupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatCreatorID = dataSnapshot.child("info").child("creator").getValue().toString();
                if (!currentUserID.equals(chatCreatorID)) {
                    updateGroupSettings.setVisibility(View.GONE);
                    deleteGroup.setVisibility(View.GONE);
                    inSearch.setVisibility(View.GONE);
                    groupName.setFocusable(false);
                    groupPassword.setFocusable(false);
                    groupDescription.setFocusable(false);
                    groupImage.setFocusable(false);
                    groupName.setFocusableInTouchMode(false);
                    groupPassword.setFocusableInTouchMode(false);
                    groupDescription.setFocusableInTouchMode(false);
                    groupImage.setFocusableInTouchMode(false);
                } else {
                    groupImage.setOnClickListener(GroupSettingsActivity.this);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        RetrieveGroupInfo();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SendUserToGroupChatActivity();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.set_chat_image:
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_PICk);
                break;
            case R.id.update_settings_button:
                UpdateSettings();
                break;
            case R.id.delete_button:
                DeleteGroup();
                break;
            case R.id.add_character_button:
                RequestNewCharacter();
                break;
        }
    }

    private void CreateNewCharacter(final String characterName) {
        final DatabaseReference membersRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(groupID).child("Characters").push();
        String charKey = membersRef.getKey();

        membersRef.child("info").child("name").setValue(characterName);
        membersRef.child("info").child("owner").setValue(currentUserID);
        membersRef.child("info").child("level").setValue("0");
        String[] charactermain = {"race", "class", "background", "description"};
        String[] characterfeatures = {"xp", "armor", "initiative", "hits", "speed"};
        String[] characterskills = {"strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma"};
        for (String s : charactermain)
            membersRef.child("Character").child(s).setValue("No info");
        for (String characterfeature : characterfeatures)
            membersRef.child("Features").child(characterfeature).setValue("0");
        for (String characterskill : characterskills)
            membersRef.child("Skills").child(characterskill).setValue("0");
        RetrieveGroupInfo();
    }

    private void RequestNewCharacter() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        builder.setTitle("Create Character");
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        final EditText characterNameText = dialogView.findViewById(R.id.value);

        builder.setView(dialogView);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String characterName = characterNameText.getText().toString();

                        if (TextUtils.isEmpty(characterName)) {
                            Toast.makeText(GroupSettingsActivity.this, "Please check your input data...", Toast.LENGTH_SHORT).show();
                        } else {
                            CreateNewCharacter(characterName);
                            dialogInterface.dismiss();
                        }
                    }
                }
        );
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    private void DeleteGroup() {
        CharSequence[] options = new CharSequence[]{"Yes", "No"};

        final AlertDialog.Builder builder = new AlertDialog.Builder(GroupSettingsActivity.this);
        builder.setTitle("You really want to delete this group?");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        SendUserToMainActivity();
                        GroupRef.removeValue();
                        break;
                    case 1:
                        break;
                }
            }
        });
        builder.show();
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(GroupSettingsActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    private void SendUserToGroupChatActivity() {
        Intent groupIntent = new Intent(GroupSettingsActivity.this, GroupChatActivity.class);
        groupIntent.putExtra("groupID", groupID);
        startActivity(groupIntent);
        finish();
    }

    private void UpdateSettings() {
        String setName = groupName.getText().toString();
        String setPassword = groupPassword.getText().toString();
        String setDescription = groupDescription.getText().toString();
        HashMap<String, Object> profileMap = new HashMap<>();
        profileMap.put("groupname", setName);
        profileMap.put("searchname", setName.toLowerCase());
        profileMap.put("password", setPassword);
        profileMap.put("description", setDescription);

        GroupRef.child("info").updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    SendUserToGroupChatActivity();
                    Toast.makeText(GroupSettingsActivity.this, "Group Updated Successfully...", Toast.LENGTH_SHORT).show();
                } else {
                    String message = task.getException().toString();
                    Toast.makeText(GroupSettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void RetrieveGroupInfo() {
        GroupRef.child("info").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    String retrievePassword = dataSnapshot.child("password").getValue().toString();
                    String retrieveName = dataSnapshot.child("groupname").getValue().toString();

                    String retrieveDescription = "";
                    if (dataSnapshot.hasChild("description"))
                        retrieveDescription = dataSnapshot.child("description").getValue().toString();

                    groupName.setText(retrieveName);
                    groupPassword.setText(retrievePassword);
                    groupDescription.setText(retrieveDescription);
                    inSearch.setChecked(dataSnapshot.child("insearch").getValue().toString().equals("opened"));

                    if (dataSnapshot.hasChild("image")) {
                        String retrieveProfilePhoto = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(retrieveProfilePhoto).placeholder(R.drawable.group_icon).into(groupImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        TimeHandler.update("online");
        GroupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    membersList.clear();
                    charactersList.clear();
                    if (dataSnapshot.child("Members").hasChildren()) {
                        for (DataSnapshot snapshot : dataSnapshot.child("Members").getChildren()) {
                            membersList.add(snapshot.getKey());
                        }
                    }
                    if (dataSnapshot.child("Characters").hasChildren()) {
                        for (DataSnapshot snapshot : dataSnapshot.child("Characters").getChildren()) {
                            charactersList.add(snapshot.getKey());
                        }
                    }
                    adapter = new MembersAdapter(GroupSettingsActivity.this, membersList, currentUserID, groupID, chatCreatorID);
                    membersRecycler.setAdapter(adapter);
                    charAdapter = new CharactersAdapter(GroupSettingsActivity.this, charactersList, groupID);
                    charactersRecycler.setAdapter(charAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICk && resultCode == RESULT_OK && data != null) {
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                loadingBar.setTitle("Set Group Image");
                loadingBar.setMessage("Please wait, your image is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                assert result != null;
                Uri resultUri = result.getUri();


                final StorageReference filePath = groupImagesRef.child(groupID + ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(GroupSettingsActivity.this, "Group Image uploaded successfully...", Toast.LENGTH_SHORT).show();

                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String downloadedUrl = uri.toString();

                                    GroupRef.child("info").child("image").setValue(downloadedUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                loadingBar.dismiss();
                                            } else {
                                                String message = task.getException().toString();
                                                Toast.makeText(GroupSettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });
                                }
                            });
                        } else {
                            String message = task.getException().toString();
                            Toast.makeText(GroupSettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimeHandler.update("online");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TimeHandler.update("offline");
    }
}
