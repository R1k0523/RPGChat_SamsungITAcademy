package com.boringowl.rpgchat.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.boringowl.rpgchat.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileDialog extends DialogFragment {

    private String receiverUserID, senderUserID, currentState;
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendRequestButton, declineButton;
    private DatabaseReference userRef, chatRequestRef, contactsRef, notificationRef;

    public ProfileDialog(String receiverUserID) {
        this.receiverUserID = receiverUserID;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myView = inflater.inflate(R.layout.dialog_profile, null);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        senderUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        userProfileImage = myView.findViewById(R.id.visit_profile_image);
        userProfileName = myView.findViewById(R.id.visit_user_name);
        userProfileStatus = myView.findViewById(R.id.visit_profile_status);
        sendRequestButton = myView.findViewById(R.id.send_message_request_button);
        declineButton = myView.findViewById(R.id.decline_message_request_button);

        currentState = "new";
        retrieveUserInfo();
        return myView;
    }

    private void retrieveUserInfo() {
        userRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String userName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                String userStatus = Objects.requireNonNull(dataSnapshot.child("status").getValue()).toString();
                if (dataSnapshot.hasChild("image")) {
                    String userImage = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                }
                userProfileName.setText(userName);
                userProfileStatus.setText(userStatus);
                if (!receiverUserID.equals(senderUserID))
                    manageChatRequests();
                else
                    sendRequestButton.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void manageChatRequests() {
        chatRequestRef.child(senderUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(receiverUserID)) {
                    String requestType = Objects.requireNonNull(dataSnapshot.child(receiverUserID)
                            .child("request_type").getValue()).toString();

                    if (requestType.equals("sent")) {
                        currentState = "request_sent";
                        sendRequestButton.setText("Cancel Request");
                    } else if (requestType.equals("received")) {
                        currentState = "request_received";
                        sendRequestButton.setText("Accept request");

                        declineButton.setVisibility(View.VISIBLE);
                        declineButton.setEnabled(true);

                        declineButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CancelChatRequest();
                            }
                        });
                    }
                } else {
                    contactsRef.child(senderUserID).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(receiverUserID)) {
                                currentState = "friends";
                                sendRequestButton.setText("Remove contact");
                            }
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
        sendRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequestButton.setEnabled(false);
                switch (currentState) {
                    case "new":
                        SendChatRequest();
                        break;
                    case "request_sent":
                        CancelChatRequest();
                        break;
                    case "request_received":
                        AcceptChatRequest();
                        break;
                    case "friends":
                        RemoveSpecificContact();
                        break;
                }
            }
        });
    }

    private void RemoveSpecificContact() {
        contactsRef.child(senderUserID).child(receiverUserID)
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    contactsRef.child(receiverUserID).child(senderUserID)
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                sendRequestButton.setEnabled(true);
                                currentState = "new";
                                sendRequestButton.setText("Send request");

                                declineButton.setVisibility(View.INVISIBLE);
                                declineButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }

    private void AcceptChatRequest() {
        contactsRef.child(senderUserID).child(receiverUserID)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            chatRequestRef.child(senderUserID).child(receiverUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                chatRequestRef.child(receiverUserID).child(senderUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                sendRequestButton.setEnabled(true);
                                                                currentState = "friends";
                                                                sendRequestButton.setText("Remove contact");

                                                                declineButton.setVisibility(View.INVISIBLE);
                                                                declineButton.setEnabled(false);
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void SendChatRequest() {
        chatRequestRef.child(senderUserID).child(receiverUserID)
            .child("request_type").setValue("sent")
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    chatRequestRef.child(receiverUserID).child(senderUserID)
                        .child("request_type").setValue("received")
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                HashMap<String, String> chatNotificationMap = new HashMap<>();
                                chatNotificationMap.put("from", senderUserID);
                                chatNotificationMap.put("type", "request");

                                notificationRef.child(receiverUserID).push()
                                    .setValue(chatNotificationMap)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                sendRequestButton.setEnabled(true);
                                                currentState = "request_sent";
                                                sendRequestButton.setText("Cancel Request");
                                            }
                                        }
                                    });
                            }
                            }
                        });
                }
                }
            });
    }

    private void CancelChatRequest() {
        chatRequestRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            chatRequestRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                sendRequestButton.setEnabled(true);
                                                currentState = "new";
                                                sendRequestButton.setText("Send request");

                                                declineButton.setVisibility(View.INVISIBLE);
                                                declineButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

}
