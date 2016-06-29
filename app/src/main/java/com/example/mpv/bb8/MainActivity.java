package com.example.mpv.bb8;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.orbotix.ConvenienceRobot;
import com.orbotix.Sphero;
import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.calibration.api.CalibrationEventListener;
import com.orbotix.calibration.api.CalibrationImageButtonView;
import com.orbotix.calibration.api.CalibrationView;
import com.orbotix.common.DiscoveryAgentEventListener;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.ResponseListener;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.common.internal.AsyncMessage;
import com.orbotix.common.internal.DeviceResponse;
import com.orbotix.joystick.api.JoystickEventListener;
import com.orbotix.joystick.api.JoystickView;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.le.RobotRadioDescriptor;

import java.util.List;

public class MainActivity extends Activity {

    private Handler mHandler = new Handler();

    private static final String TAG = "MainActivity";


    // Our current discovery agent that we will use to find BB8s
    private DiscoveryAgentLE _discoveryAgent;

    // The connected robot
    private ConvenienceRobot _robot;

    // The joystick that we will use to send roll commands to the robot
    private JoystickView _joystick;

    // The calibration view, used for setting the default heading of the robot
    private CalibrationView _calibrationView;

    //A button used for one finger calibration
    private CalibrationImageButtonView _calibrationButtonView;

    private TextView events;
    private TextView score;

    private long startGameTime;
    private long endGameTime;


    private int life;

    private static final float ROBOT_VELOCITY = 0.6f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        life = 100;

        events = (TextView) findViewById(R.id.events);
        events = (TextView) findViewById(R.id.score);
        events.setText(Integer.toString(life));

