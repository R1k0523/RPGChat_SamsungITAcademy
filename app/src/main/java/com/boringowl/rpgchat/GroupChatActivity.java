package com.boringowl.rpgchat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.adapters.GroupMessageAdapter;
import com.boringowl.rpgchat.dialogs.DiceDialog;
import com.boringowl.rpgchat.models.Messages;
import com.boringowl.rpgchat.tools.Dice;
import com.boringowl.rpgchat.tools.TimeHandler;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatActivity extends AppCompatActivity {
    private final List<Messages> messagesList = new ArrayList<>();
    private String groupID, groupName, messageSenderID, messageSender, chatCreator, chatImageURL;
    private DatabaseReference rootRef;
    private EditText messageInputText, characterText;
    private CircleImageView chatImage;
    private GroupMessageAdapter messageAdapter;
    private RecyclerView userMessagesList;

    private String checker = "", myUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        messageSenderID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        groupID = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("groupID")).toString();

        Toolbar chatToolBar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolBar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert layoutInflater != null;
        View actionBarView = layoutInflater.inflate(R.layout.bar_chat, null);
        actionBar.setCustomView(actionBarView);


        final TextView groupNameText = findViewById(R.id.chat_name);
        groupNameText.setTextSize(24);
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageSender = Objects.requireNonNull(dataSnapshot.child("Users").child(messageSenderID).child("name").getValue()).toString();
                chatCreator = Objects.requireNonNull(dataSnapshot.child("Groups").child(groupID).child("info").child("creator").getValue()).toString();
                groupName = Objects.requireNonNull(dataSnapshot.child("Groups").child(groupID).child("info").child("groupname").getValue()).toString();
                groupNameText.setText(groupName);
                if (dataSnapshot.child("Groups").child(groupID).child("info").hasChild("image"))
                    chatImageURL = Objects.requireNonNull(dataSnapshot.child("Groups").child(groupID).child("info").child("image").getValue()).toString();
                if (chatImageURL != null)
                    Picasso.get().load(chatImageURL).placeholder(R.drawable.group_icon).into(chatImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });


        findViewById(R.id.user_last_seen).setVisibility(View.GONE);

        ImageButton sendMessageButton = findViewById(R.id.send_message_button);
        ImageButton sendFilesButton = findViewById(R.id.send_files_btn);
        ImageButton characterButton = findViewById(R.id.character_btn);
        ImageButton helpButton = findViewById(R.id.help_button);
        messageInputText = findViewById(R.id.input_message);
        characterText = findViewById(R.id.input_character);
        chatImage = findViewById(R.id.chat_image);

        messageAdapter = new GroupMessageAdapter(GroupChatActivity.this, messagesList, groupID);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        chatImage.setImageResource(R.drawable.group_icon);

        characterButton.setVisibility(View.VISIBLE);

        characterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (characterText.getVisibility() == View.GONE) {
                    characterText.setVisibility(View.VISIBLE);
                } else
                    characterText.setVisibility(View.GONE);
            }
        });

        chatImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToGroupSettingsActivity();
            }
        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
                userMessagesList.scrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount() - 1);
            }
        });

        sendFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checker = "image";
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Image"), 443);
                userMessagesList.scrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount() - 1);
            }
        });

        helpButton.setVisibility(View.VISIBLE);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DiceDialog dialog = new DiceDialog();
                dialog.show(getSupportFragmentManager(), "a");
            }
        });
    }

    private void sendUserToGroupSettingsActivity() {
        Intent groupSettingsIntent = new Intent(GroupChatActivity.this, GroupSettingsActivity.class);
        groupSettingsIntent.putExtra("groupID", groupID);
        startActivity(groupSettingsIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 443 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            if (checker.equals("image")) {

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");
                final String groupRef = "Groups/" + groupID + "/Messages/";

                DatabaseReference messageKeyRef = rootRef.child("Groups")
                        .child(groupID).child("Messages").push();

                final String messagePushID = messageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");

                StorageTask uploadTask = filePath.putFile(fileUri);
                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()) throw task.getException();
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();

                        String characterName = characterText.getText().toString();
                        if (TextUtils.isEmpty(characterName)) {
                            characterName = messageSender;
                        }

                        Map<String, String> messageTextBody = new HashMap<>();
                        messageTextBody.put("message", myUrl);
                        messageTextBody.put("type", checker);
                        messageTextBody.put("from", messageSenderID);
                        messageTextBody.put("name", characterName);
                        messageTextBody.put("to", groupID);
                        messageTextBody.put("messageID", messagePushID);
                        messageTextBody.put("time", TimeHandler.getTime());

                        Map<String, Object> messageBodyDetails = new HashMap<>();
                        messageBodyDetails.put(groupRef + "/" + messagePushID, messageTextBody);

                        rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                messageInputText.setText("");
                                userMessagesList.scrollToPosition(userMessagesList.getAdapter().getItemCount() - 1);
                            }
                        });
                        rootRef.child("Groups").child(groupID).child("info").child("lastmessage").setValue(TimeHandler.getLastTime());
                    }
                });

            } else {
                Toast.makeText(this, "Nothing selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        TimeHandler.update("online");
        rootRef.child("Groups").child(groupID).child("Messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        messagesList.clear();
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Messages messages = snapshot.getValue(Messages.class);
                                messagesList.add(messages);
                            }

                        }
                        messageAdapter = new GroupMessageAdapter(GroupChatActivity.this, messagesList, groupID);
                        userMessagesList.setAdapter(messageAdapter);
                        userMessagesList.scrollToPosition(userMessagesList.getAdapter().getItemCount() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendMessage() {
        String messageText = messageInputText.getText().toString();
        String characterName = characterText.getText().toString();
        if (TextUtils.isEmpty(characterName)) {
            characterName = messageSender;
        }
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "first write your message...", Toast.LENGTH_SHORT).show();
        } else {

            String groupRef = "Groups/" + groupID + "/Messages/";

            DatabaseReference groupMessageKeyRef = rootRef.child("Groups")
                    .child(groupID).child("Messages").push();

            String messagePushID = groupMessageKeyRef.getKey();
            if (messageText.substring(0, 1).equals("/"))
                messageText += Dice.roll(messageText);

            Map<String, String> messageTextBody = new HashMap<>();
            messageTextBody.put("message", messageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);
            messageTextBody.put("name", characterName);
            messageTextBody.put("to", groupID);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("time", TimeHandler.getTime());

            Map<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(groupRef + "/" + messagePushID, messageTextBody);

            rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    messageInputText.setText("");
                }
            });
            rootRef.child("Groups").child(groupID).child("info").child("lastmessage").setValue(TimeHandler.getLastTime());
            userMessagesList.scrollToPosition(userMessagesList.getAdapter().getItemCount() - 1);
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
