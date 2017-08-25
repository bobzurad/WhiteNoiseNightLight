package net.zurad.bob.whitenoisenightlight;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.media.SoundPool;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    LinearLayout _mainLayout;
    LinearLayout _controlBrightnessLayout;
    LinearLayout _flashlightLayout;
    SeekBar _seekBar;
    Switch _controlBrightnessSwitch;
    Switch _whiteNoiseSwitch;
    Switch _flashlightSwitch;
    TextView _controlBrightnessTextView;
    TextView _brightnessTextView;
    TextView _whiteNoiseTextView;
    TextView _flashlightTextView;
    SoundPool _soundPool;
    ActionBar _bar;
    SurfaceView _surfaceView;
    SurfaceHolder _surfaceHolder;

    //Camera for Android versions less than 6
    Camera _camera;

    //Camera for Android 6 and up
    CameraManager _cameraManager;
    String _cameraId;


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
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        _bar = getSupportActionBar();
        _mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        _controlBrightnessLayout = (LinearLayout) findViewById(R.id.controlBrightnessLayout);
        _flashlightLayout = (LinearLayout) findViewById(R.id.flashlightLayout);
        _seekBar = (SeekBar) findViewById(R.id.seekBar);
        _controlBrightnessSwitch = (Switch) findViewById(R.id.controllBrightnessSwitch);
        _whiteNoiseSwitch = (Switch) findViewById(R.id.whiteNoiseSwitch);
        _flashlightSwitch = (Switch) findViewById(R.id.flashlightSwitch);
        _brightnessTextView = (TextView) findViewById(R.id.brightnessTextView);
        _controlBrightnessTextView = (TextView) findViewById(R.id.controlBrightnessTextView);
        _whiteNoiseTextView = (TextView) findViewById(R.id.whiteNoiseTextView);
        _flashlightTextView = (TextView) findViewById(R.id.flashlightTextView);
        _soundPool = createSoundPool();
        _soundId = _soundPool.load(this, R.raw.whitenoise, 1);
        _soundPool.setLoop(_soundId, -1);
        _bar.setTitle(fromHtml("<font color='#000000'>&nbsp;&nbsp;" + getString(R.string.app_name) + "</font>"));
        _bar.setDisplayShowHomeEnabled(true);
        _bar.setLogo(R.drawable.ic_action_bar_tab_light);
        _bar.setDisplayUseLogoEnabled(true);

        //for camera flash
        _hasFlashlight = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (_hasFlashlight) {
            _flashlightLayout.setVisibility(View.VISIBLE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                _surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
                _surfaceHolder = _surfaceView.getHolder();
                _surfaceHolder.addCallback(this);
                _camera = Camera.open();
                try {
                    _camera.setPreviewDisplay(_surfaceHolder);
                } catch (java.io.IOException ex) {
                    if (BuildConfig.DEBUG) {
                        Log.e("onCreate", ex.getMessage());
                    }
                }
            }
        }

        //save starting brightness values
        try {
            _brightnessOnAppStart = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            _brightnessModeOnAppStart = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException ex) {
            if (BuildConfig.DEBUG) {
                Log.e("Error", ex.getMessage());
            }
        }

        //check permissions to change screen brightness
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            _canChangeScreenBrightness = Settings.System.canWrite(this);
        } else {
            _canChangeScreenBrightness = ContextCompat
                    .checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        if (_canChangeScreenBrightness) {
            _controlBrightnessSwitch.setChecked(true);
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
                        _bar.setTitle(fromHtml("<font color='#C0C0C0'>&nbsp;&nbsp;" + getString(R.string.app_name) + "</font>"));
                        _brightnessTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.textColorLight));
                        _controlBrightnessTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.textColorLight));
                        _whiteNoiseTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.textColorLight));
                        _flashlightTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.textColorLight));
                        _bar.setLogo(R.drawable.ic_action_bar_tab_dark);
                    } else {
                        _bar.setTitle(fromHtml("<font color='#000000'>&nbsp;&nbsp;" + getString(R.string.app_name) + "</font>"));
                        _brightnessTextView.setTextColor(Color.BLACK);
                        _controlBrightnessTextView.setTextColor(Color.BLACK);
                        _whiteNoiseTextView.setTextColor(Color.BLACK);
                        _flashlightTextView.setTextColor(Color.BLACK);
                        _bar.setLogo(R.drawable.ic_action_bar_tab_light);
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

        //logic for control screen brightness switch
        _controlBrightnessSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                if (isOn) {
                    if (!_canChangeScreenBrightness
                            && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        //dialog that explains user must allow special permission
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle("Permission Requested");
                        alertDialog.setMessage(getString(R.string.permission_alert_text));
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        _controlBrightnessSwitch.setChecked(false);
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        //launch intent for permission to change screen brightness
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                        intent.setData(Uri.parse("package:" + getPackageName()));
                                        startActivityForResult(intent, CODE_WRITE_SETTINGS_PERMISSION);
                                    }
                                });
                        alertDialog.show();
                    } else if (_canChangeScreenBrightness) {
                        setBrightness(_seekBar.getProgress());
                    }
                } else if (_canChangeScreenBrightness) {
                    //put brightness and mode values back to what they were when the app started
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, _brightnessOnAppStart);
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, _brightnessModeOnAppStart);
                }
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
                if (_hasFlashlight) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        //for Android versions less than 6
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
                        }
                    } else {
                        //for Android 6 and up
                        if (isOn) {
                            try {
                                _cameraManager.setTorchMode(_cameraId, true);   //Turn ON
                            } catch (CameraAccessException e) {
                                if (BuildConfig.DEBUG) {
                                    Log.e("flashlight switch", e.getMessage());
                                }
                            }
                        } else {
                            try {
                                _cameraManager.setTorchMode(_cameraId, false);
                            } catch (CameraAccessException e) {
                                if (BuildConfig.DEBUG) {
                                    Log.e("flashlight switch", e.getMessage());
                                }
                            }
                        }
                    }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            _cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                _cameraId = _cameraManager.getCameraIdList()[0];
            } catch (CameraAccessException ex) {
                if (BuildConfig.DEBUG) {
                    Log.e("onResume", ex.getMessage());
                }
            }
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
        } else {
            _controlBrightnessSwitch.setChecked(false);
        }
    }

    public void setBrightness(int brightness) {
        //constrain the value of brightness
        if (brightness < 0)
            brightness = 0;
        else if (brightness > getResources().getInteger(R.integer.brightnessMax))
            brightness = getResources().getInteger(R.integer.brightnessMax);

        if (_controlBrightnessSwitch.isChecked()) {
            try {
                //make sure brightness is set to manual mode
                int brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);

                if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                }

                //set brightness
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
            } catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    Log.e("Error", "Cannot access screen brightness");
                    e.printStackTrace();
                }
            }
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

    //region SurfaceHolder.Callback implementation (for flashlight on Android versions less than 6)
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    public void surfaceCreated(SurfaceHolder holder) {
        if (_hasFlashlight && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            _surfaceHolder = holder;

            try {
                _camera.setPreviewDisplay(_surfaceHolder);
            } catch (java.io.IOException ex) {
                if (BuildConfig.DEBUG) {
                    Log.e("surfaceCreated", ex.getMessage());
                }
            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (_hasFlashlight && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            _camera.stopPreview();
            _surfaceHolder = null;
        }
    }
    //endregion
}
