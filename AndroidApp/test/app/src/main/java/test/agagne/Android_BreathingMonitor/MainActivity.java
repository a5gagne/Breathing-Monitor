package test.agagne.Android_BreathingMonitor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

@SuppressLint("HandlerLeak")
private final Handler mHandler = new Handler(){
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch(msg.what){
            case Bluetooth.SUCCESS_CONNECT:
                Bluetooth.connectedThread = new Bluetooth.ConnectedThread((BluetoothSocket)msg.obj);
                Bluetooth.connectedThread.start();
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                break;
            case Bluetooth.MESSAGE_READ:
                // Get data as string
                byte[] readBuff = (byte[])msg.obj;
                String strInc = new String(readBuff,0,5);

                // If 's' in position 0 and '.' in position 2:
                // Remove header, convert string to double and append to graph
                // Check if it's a float:
                // Convert to double and add to graph
                if(strInc.indexOf('s')==0 && strInc.indexOf('.')==2){
                    strInc = strInc.replace("s","");
                    if(isfloatnumber(strInc)){
                       Series.appendData(new DataPoint(xLastValue,Double.parseDouble(strInc)),Autoscroll,1000);
                    }
                xLastValue += 0.01;
                }
                break;
        }

}

    private boolean isfloatnumber(String strInc) {
        try{
            Double.parseDouble(strInc);
        }catch (NumberFormatException nfe){
            return false;
        }
        return true;
    }
};

Button bConnect, bDisconnect, bXMinus, bXPlus;
ToggleButton tbStream, tbScroll, tbLock;
static boolean Lock, Autoscroll, Stream;

static GraphView graphView;
static LineGraphSeries<DataPoint> Series;
private static double xLastValue = 0;
// private static int Xview = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set orientation, background
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        LinearLayout background = (LinearLayout)findViewById(R.id.bg);
        background.setBackgroundColor(Color.BLACK);

        buttoninit();
        Bluetooth.gethandler(mHandler);

        // Initialize graph
        graphView = (GraphView)findViewById(R.id.Graph);
        Series = new LineGraphSeries<>();
        Series.setColor(Color.GREEN);
        Series.setThickness(2);
        graphView.addSeries(Series);
        Viewport viewport = graphView.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setScalable(true);
        viewport.setMinY(1.4);
        viewport.setMaxY(2.0);
        viewport.setScrollable(true);
    }

    private void buttoninit() {
        bConnect = (Button)findViewById(R.id.bConnect);
        bConnect.setOnClickListener(this);
        bDisconnect = (Button)findViewById(R.id.bDisconnect);
        bDisconnect.setOnClickListener(this);
        bXMinus = (Button)findViewById(R.id.bXMinus);
        bXMinus.setOnClickListener(this);
        bXPlus = (Button)findViewById(R.id.bXPlus);
        bXPlus.setOnClickListener(this);

        tbLock = (ToggleButton)findViewById(R.id.tbLock);
        tbLock.setOnClickListener(this);
        tbScroll = (ToggleButton)findViewById(R.id.tbScroll);
        tbScroll.setOnClickListener(this);
        tbStream = (ToggleButton)findViewById(R.id.tbStream);
        tbStream.setOnClickListener(this);

        Lock = true;
        Autoscroll = true;
        Stream = false;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.bConnect:
                startActivity(new Intent("android.intent.action.BT1"));
                break;
            case R.id.bDisconnect:
                Bluetooth.disconnect();
                break;
            case R.id.bXMinus:

                break;
            case R.id.bXPlus:

                break;
            case R.id.tbScroll:
                if(tbScroll.isChecked()){
                    Autoscroll = true;
                }
                else{
                    Autoscroll = false;
                }
                break;
            case R.id.tbStream:
                if(tbStream.isChecked()){
                    // Start the stream
                    if(Bluetooth.connectedThread != null) Bluetooth.connectedThread.write("E");
                }
                else{
                    // Stop the stream
                    if(Bluetooth.connectedThread != null) Bluetooth.connectedThread.write("Q");
                }
                break;
            case R.id.tbLock:
                if(tbLock.isChecked()){
                    Lock = true;
                }
                else{
                    Lock = false;
                }
                break;
        }
    }
}
