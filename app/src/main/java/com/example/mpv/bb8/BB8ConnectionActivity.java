package com.example.mpv.bb8;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.orbotix.ConvenienceRobot;
import com.orbotix.Sphero;
import com.orbotix.common.DiscoveryAgentEventListener;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.le.RobotRadioDescriptor;

import java.util.List;

public class BB8ConnectionActivity extends AppCompatActivity implements RobotChangedStateListener{

    private TextView daStatus;
    private TextView robotStatus;
    private Button newGameButton;

    // Bluetooth adapter (we need to keep checking if user disable it
    private BluetoothAdapter mBluetoothAdapter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        reactivateBluetoothOrLocation();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    // Our current discovery agent that we will use to find BB8s
    private DiscoveryAgentLE _discoveryAgent;

    // The connected robot
    private static ConvenienceRobot _robot;

    // The Discovery Agent listener to handle robots availables
    private DiscoveryAgentEventListener _discoveryAgentEventListener = new DiscoveryAgentEventListener() {
        @Override
        public void handleRobotsAvailable(List<Robot> robots) {
            daStatus.setText("Found " + robots.size() + " robots");

            for (Robot robot : robots) {
                daStatus.setText(daStatus.getText().toString() + "\n" + robot.getName());
            }
        }

    };

    // The Robot listener to handle state changes
    private RobotChangedStateListener _robotStateListener = new RobotChangedStateListener() {
        @Override
        public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType robotChangedStateNotificationType) {
            switch (robotChangedStateNotificationType) {
                case Online:
                    stopDiscovery();


                    robotStatus.setText("Robot " + robot.getName() + " is Online!");
                    _robot = new Sphero(robot);

                    // Finally for visual feedback let's turn the robot green saying that it's been connected
                    _robot.setLed(0f, 1f, 0f);

                    _robot.enableCollisions(true); // Enabling the collisions detector

                    newGameButton.setEnabled(true);
                    newGameButton.setClickable(true);
                    break;
                case Offline:
                    robotStatus.setText("Robot " + robot.getName() + " is now Offline!");
                    newGameButton.setClickable(false);
                    newGameButton.setEnabled(false);
                    startDiscovery();
                    break;
                case Connecting:
                    robotStatus.setText("Connecting to " + robot.getName());
                    newGameButton.setClickable(false);
                    newGameButton.setEnabled(false);
                    break;
                case Connected:
                    robotStatus.setText("Connected to " + robot.getName());
                    newGameButton.setClickable(false);
                    newGameButton.setEnabled(false);
                    break;
                case Disconnected:
                    robotStatus.setText("Disconnected from " + robot.getName());
                    newGameButton.setClickable(false);
                    newGameButton.setEnabled(false);
                    startDiscovery();
                    break;
                case FailedConnect:
                    robotStatus.setText("Failed to connect to " + robot.getName());
                    newGameButton.setClickable(false);
                    newGameButton.setEnabled(false);
                    startDiscovery();
                    break;
            }
        }
    };

    // Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bb8_connection);

        daStatus = (TextView) findViewById(R.id.dastatus);
        robotStatus = (TextView) findViewById(R.id.robotstatus);
        newGameButton = (Button) findViewById(R.id.newgamebutton);

        newGameButton.setClickable(false);
        newGameButton.setEnabled(false);

        newGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent newGame = new Intent(BB8ConnectionActivity.this, MainActivity.class);
                startActivity(newGame);
            }
        });

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        // Check if bluetooth or location are still enabled
        reactivateBluetoothOrLocation();

        DiscoveryAgentLE.getInstance().addRobotStateListener(this);

        startDiscovery();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sleepRobotStuff();

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sleepRobotStuff();
    }

    @Override
    protected void onResume() {
        super.onResume();

        reactivateBluetoothOrLocation();
        startDiscovery();
    }

    // RobotChangedStateListener
    @Override
    public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType robotChangedStateNotificationType) {

    }

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

    private void reactivateBluetoothOrLocation(){
        // Check if location is still enabled
        final LocationManager mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(BB8ConnectionActivity.this, BluetoothActivity.class));
        }

        // Check if bluetooth is still enabled
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()){
            startActivity(new Intent(BB8ConnectionActivity.this, BluetoothActivity.class));
        }
    }

    public void sleepRobotStuff(){
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

    public static ConvenienceRobot getRobot(){
        return _robot;
    }
}
