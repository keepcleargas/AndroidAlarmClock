/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package androidIOAlarmClock2.sample;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.widget.TextView;
import android.widget.Toast;
import android_serialport_api.SerialPort;

/**
 * 
 * Mostly controls reading and writing to and from serialport as well as contains the clockpanel.
 * Whereas clickpanel mostly does drawing and small level stuff, this class handles data
 * transmission and higher level tasks.
 *
 */
public class ConsoleActivity extends Activity {

	TextView text;
	static ReadWriteThread mReadThread;
	static OutputStream mOutputStream;
	static InputStream mInputStream;
	static protected Application mApplication;
	static protected SerialPort mSerialPort;
	DataStore dataStore;
	boolean watchStarted=false;
	public static int width, height;
	public static boolean svState1=false, svState2=false, svState3=false, svState4=false;
	public static Time svClockTime, svAlarmTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
		////ROOT ACCESS
	    Process p;  
	    try {  
	       // Preform su to get root privledges  
	       p = Runtime.getRuntime().exec("su");   
	      
	       // Attempt to write a file to a root-only  
	       DataOutputStream os = new DataOutputStream(p.getOutputStream());  
	       os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n");  
	      
	       // Close the terminal  
	       os.writeBytes("exit\n");  
	       os.flush();  
	       try {  
	          p.waitFor();  
	               if (p.exitValue() != 255) {  
	                  // TODO Code to run on success  
	            	   Toast.makeText(getApplicationContext(), "root", Toast.LENGTH_SHORT).show(); 
	               }  
	               else {  
	                   // TODO Code to run on unsuccessful  
	            	   Toast.makeText(getApplicationContext(), "not root", Toast.LENGTH_SHORT).show();  
	               }  
	       } catch (InterruptedException e) {  
	          // TODO Code to run in interrupted exception  
	    	   Toast.makeText(getApplicationContext(), "not root", Toast.LENGTH_SHORT).show();
	       }  
	    } catch (IOException e) {  
	       // TODO Code to run in input/output exception  
	    	Toast.makeText(getApplicationContext(), "not root", Toast.LENGTH_SHORT).show();
	    }  
		
		
		
		
		//handle displaying after root powers have been gained
		Display display = getWindowManager().getDefaultDisplay();
		width = display.getWidth();
        height = display.getHeight();
		setContentView(new ClockPanel(this)); 
		Alarm.activity=this;
	}
	
	/**
	 * Redraws the screen and starts the alarm counters over again if the screen shuts off. This is tricky because
	 * we need to continue the alarm and counters if they were occuring before the shutoff.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Display display = getWindowManager().getDefaultDisplay();
		width = display.getWidth();
        height = display.getHeight();
		setContentView(new ClockPanel(this,svState1,svState2,svState3,svState4,svClockTime,svAlarmTime)); 
		Alarm.activity=this;
	}
	
	/**
	 * Start a new alarm that makes a noise to wake user up
	 */
	public static void newAlarm() {
		if(ClockPanel.toggleButtons[2].getState()==false) {
			ClockPanel.toggleButtons[2].clicked();
		}
		Alarm.play();
	}
	
	/**
	 * Connect to the port to read and write to USB of the TI Chronos watch.
	 * 
	 * @param portPath
	 * @param baudRate
	 * @throws SecurityException
	 * @throws IOException
	 */
	public void connect(String portPath, int baudRate) throws SecurityException, IOException{
    	File device = new File(portPath);
    	// Asking for permission of R/W to the device via system command as superuser
    	if(!device.canRead() || !device.canWrite())
    	{
    	//if (D) Log.d(TAG, " asking for w/r permission ");
    		try{
    			Process su;
    			su = Runtime.getRuntime().exec("su");
    			String cmd = "chmod 777 " + device.getAbsolutePath() + "\n" + "exit\n";
    			su.getOutputStream().write(cmd.getBytes());
    			if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
    				throw new SecurityException();
    			}
    		}catch (Exception e) {
    			e.printStackTrace();
    			throw new SecurityException();
    		}
    	}

    	SerialPort mSerialPort = new SerialPort(device, baudRate, 0);
    	mOutputStream = mSerialPort.getOutputStream();
    	mInputStream = mSerialPort.getInputStream();
    	
    	mOutputStream.write(new byte[]{(byte)0xFF,0x07,0x03});// which is FF 07 03
    	byte[] recvData = new byte[7];
		mInputStream.read(recvData);
		
		mReadThread=ReadWriteThread.getInstance();
		ReadWriteThread.setStuff(this,mInputStream,mOutputStream);
		mReadThread.start();
		
    }
	
	/*public void startWatch() {
		Log.d("MyApp","start watch");
		if(watchStarted) return;
		watchStarted=true;
		//get references
		mApplication = (Application) getApplication();
		try {
			mSerialPort = mApplication.getSerialPort();
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();
			
			mOutputStream.write(new byte[]{(byte)0xFF,0x07,0x03});
			
			mReadThread=ReadWriteThread.getInstance();
			ReadWriteThread.setStuff(this,mInputStream,mOutputStream);
			mReadThread.start();
		} catch (Exception e) {
			//Looper.prepare();
			//Toast.makeText(this, "You must connect your watch. Read the application description for instructions.", Toast.LENGTH_SHORT).show();
		}
		
	}*/

	public void onDataRecieved() {
		runOnUiThread(new Runnable() {
			public void run() {
				//DataStore.getAverageDivergence(text);
				//DataStore.findShape(text);
				//DataStore.displayPosition(text);
			}
		});
	}

	@Override
	protected void onDestroy() {
		/*Log.d("MyApp","asdfjsdhfjhsjkfjksgfkdfjkhasdhfjksd");
		if (mReadThread != null)
			mReadThread.interrupt();
		Alarm.interrupt();
		ClockPanel.interruptThread();
		mApplication.closeSerialPort();
		mSerialPort = null;
		Log.d("MyApp","asdfjsdhfjhsjkfjksgfkdfjkhasdhfjksd");
		*/super.onDestroy();
	}
}
