package com.example.mpv.bb8;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.orbotix.ConvenienceRobot;
import com.orbotix.Sphero;
import com.orbotix.common.DiscoveryAgentEventListener;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.le.RobotRadioDescriptor;

import java.util.List;

public class MainActivity extends Activity {
    private DiscoveryAgentLE _discoveryAgent;
    private ConvenienceRobot _robot;
    private static final float ROBOT_VELOCITY = 0.6f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startDiscovery();
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
                    Log.i("Sphero", "Robot " + robot.getName() + " Online!");
                    _robot = new Sphero(robot);
                    stopDiscovery();

                    _robot.setLed(0.f, 1.f, 0.f);

                    //_robot.drive(90, 0);

                    //_robot.stop();
                case Connecting:
                    Log.i("Sphero", "Robot " + robot.getName() + " Connecting!");
                    break;
                case Connected:
                    Log.i("Sphero", "Robot " + robot.getName() + " Connected!");
                    break;
                // Handle other cases
            }
        }
    };

    private void startDiscovery() {
        _discoveryAgent = DiscoveryAgentLE.getInstance();
        _discoveryAgent.addDiscoveryListener(_discoveryAgentEventListener);
        _discoveryAgent.addRobotStateListener(_robotStateListener);

        RobotRadioDescriptor robotRadioDescriptor = new RobotRadioDescriptor();
        robotRadioDescriptor.setNamePrefixes(new String[]{"BB-"});
        _discoveryAgent.setRadioDescriptor(robotRadioDescriptor);

        try {
            _discoveryAgent.startDiscovery(this);
        } catch (DiscoveryException e) {
            Log.e("Sphero", "Discovery Error: " + e);
            e.printStackTrace();
        }
    }

    private void stopDiscovery() {
        _discoveryAgent.stopDiscovery();
        _discoveryAgent.removeDiscoveryListener(_discoveryAgentEventListener);
        _discoveryAgent.removeRobotStateListener(_robotStateListener);
        _discoveryAgent = null;
    }

}