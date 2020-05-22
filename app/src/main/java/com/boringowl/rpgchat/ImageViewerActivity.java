package com.boringowl.rpgchat;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.boringowl.rpgchat.tools.TimeHandler;
import com.squareup.picasso.Picasso;


public class ImageViewerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView imageView = findViewById(R.id.image_viewer);
        String imageUrl = getIntent().getStringExtra("url");

        Picasso.get().load(imageUrl).into(imageView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        TimeHandler.update("online");
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
