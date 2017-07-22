package net.zurad.bob.whitenoisenightlight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    LinearLayout _mainLayout;
    SeekBar _seekBar;
    boolean _canChangeScreenBrightness;

    final int CODE_WRITE_SETTINGS_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        _seekBar = (SeekBar) findViewById(R.id.seekBar);

        //check permissions to change screen brightness
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            _canChangeScreenBrightness = Settings.System.canWrite(this);
        } else {
            _canChangeScreenBrightness = ContextCompat
                    .checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }

        //request permissions
        if (!_canChangeScreenBrightness) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                //permission for changing screen brightness
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, CODE_WRITE_SETTINGS_PERMISSION);
            } else {
                //TODO: WRITE_SETTINGS does not work with requestPermissions
                //TODO: figure out which permissions this works for
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_SETTINGS}, CODE_WRITE_SETTINGS_PERMISSION);
            }
        } else {
            setBrightness(R.integer.startingBrightness);
        }

        //logic to handle seekbar
        _seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String hex = Integer.toHexString(progress);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                _mainLayout.setBackgroundColor(Color.parseColor("#" + hex + hex + hex));

                if (_canChangeScreenBrightness) {
                    setBrightness(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && requestCode == CODE_WRITE_SETTINGS_PERMISSION && Settings.System.canWrite(this)){
            _canChangeScreenBrightness = true;
            setBrightness(R.integer.startingBrightness);
        }
    }

    //TODO: this method will never get called for WRITE_SETTINGS
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_WRITE_SETTINGS_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            _canChangeScreenBrightness = true;
        }
    }

    public void setBrightness(int brightness){
        //constrain the value of brightness
        if(brightness < 0)
            brightness = 0;
        else if(brightness > R.integer.brightnessMax)
            brightness = R.integer.brightnessMax;

        try {
            //make sure brightness is set to manual mode
            int brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);

            if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }

            //set brightness
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
        } catch (Exception e) {
            Log.e("Error", "Cannot access screen brightness");
            e.printStackTrace();
        }
    }
}