        Game();
    }

    /**
     * onPause we sleep the robot
     */
    @Override
    protected void onPause() {
        super.onPause();

        BB8ConnectionActivity.getRobot().sleep();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkIfGameOver();
    }

    /**
     * Sets up the joystick from scratch
     */
    private void setupJoystick() {
        // Get a reference to the joystick view so that we can use it to send roll commands
        _joystick = (JoystickView) findViewById(R.id.joystickView);

        // In order to get the events from the joystick, you need to implement the JoystickEventListener interface
        // (or declare it anonymously) and set the listener.
        _joystick.setJoystickEventListener(new JoystickEventListener() {
            /**
             * Invoked when the user starts touching on the joystick
             */
            @Override
            public void onJoystickBegan() {
                // Here you can do something when the user starts using the joystick.
            }

            /**
             * Invoked when the user moves their finger on the joystick
             *
             * @param distanceFromCenter The distance from the center of the joystick that the user is touching from 0.0 to 1.0
             *                           where 0.0 is the exact center, and 1.0 is the very edge of the outer ring.
             * @param angle              The angle from the top of the joystick that the user is touching.
             */
            @Override
            public void onJoystickMoved(double distanceFromCenter, double angle) {
                // Here you can use the joystick input to drive the connected robot. You can easily do this with the
                // ConvenienceRobot#drive() method
                // Note that the arguments do flip here from the order of parameters
                if(BB8ConnectionActivity.getRobot() != null)
                    BB8ConnectionActivity.getRobot().drive((float) angle, (float) distanceFromCenter);
            }

            /**
             * Invoked when the user stops touching the joystick
             */
            @Override
            public void onJoystickEnded() {
                // Here you can do something when the user stops touching the joystick. For example, we'll make it stop driving.
                if(BB8ConnectionActivity.getRobot() != null)
                    BB8ConnectionActivity.getRobot().stop();
            }
        });
    }

    /**
     * Sets up the calibration gesture and button
     */
    private void setupCalibration() {
        // Get the view from the xml file
        _calibrationView = (CalibrationView)findViewById(R.id.calibrationView);
        // Set the glow. You might want to not turn this on if you're using any intense graphical elements.
        _calibrationView.setShowGlow(true);
        // Register anonymously for the calibration events here. You could also have this class implement the interface
        // manually if you plan to do more with the callbacks.
        _calibrationView.setCalibrationEventListener(new CalibrationEventListener() {
            /**
             * Invoked when the user begins the calibration process.
             */
            @Override
            public void onCalibrationBegan() {
                // The easy way to set up the robot for calibration is to use ConvenienceRobot#calibrating(true)
                if(BB8ConnectionActivity.getRobot() != null){
                    Log.v(TAG, "Calibration began!");
                    BB8ConnectionActivity.getRobot().calibrating(true);
                }
            }

            /**
             * Invoked when the user moves the calibration ring
             * @param angle The angle that the robot has rotated to.
             */
            @Override
            public void onCalibrationChanged(float angle) {
                // The usual thing to do when calibration happens is to send a roll command with this new angle, a speed of 0
                // and the calibrate flag set.
                if(BB8ConnectionActivity.getRobot() != null)
                    BB8ConnectionActivity.getRobot().rotate(angle);
            }

            /**
             * Invoked when the user stops the calibration process
             */
            @Override
            public void onCalibrationEnded() {
                // This is where the calibration process is "committed". Here you want to tell the robot to stop as well as
                // stop the calibration process.
                if(BB8ConnectionActivity.getRobot() != null) {
                    BB8ConnectionActivity.getRobot().stop();
                    BB8ConnectionActivity.getRobot().calibrating(false);
                }
            }
        });
        // Like the joystick, turn this off until a robot connects.
        _calibrationView.setEnabled(false);

        // To set up the button, you need a calibration view. You get the button view, and then set it to the
        // calibration view that we just configured.
        _calibrationButtonView = (CalibrationImageButtonView) findViewById(R.id.calibrateButton);
        _calibrationButtonView.setCalibrationView(_calibrationView);
        _calibrationButtonView.setEnabled(false);
    }


    private void Game(){
        setupJoystick();
        setupCalibration();

        // Here, you need to route all the touch events to the joystick and calibration view so that they know about
        // them. To do this, you need a way to reference the view (in this case, the id "entire_view") and attach
        // an onTouchListener which in this case is declared anonymously and invokes the
        // Controller#interpretMotionEvent() method on the joystick and the calibration view.
        findViewById(R.id.entire_view).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                _joystick.interpretMotionEvent(event);
                _calibrationView.interpretMotionEvent(event);
                return true;
            }
        });

        // Don't forget to turn on UI elements
        _joystick.setEnabled(true);
        _calibrationView.setEnabled(true);
        _calibrationButtonView.setEnabled(true);

        startGameTime = System.currentTimeMillis();

        BB8ConnectionActivity.getRobot().addResponseListener(new ResponseListener() {
            @Override
            public void handleResponse(DeviceResponse deviceResponse, Robot robot) {

            }

            @Override
            public void handleStringResponse(String s, Robot robot) {

            }

            @Override
            public void handleAsyncMessage(AsyncMessage asyncMessage, Robot robot) {
                if (asyncMessage == null)
                    return;

                //Check the asyncMessage type to see if it is a DeviceSensor message
                if (asyncMessage instanceof CollisionDetectedAsyncData) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BB8ConnectionActivity.getRobot().setLed(1f, 0f, 0f);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            BB8ConnectionActivity.getRobot().setLed(0f, 1f, 0f);
                        }
                    }
                    ).start();
                    final CollisionDetectedAsyncData collisionData = (CollisionDetectedAsyncData) asyncMessage;
                    float collisionSpeed = ((CollisionDetectedAsyncData) asyncMessage).getImpactSpeed();
                    float c = (collisionSpeed * 10);

                    life = life - Math.round(c);
                    if (life < 0)
                        life = 0;
                    events.setText(String.valueOf(life));

                    checkIfGameOver();
                }
            }
        });
    }

    private void checkIfGameOver(){
        if(life == 0){
            endGameTime = System.currentTimeMillis() - startGameTime;
            int sec = (int) (endGameTime / 1000);

            Intent gameOverIntent = new Intent(MainActivity.this, GameOverActivity.class);
            gameOverIntent.putExtra("score", sec);
            startActivity(gameOverIntent);
        }
    }

}