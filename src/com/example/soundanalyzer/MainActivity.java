package com.example.soundanalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;





import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	private static final String TAG = "CameraTestActivity";

	private static final int POLL_INTERVAL = 300;
	private Handler mHandler = new Handler();
	
	private SoundMeter mSensor;
	private PowerManager.WakeLock mWakeLock;
	private Camera camera;
	private Camera mCamera;
	
	
	double amp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSensor = new SoundMeter();
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NoiseAlert");
	}
	
	private Runnable mPollTask = new Runnable() {
        public void run() {
                amp = mSensor.getAmplitude();
                //Log.i("Noise Alert", "Value : " + String.valueOf(amp));
                
                ((TextView) findViewById(R.id.amplitude_value_text)).setText("Value : " + String.valueOf(amp));
                mHandler.postDelayed(mPollTask, POLL_INTERVAL);
//                try {
//					capture();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
                
                
               
                	
        }
};

	private void capture() throws IOException{
		
		if(amp > 5){
			camera.takePicture(shutterCallback, rawCallback, jpegCallback);
			Toast.makeText(getApplicationContext(), "Photo Captured", Toast.LENGTH_LONG).show();			               }
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!mWakeLock.isHeld()) 
            mWakeLock.acquire();
		try {
			mSensor.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mHandler.postDelayed(mPollTask, POLL_INTERVAL);
	
		int numCams = Camera.getNumberOfCameras();
		if(numCams > 0){
			try{
				camera = Camera.open(0);
				camera.startPreview();
				setCamera(camera);
			} catch (RuntimeException ex){
			
			}
		}
	
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (mWakeLock.isHeld()) 
            mWakeLock.release();
		mHandler.removeCallbacks(mPollTask);
		mSensor.stop();
	}
	
	public void setCamera(Camera camera) {
    	mCamera = camera;
    	if (mCamera != null) {

    		// get Camera parameters
    		Camera.Parameters params = mCamera.getParameters();

    		List<String> focusModes = params.getSupportedFocusModes();
    		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
    			// set the focus mode
    			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    			// set Camera parameters
    			mCamera.setParameters(params);
    		}
    	}
    }
	
	private void resetCam() {
		camera.startPreview();
		
	}

	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}
	
	ShutterCallback shutterCallback = new ShutterCallback() {
		@Override
		public void onShutter() {
			//			 Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			//			 Log.d(TAG, "onPictureTaken - raw");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			new SaveImageTask().execute(data);
			resetCam();
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};

	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

		@Override
		protected Void doInBackground(byte[]... data) {
			FileOutputStream outStream = null;

			// Write to SD Card
			try {
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File (sdCard.getAbsolutePath() + "/camtest");
				dir.mkdirs();				

				String fileName = String.format("%d.jpg", System.currentTimeMillis());
				File outFile = new File(dir, fileName);

				outStream = new FileOutputStream(outFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();

				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

				refreshGallery(outFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			return null;
		}

	}
}

