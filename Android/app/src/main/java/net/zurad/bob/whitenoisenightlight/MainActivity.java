package net.zurad.bob.whitenoisenightlight;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    LinearLayout _mainLayout;
    SeekBar _seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _mainLayout = (LinearLayout) findViewById(R.id.mainLayout);

        _seekBar = (SeekBar) findViewById(R.id.seekBar);
        _seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String hex = Integer.toHexString(progress);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                _mainLayout.setBackgroundColor(Color.parseColor("#" + hex + hex + hex));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }
}
