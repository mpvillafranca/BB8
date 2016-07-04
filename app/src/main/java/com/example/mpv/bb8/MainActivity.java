package com.example.mpv.bb8;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.calibration.api.CalibrationEventListener;
import com.orbotix.calibration.api.CalibrationImageButtonView;
import com.orbotix.calibration.api.CalibrationView;
import com.orbotix.common.ResponseListener;
import com.orbotix.common.Robot;
import com.orbotix.common.internal.AsyncMessage;
import com.orbotix.common.internal.DeviceResponse;
import com.orbotix.joystick.api.JoystickEventListener;
import com.orbotix.joystick.api.JoystickView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private Timer updateTimer;

    // The joystick that we will use to send roll commands to the robot
    private JoystickView _joystick;

    // The calibration view, used for setting the default heading of the robot
    private CalibrationView _calibrationView;

    //A button used for one finger calibration
    private CalibrationImageButtonView _calibrationButtonView;

    private TextView life;
    private TextView score;

    private long startGameTime;
    private long endGameTime;
    private int scoreCounter;
    private int lifeCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateTimer = new Timer();
        lifeCounter = 100;
        scoreCounter = 0;

        life = (TextView) findViewById(R.id.life);
        score = (TextView) findViewById(R.id.score);
        life.setText("Life: " + String.valueOf(lifeCounter));
        score.setText("Score: " + String.valueOf(score));

        Game();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateTimer.schedule(new UpdateTask(new Handler(), this), 0, 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateTimer.purge();
    }

    private void update(){
        endGameTime = System.currentTimeMillis() - startGameTime;
        scoreCounter = (int) (endGameTime / 1000);

        score.setText("Score: " + String.valueOf(scoreCounter));
    }

    private class UpdateTask extends TimerTask {
        Handler handler;
        MainActivity ref;

        public UpdateTask(Handler handler, MainActivity ref){
            super();
            this.handler = handler;
            this.ref = ref;
        }

        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ref.update();
                }
            });
        }
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

        // In order to get the life from the joystick, you need to implement the JoystickEventListener interface
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
        // Register anonymously for the calibration life here. You could also have this class implement the interface
        // manually if you plan to do more with the callbacks.
        _calibrationView.setCalibrationEventListener(new CalibrationEventListener() {
            /**
             * Invoked when the user begins the calibration process.
             */
            @Override
            public void onCalibrationBegan() {
                // The easy way to set up the robot for calibration is to use ConvenienceRobot#calibrating(true)
                if(BB8ConnectionActivity.getRobot() != null){
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

        // Here, you need to route all the touch life to the joystick and calibration view so that they know about
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

                    lifeCounter = lifeCounter - Math.round(c);
                    if (lifeCounter < 0)
                        lifeCounter = 0;
                    life.setText("Life: " + String.valueOf(lifeCounter));

                    checkIfGameOver();
                }
            }
        });
    }

    private void checkIfGameOver(){
        if(lifeCounter == 0){
            endGameTime = System.currentTimeMillis() - startGameTime;
            scoreCounter = (int) (endGameTime / 1000);

            Intent gameOverIntent = new Intent(MainActivity.this, GameOverActivity.class);
            gameOverIntent.putExtra("score", scoreCounter);
            startActivity(gameOverIntent);
        }
    }

}