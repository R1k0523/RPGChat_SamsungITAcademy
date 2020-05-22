package com.boringowl.rpgchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.boringowl.rpgchat.tools.TimeHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final int GALLERY_PICk = 1;
    private DatabaseReference RootRef;
    private String currentUserID;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;
    private StorageReference UserProfileImagesRef;
    private ProgressDialog loadingBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        Button updateAccountSettings = findViewById(R.id.update_settings_button);
        final Button deleteButton = findViewById(R.id.delete_button);
        userName = findViewById(R.id.set_username);
        userStatus = findViewById(R.id.set_status);
        userProfileImage = findViewById(R.id.set_profile_image);

        loadingBar = new ProgressDialog(this);

        Toolbar settingsToolBar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(settingsToolBar);
        getSupportActionBar().setTitle("Account Settings");

        RootRef.child("Users").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("name"))
                    deleteButton.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpdateSettings();
            }
        });

        RetrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_PICk);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });
    }

    private void deleteAccount() {
        final FirebaseUser currentUser = mAuth.getCurrentUser();
        RootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.child("Chat Requests").getChildren()) {
                    if (snapshot.hasChild(currentUserID)) {
                        snapshot.child(currentUserID).getRef().removeValue();
                    }
                    if (snapshot.getKey().equals(currentUserID))
                        snapshot.getRef().removeValue();
                }
                for (DataSnapshot snapshot : dataSnapshot.child("Notifications").getChildren()) {
                    if (snapshot.hasChild(currentUserID)) {
                        snapshot.child(currentUserID).getRef().removeValue();
                    }
                    if (snapshot.getKey().equals(currentUserID))
                        snapshot.getRef().removeValue();
                }
                for (DataSnapshot snapshot : dataSnapshot.child("Contacts").getChildren()) {
                    if (snapshot.hasChild(currentUserID)) {
                        snapshot.child(currentUserID).getRef().removeValue();
                    }
                    if (snapshot.getKey().equals(currentUserID))
                        snapshot.getRef().removeValue();
                }
                for (DataSnapshot snapshot : dataSnapshot.child("Messages").getChildren()) {
                    if (snapshot.hasChild(currentUserID)) {
                        snapshot.child(currentUserID).getRef().removeValue();
                    }
                    if (snapshot.getKey().equals(currentUserID))
                        snapshot.getRef().removeValue();
                }
                for (DataSnapshot snapshot : dataSnapshot.child("Groups").getChildren()) {
                    if (snapshot.child("Members").hasChild(currentUserID)) {
                        snapshot.child("Members").child(currentUserID).getRef().removeValue();
                        if (!snapshot.child("Members").hasChildren())
                            snapshot.getRef().removeValue();
                        else if (snapshot.child("info").child("creator").getValue().toString().equals(currentUserID)) {
                            for (DataSnapshot snap : snapshot.child("Members").getChildren())
                                snapshot.child("info").child("creator").getRef().setValue(snap.getKey());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        RootRef.child("Users").child(currentUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                                SendUserToLoginActivity();
                            else
                                Toast.makeText(ProfileActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void UpdateSettings() {
        String setUserName = userName.getText().toString();
        String setStatus = userStatus.getText().toString();
        if (TextUtils.isEmpty(setUserName)) {
            Toast.makeText(this, "Please write your user name first....", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(setStatus)) {
            Toast.makeText(this, "Please write your status....", Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserID);
            profileMap.put("name", setUserName);
            profileMap.put("searchname", setUserName.toLowerCase());
            profileMap.put("status", setStatus);

            RootRef.child("Users").child(currentUserID).updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        SendUserToMainActivity();
                        Toast.makeText(ProfileActivity.this, "Profile Updated Successfully...", Toast.LENGTH_SHORT).show();
                    } else {
                        String message = Objects.requireNonNull(task.getException()).toString();
                        Toast.makeText(ProfileActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void RetrieveUserInfo() {
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if ((dataSnapshot.hasChild("name") && (dataSnapshot.hasChild("image")))) {
                        String retrieveUserName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                        String retrieveStatus = Objects.requireNonNull(dataSnapshot.child("status").getValue()).toString();
                        String retrieveProfilePhoto = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();

                        userName.setText(retrieveUserName);
                        userStatus.setText(retrieveStatus);
                        Picasso.get().load(retrieveProfilePhoto).placeholder(R.drawable.profile_image).into(userProfileImage);
                    } else if ((dataSnapshot.hasChild("name"))) {
                        String retrieveUserName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                        String retrievesStatus = Objects.requireNonNull(dataSnapshot.child("status").getValue()).toString();

                        userName.setText(retrieveUserName);
                        userStatus.setText(retrievesStatus);
                    } else {
                        Toast.makeText(ProfileActivity.this, "Please set & update your profile information...", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(ProfileActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
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
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, your profile image is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                assert result != null;
                Uri resultUri = result.getUri();


                final StorageReference filePath = UserProfileImagesRef.child(currentUserID + ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Profile Image uploaded Successfully...", Toast.LENGTH_SHORT).show();

                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String downloadedUrl = uri.toString();

                                    RootRef.child("Users").child(currentUserID).child("image")
                                            .setValue(downloadedUrl)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(ProfileActivity.this, "Image saved in Database", Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    } else {
                                                        String message = Objects.requireNonNull(task.getException()).toString();
                                                        Toast.makeText(ProfileActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }
                                                }
                                            });
                                }
                            });
                        } else {
                            String message = Objects.requireNonNull(task.getException()).toString();
                            Toast.makeText(ProfileActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        TimeHandler.update("online");
    }

}
