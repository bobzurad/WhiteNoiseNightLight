package net.zurad.bob.whitenoisenightlight;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.media.SoundPool;

public class MainActivity extends AppCompatActivity {

    LinearLayout _mainLayout;
    SeekBar _seekBar;
    Switch _whiteNoiseSwitch;
    SoundPool _soundPool;
    int _soundId;
    int _playId;

    boolean _canChangeScreenBrightness;
    int _brightnessOnAppStart;
    int _brightnessModeOnAppStart;

    final int CODE_WRITE_SETTINGS_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        _seekBar = (SeekBar) findViewById(R.id.seekBar);
        _whiteNoiseSwitch = (Switch) findViewById(R.id.whiteNoiseSwitch);
        _soundPool = createSoundPool();
        _soundId = _soundPool.load(this, R.raw.whitenoise, 1);
        _soundPool.setLoop(_soundId, -1);

        //save starting brightness values
        try {
            _brightnessOnAppStart = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            _brightnessModeOnAppStart = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException ex) {
            Log.e("Error", ex.getMessage());
        }

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
            setBrightness(_seekBar.getProgress());
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
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //logic for white noise switch
        _whiteNoiseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                if (isOn) {
                    if (_playId > 0) {
                        _soundPool.resume(_playId);
                    } else {
                        _playId = _soundPool.play(_soundId, 1.0f, 1.0f, 1, -1, 1.0f);
                    }
                } else {
                    _soundPool.pause(_playId);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //allow screen to turn off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (_canChangeScreenBrightness) {
            //put brightness and mode values back to what they were when the app started
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, _brightnessOnAppStart);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, _brightnessModeOnAppStart);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && requestCode == CODE_WRITE_SETTINGS_PERMISSION && Settings.System.canWrite(this)) {
            _canChangeScreenBrightness = true;
            setBrightness(getResources().getInteger(R.integer.defaultBrightness));
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

    public void setBrightness(int brightness) {
        //constrain the value of brightness
        if (brightness < 0)
            brightness = 0;
        else if (brightness > getResources().getInteger(R.integer.brightnessMax))
            brightness = getResources().getInteger(R.integer.brightnessMax);

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

    protected SoundPool createSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return createNewSoundPool();
        } else {
            return createOldSoundPool();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected SoundPool createNewSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        return new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
    }

    @SuppressWarnings("deprecation")
    protected SoundPool createOldSoundPool() {
        return new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    }
}
