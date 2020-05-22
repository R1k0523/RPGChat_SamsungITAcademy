package com.boringowl.rpgchat.tools;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class TimeHandler {

    private static String inputTime = "dd MMM yyyy, HH:mm, z";
    private static String outputTime = "dd MMM,HH:mm";
    private static String lastMessageTime = "yyyy MM dd HH mm ss";

    public static void update(final String state) {

        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat timeFormat = new SimpleDateFormat(inputTime, Locale.ENGLISH);
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        final String saveCurrentTime = timeFormat.format(calendar.getTime());
        final String currentUserID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseDatabase.getInstance().getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    dataSnapshot.child(currentUserID).child("userState").child("time").getRef().setValue(saveCurrentTime);
                    dataSnapshot.child(currentUserID).child("userState").child("state").getRef().setValue(state);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static String convertTime(String timeGMT) {
        SimpleDateFormat intimeFormat = new SimpleDateFormat(inputTime, Locale.ENGLISH);
        SimpleDateFormat outtimeFormat = new SimpleDateFormat(outputTime, Locale.ENGLISH);
        try {
            Date date = intimeFormat.parse(timeGMT);
            assert date != null;
            return outtimeFormat.format(date);
        } catch (ParseException e) {
            return timeGMT;
        }
    }

    public static String getTime() {
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat timeFormat = new SimpleDateFormat(inputTime, Locale.ENGLISH);
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return timeFormat.format(calendar.getTime());
    }

    public static String getLastTime() {
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat timeFormat = new SimpleDateFormat(lastMessageTime, Locale.ENGLISH);
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return timeFormat.format(calendar.getTime());
    }
}
