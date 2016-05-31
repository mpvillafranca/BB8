package com.example.mpv.bb8;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.orbotix.ConvenienceRobot;
import com.orbotix.Sphero;
import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.common.DiscoveryAgentEventListener;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.ResponseListener;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.common.internal.AsyncMessage;
import com.orbotix.common.internal.DeviceResponse;
import com.orbotix.common.sensor.DeviceSensorsData;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.le.RobotRadioDescriptor;
import com.orbotix.subsystem.SensorControl;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, RobotChangedStateListener, ResponseListener {
    private DiscoveryAgentLE _discoveryAgent;
    private ConvenienceRobot _robot;
    private Button procedimiento1;
    private Button procedimiento2;
    private Button procedimiento3;
    private Button procedimiento4;


    private Button detener;
    private TextView eventos;
    private TextView acelerometro;
    private TextView altitud;
    private TextView orientacion;
    private TextView back;
    private TextView giroscopio;







    private static final float ROBOT_VELOCITY = 0.6f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        procedimiento1 = (Button) findViewById(R.id.procedimiento1);
        procedimiento2 = (Button) findViewById(R.id.procedimiento2);
        procedimiento3 = (Button) findViewById(R.id.procedimiento3);
        procedimiento4 = (Button) findViewById(R.id.procedimiento4);


        eventos = (TextView)findViewById(R.id.evento);
        acelerometro = (TextView)findViewById(R.id.acelerometro);
        altitud = (TextView)findViewById(R.id.altitud);
        orientacion = (TextView)findViewById(R.id.orientacion);
        back = (TextView)findViewById(R.id.back);
        giroscopio = (TextView)findViewById(R.id.giro);

        detener = (Button) findViewById(R.id.detener);

        procedimiento1.setOnClickListener(this);
        procedimiento2.setOnClickListener(this);
        procedimiento3.setOnClickListener(this);
        procedimiento4.setOnClickListener(this);

        //

        detener.setOnClickListener(this);


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
                    eventos.setText("Conexion Establecida");
                    long sensorFlag = SensorFlag.QUATERNION.longValue()
                            | SensorFlag.ACCELEROMETER_NORMALIZED.longValue()
                            | SensorFlag.GYRO_NORMALIZED.longValue()
                            | SensorFlag.MOTOR_BACKEMF_NORMALIZED.longValue()
                            | SensorFlag.ATTITUDE.longValue();
                    //Enable sensors based on the flag defined above, and stream their data ten times a second to the mobile device
                    _robot.enableSensors(sensorFlag, SensorControl.StreamingRate.STREAMING_RATE10);
                    //Listen to data responses from the robot
                   // float contador=45;
                   // while(true) {
                     //   _robot.drive(contador, 0);
                        //android.os.SystemClock.sleep(500);
                        //_robot.drive(90, 0);
                        //android.os.SystemClock.sleep(500);
                        //_robot.drive(180, 0);
                       // contador+=25;
                       // android.os.SystemClock.sleep(250);
                        //_robot.drive(270, 0);
                   // }
                    /*for(float i=0;i<360;i+=45){
                        _robot.drive(i, 0.01f);
                    }*/

                    //_robot.drive(90, 0);

                    //_robot.stop();
                case Offline:
                    break;
                case Connecting:
                    Log.i("Sphero", "Robot " + robot.getName() + " Connecting!");
                    break;
                case Connected:
                    Log.i("Sphero", "Robot " + robot.getName() + " Connected!");
                    break;
                // Handle other cases
                case Disconnected:
                    break;
                case FailedConnect:
                    break;
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
        if( asyncMessage instanceof DeviceSensorAsyncMessage ) {
            DeviceSensorAsyncMessage message = (DeviceSensorAsyncMessage) asyncMessage;

            if( message.getAsyncData() == null
                    || message.getAsyncData().isEmpty()
                    || message.getAsyncData().get( 0 ) == null )
                return;

            //Retrieve DeviceSensorsData from the async message
            DeviceSensorsData data = message.getAsyncData().get( 0 );

            //Extract the accelerometer data from the sensor data
            acelerometro.setText(data.getAccelerometerData().toString());
            altitud.setText(data.getAttitudeData().toString());
            orientacion.setText(data.getQuaternion().toString());
            back.setText(data.getBackEMFData().getEMFFiltered().toString());
            giroscopio.setText(data.getGyroData().toString());


        }
    }

    @Override
    public void onClick(View v) {
        if (_robot == null)
            return;

        switch (v.getId()) {


            case R.id.procedimiento1: {
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
                break;
        }
    }


    @Override
    public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType robotChangedStateNotificationType) {

    }
}