package net.zurad.bob.whitenoisenightlight;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.media.SoundPool;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    LinearLayout _mainLayout;
    SeekBar _seekBar;
    Switch _whiteNoiseSwitch;
    Switch _flashlightSwitch;
    TextView _brightnessTextView;
    TextView _whiteNoiseTextView;
    TextView _flashlightTextView;
    SoundPool _soundPool;
    ActionBar _bar;
    SurfaceView _surfaceView;
    SurfaceHolder _surfaceHolder;
    Camera _camera;

    int _soundId;
    int _playId;
    boolean _canChangeScreenBrightness;
    int _brightnessOnAppStart;
    int _brightnessModeOnAppStart;
    boolean _hasFlashlight;

    final int CODE_WRITE_SETTINGS_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _bar = getSupportActionBar();
        _mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        _seekBar = (SeekBar) findViewById(R.id.seekBar);
        _whiteNoiseSwitch = (Switch) findViewById(R.id.whiteNoiseSwitch);
        _flashlightSwitch = (Switch) findViewById(R.id.flashlightSwitch);
        _brightnessTextView = (TextView) findViewById(R.id.brightnessTextView);
        _whiteNoiseTextView = (TextView) findViewById(R.id.whiteNoiseTextView);
        _flashlightTextView = (TextView) findViewById(R.id.flashlightTextView);
        _soundPool = createSoundPool();
        _soundId = _soundPool.load(this, R.raw.whitenoise, 1);
        _soundPool.setLoop(_soundId, -1);
        _bar.setTitle(fromHtml("<font color='#000000'>White Noise Night Light</font>"));

        //for camera flash
        _hasFlashlight = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        _surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        _surfaceHolder = _surfaceView.getHolder();
        _surfaceHolder.addCallback(this);
        _camera = Camera.open();
        try {
            _camera.setPreviewDisplay(_surfaceHolder);
        } catch (java.io.IOException ex) {}

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
            }
        } else {
            setBrightness(_seekBar.getProgress());
        }

        //logic to handle seekbar
        _seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //build hex color
                String hex = Integer.toHexString(progress);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                //change color of ActionBar
                if (_bar != null) {
                    _bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#" + hex + hex + hex)));
                    if (progress < 80) {
                        _bar.setTitle(fromHtml("<font color='#C0C0C0'>White Noise Night Light</font>"));
                        _brightnessTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.textColorLight));
                        _whiteNoiseTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.textColorLight));
                        _flashlightTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.textColorLight));
                    } else {
                        _bar.setTitle(fromHtml("<font color='#000000'>White Noise Night Light</font>"));
                        _brightnessTextView.setTextColor(Color.BLACK);
                        _whiteNoiseTextView.setTextColor(Color.BLACK);
                        _flashlightTextView.setTextColor(Color.BLACK);
                    }
                }
                //change color of background
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
                        _playId = _soundPool.play(_soundId, 0.5f, 0.5f, 1, -1, 1.0f);
                    }
                } else {
                    _soundPool.pause(_playId);
                }
            }
        });

        //logic for flashlight switch
        _flashlightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isOn) {
                if (isOn) {
                    Camera.Parameters params = _camera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    _camera.setParameters(params);
                    _camera.startPreview();
                } else {
                    Camera.Parameters params = _camera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    _camera.setParameters(params);
                    _camera.stopPreview();
                    _camera.release();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (_canChangeScreenBrightness) {
            setBrightness(_seekBar.getProgress());
        }
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

    @SuppressWarnings("deprecation")
    private Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    //region SurfaceHolder.Callback implementation
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    public void surfaceCreated(SurfaceHolder holder) {
        _surfaceHolder = holder;
        try {
            _camera.setPreviewDisplay(_surfaceHolder);
        } catch (java.io.IOException e) {}
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        _camera.stopPreview();
        _surfaceHolder = null;
    }
    //endregion
}
