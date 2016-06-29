package com.example.mpv.bb8;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GameOverActivity extends AppCompatActivity {

    private TextView scoreTextView;
    private Button menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        scoreTextView = (TextView) findViewById(R.id.scoretextview);
        menuButton = (Button) findViewById(R.id.menubutton);

        scoreTextView.setText(scoreTextView.getText().toString() + " " + getIntent().getExtras().getInt("score"));
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GameOverActivity.this, BB8ConnectionActivity.class));
            }
        });
    }
}
