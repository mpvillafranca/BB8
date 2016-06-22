package com.example.mpv.bb8;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

public class MainActivity extends Activity implements View.OnClickListener, RobotChangedStateListener, ResponseListener {

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
        DiscoveryAgentLE.getInstance().addRobotStateListener( this );

        events = (TextView) findViewById(R.id.events);
        events = (TextView) findViewById(R.id.score);
        events.setText(Integer.toString(life));

        if( Build.VERSION.SDK_INT >= 23){
            // Bluetooth permissions
        }

        startDiscovery();
    }

    /**
     * onPause we sleep the robot
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (_discoveryAgent != null) {
            // When pausing, you want to make sure that you let go of the connection to the robot so that it may be
            // accessed from within other applications. Before you do that, it is a good idea to unregister for the robot
            // state change events so that you don't get the disconnection event while the application is closed.
            // This is accomplished by using DiscoveryAgent#removeRobotStateListener().
            _discoveryAgent.removeRobotStateListener(this);

            // Here we are only handling disconnecting robots if the user selected a type of robot to connect to. If you
            // didn't use the robot picker, you will need to check the appropriate discovery agent manually by using
            // DiscoveryAgent.getInstance().getConnectedRobots()
            for (Robot r : _discoveryAgent.getConnectedRobots()) {
                // There are a couple ways to disconnect a robot: sleep and disconnect. Sleep will disconnect the robot
                // in addition to putting it into standby mode. If you choose to just disconnect the robot, it will
                // use more power than if it were in standby mode. In the case of Ollie, the main LED light will also
                // turn a bright purple, indicating that it is on but disconnected. Unless you have a specific reason
                // to leave a robot on but disconnected, you should use Robot#sleep()
                r.sleep();
            }
        }
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
                if(_robot != null)
                    _robot.drive((float) angle, (float) distanceFromCenter);
            }

            /**
             * Invoked when the user stops touching the joystick
             */
            @Override
            public void onJoystickEnded() {
                // Here you can do something when the user stops touching the joystick. For example, we'll make it stop driving.
                if(_robot != null)
                    _robot.stop();
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
                if(_robot != null){
                    Log.v(TAG, "Calibration began!");
                    _robot.calibrating(true);
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
                if(_robot != null)
                    _robot.rotate(angle);
            }

            /**
             * Invoked when the user stops the calibration process
             */
            @Override
            public void onCalibrationEnded() {
                // This is where the calibration process is "committed". Here you want to tell the robot to stop as well as
                // stop the calibration process.
                if(_robot != null) {
                    _robot.stop();
                    _robot.calibrating(false);
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

    private DiscoveryAgentEventListener _discoveryAgentEventListener = new DiscoveryAgentEventListener() {
        @Override
        public void handleRobotsAvailable(List<Robot> robots) {
            Log.i("Sphero", "Found " + robots.size() + " robots");

            for (Robot robot : robots) {
                Log.i("Sphero", "  " + robot.getName());
            }
        }

    };

    private RobotChangedStateListener _robotStateListener = new RobotChangedStateListener() {
        @Override
        public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType robotChangedStateNotificationType) {
            switch (robotChangedStateNotificationType) {
                case Online:
                    Toast.makeText(getApplicationContext(), robot.getName() + " is now Online!", Toast.LENGTH_SHORT).show();

                    stopDiscovery();

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

                    Log.i("Sphero", "Robot " + robot.getName() + " Online!");
                    _robot = new Sphero(robot);

                    startGameTime = System.currentTimeMillis();


                            // Finally for visual feedback let's turn the robot green saying that it's been connected
                    _robot.setLed(0f, 1f, 0f);

                    _robot.enableCollisions(true); // Enabling the collisions detector
                    _robot.addResponseListener(new ResponseListener() {
                        @Override
                        public void handleResponse(DeviceResponse deviceResponse, Robot robot) {

                        }

                        @Override
                        public void handleStringResponse(String s, Robot robot) {

                        }

                        @Override
                        public void handleAsyncMessage(AsyncMessage asyncMessage, Robot robot) {
                            if( asyncMessage == null )
                                return;

                            //Check the asyncMessage type to see if it is a DeviceSensor message
                            if( asyncMessage instanceof CollisionDetectedAsyncData) {
                                //Toast.makeText(getApplicationContext(),"Colision detectadaaaaa",Toast.LENGTH_LONG).show();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        _robot.setLed(1f, 0f, 0f);
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        _robot.setLed(0f, 1f, 0f);
                                    }
                                }
                                ).start();
                                final CollisionDetectedAsyncData collisionData = (CollisionDetectedAsyncData) asyncMessage;
                                float collisionSpeed =((CollisionDetectedAsyncData) asyncMessage).getImpactSpeed();
                               // Toast.makeText(getApplicationContext(),Float.toString(collisionSpeed),Toast.LENGTH_SHORT).show();
                                float c= (collisionSpeed * 10);
                                //Toast.makeText(getApplicationContext(),Float.toString(c),Toast.LENGTH_SHORT).show();
                                //Toast.makeText(getApplicationContext(),Integer.toString(Math.round(c)),Toast.LENGTH_SHORT).show();
                                life=life-Math.round(c);
                                if(life<0)
                                    life=0;
                                Toast.makeText(getApplicationContext(),Integer.toString(life),Toast.LENGTH_SHORT).show();
                                events.setText(Integer.toString(life));
                                if(life==0) {
                                    Toast.makeText(getApplicationContext(), "YOUUU ARE DEADDDDDD", Toast.LENGTH_SHORT).show();
                                    endGameTime = System.currentTimeMillis() - startGameTime;
                                    int sec=(int)(endGameTime/1000);
                                    Toast.makeText(getApplicationContext(),"Tiempo: "+ Integer.toString(sec), Toast.LENGTH_SHORT).show();
                                    //score.setText(Integer.toString(sec));
                                }


                                // eventos.setText(((CollisionDetectedAsyncData) asyncMessage).getImpactPower().toString());
                                //events.setText(asyncMessage.toString());
                            }
                        }
                    });
                    break;
                case Offline:
                    break;
                case Connecting:
                    Toast.makeText(getApplicationContext(), "Connecting to " + robot.getName(), Toast.LENGTH_SHORT).show();
                    break;
                case Connected:
                    Toast.makeText(getApplicationContext(), "Connected to " + robot.getName(), Toast.LENGTH_SHORT).show();
                    break;
                // Handle other cases
                case Disconnected:
                    // When a robot disconnects, it is a good idea to disable UI elements that send commands so that you
                    // do not have to handle the user continuing to use them while the robot is not connected
                    _joystick.setEnabled(false);
                    _calibrationView.setEnabled(false);
                    _calibrationButtonView.setEnabled(false);
                    break;
                case FailedConnect:
                    break;
            }
        }
    };

    private void startDiscovery() {
        try {
            _discoveryAgent = DiscoveryAgentLE.getInstance();

            // You first need to set up so that the discovery agent will notify you when it finds robots.
            // To do this, you need to implement the DiscoveryAgentEventListener interface (or declare
            // it anonymously) and then register it on the discovery agent with DiscoveryAgent#addDiscoveryListener()
            _discoveryAgent.addDiscoveryListener(_discoveryAgentEventListener);

            // Second, you need to make sure that you are notified when a robot changes state. To do this,
            // implement RobotChangedStateListener (or declare it anonymously) and use
            // DiscoveryAgent#addRobotStateListener()
            _discoveryAgent.addRobotStateListener(_robotStateListener);

            // Creating a new radio descriptor to be able to connect to the BB8 robots
            RobotRadioDescriptor robotRadioDescriptor = new RobotRadioDescriptor();
            robotRadioDescriptor.setNamePrefixes(new String[]{"BB-"});
            _discoveryAgent.setRadioDescriptor(robotRadioDescriptor);

            // Then to start looking for a BB8, you use DiscoveryAgent#startDiscovery()
            // You do need to handle the discovery exception. This can occur in cases where the user has
            // Bluetooth off, or when the discovery cannot be started for some other reason.
            _discoveryAgent.startDiscovery(this);
        } catch (DiscoveryException e) {
            Log.e("Sphero", "Discovery Error: " + e);
            e.printStackTrace();
        }
    }

    private void stopDiscovery() {
        // When a robot is connected, this is a good time to stop discovery. Discovery takes a lot of system
        // resources, and if left running, will cause your app to eat the user's battery up, and may cause
        // your application to run slowly. To do this, use DiscoveryAgent#stopDiscovery().
        _discoveryAgent.stopDiscovery();

        // It is also proper form to not allow yourself to re-register for the discovery listeners, so let's
        // unregister for the available notifications here using DiscoveryAgent#removeDiscoveryListener().
        _discoveryAgent.removeDiscoveryListener(_discoveryAgentEventListener);
        _discoveryAgent.removeRobotStateListener(_robotStateListener);
        _discoveryAgent = null;
    }

    @Override
    public void handleResponse(DeviceResponse deviceResponse, Robot robot) {

    }

    @Override
    public void handleStringResponse(String s, Robot robot) {

    }

    @Override
    public void handleAsyncMessage(AsyncMessage asyncMessage, Robot robot) {

    }

    @Override
    public void onClick(View v) {
        if (_robot == null)
            return;

        switch (v.getId()) {


            /*case R.id.procedimiento1: {
                eventos.setText("Giro Loco Loco");
                    _robot.drive(45, 0);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(90, 0);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(135, 0);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(180, 0);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(225, 0);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(270, 0);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(324, 0);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(360, 0);
                    android.os.SystemClock.sleep(75);
                    //_robot.drive(0, 0);
                    //android.os.SystemClock.sleep(75);
                   // _robot.drive(360, 0);
                    //android.os.SystemClock.sleep(75);


                break;
            }
            case R.id.procedimiento2: {
                eventos.setText("Giro Loco Loco 2");
                _robot.enableStabilization(false);
                _robot.addResponseListener(this);

                for (int i=0;i<15;i++) {
                    _robot.drive(45, 0.0000f);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(90, 0.0000f);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(135, 0.0000f);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(180, 0.0000f);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(225, 0.0000f);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(270, 0.0000f);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(324, 0.0000f);
                    android.os.SystemClock.sleep(75);
                    _robot.drive(360, 0.0000f);
                    android.os.SystemClock.sleep(75);

                }
                break;
            }
            case R.id.procedimiento3:
                eventos.setText("AVANCE INFINITO!");
                for(int i=0;i<20;i++){
                    _robot.drive(0, 0.6f);
                    android.os.SystemClock.sleep(75);
                }
                break;
            case R.id.procedimiento4:
                eventos.setText("HALT!");
                //_robot.drive(180, 0.6f);
                android.os.SystemClock.sleep(35);
                for(int i=0;i<20;i++){
                    _robot.drive(180, 0.6f);
                    android.os.SystemClock.sleep(75);
                }
                break;

            case R.id.detener:
                eventos.setText("HALT!");
                _robot.drive(360, 0);
                break;*/
        }
    }


    @Override
    public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType robotChangedStateNotificationType) {

    }
}