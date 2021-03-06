package com.coreblu.sample;

//import com.crashlytics.android.Crashlytics;
//import io.fabric.sdk.android.Fabric;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import coreblu.SDK.Beacons.AnyBeacon;
import coreblu.SDK.Beacons.Beacon;
import coreblu.SDK.Beacons.iBeacon;
import coreblu.SDK.CorebluDevice.CorebluDeviceManager;
import coreblu.SDK.CorebluDevice.CorebluDeviceManager.AnyBeaconListener;
import coreblu.SDK.CorebluDevice.CorebluDeviceManager.IBeaconsInRegionListner;
import coreblu.SDK.CorebluDevice.CorebluDeviceManager.iBeaconListener;
import coreblu.SDK.CorebluDevice.Region;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

	private Button startIbeaconScan,startAllBeaconScan,startScanningInRegion;
	private boolean scanningAllBeacons=false,scanningIbeacons=false,scanningInRegion = false;
	private CorebluDeviceManager mCorebluDeviceManager;
	private ListAdapter anyBeaconAdapter;
	private ListAdapterIbeacon mListAdapterIbeacon;
	private ListView listv;
	private final String TAG = getClass().getSimpleName();

	private final String stopIbeaconScan = "Please stop ibeacon scan first";
	private final String stopAllBeaconScan = "Please stop all beacon scan first";
	private final String stopInRegionBeaconScan = "Please stop in region beacon scan first";
	private final int REQUEST_ENABLE_BT = 1;
	private TextView count;

	Timer T;

	/**
	 * Checks whether the Bluetooth adapter is enabled.
	 */
	private boolean isBleEnabled() {
		final BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		final BluetoothAdapter ba = bm.getAdapter();
		return ba != null && ba.isEnabled();
	}

	/**
	 * Checks whether the device supports Bluetooth Low Energy communication
	 * 
	 * @return <code>true</code> if BLE is supported, <code>false</code> otherwise
	 */
	private boolean ensureBleExists() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Unsupported Device..", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	/**
	 * Tries to start Bluetooth adapter.
	 */
	private void enableBle() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}


	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode != RESULT_OK) {
				finish();
			} 
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Fabric.with(this, new Crashlytics());

		setContentView(R.layout.activity_main);
		T=new Timer();
		T.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				//count.setText("count="+countt);
				//Log.i("Timer Loop" ,"Tick ="+MyVar.myInt);
				MyVar.myInt++;
				MyVar.beacon3LastSeen++;
			}
		}, 1000, 1000);


		if (ensureBleExists()  && !isBleEnabled())
		{
			enableBle();
		}

		mCorebluDeviceManager =CorebluDeviceManager.getInstance(getApplicationContext());
		//mCorebluDeviceManager.StartHealthMonitoring(30);
		
		mListAdapterIbeacon = new ListAdapterIbeacon(this , new ArrayList<iBeacon>() , R.layout.beacon_list_layout);
		anyBeaconAdapter = new ListAdapter(this , new ArrayList<AnyBeacon>() , R.layout.beacon_list_layout);


		listv = (ListView)findViewById(R.id.beacon_listview);
		listv.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int pos, long id) {
				// TODO Auto-generated method stub
				if(listv.getAdapter() instanceof ListAdapterIbeacon)
				{
					iBeacon ib = mListAdapterIbeacon.getobj(pos);
					if(ib.getType().equals(Beacon.BEACON_TYPE_COREBLU_IBEACON))
					{
						if(!ib.isConnectable())
						{
							showToast("Beacon is not connectable");
							return true;
						}
						stopScan();
						ShowConfigDialog(ib);
					}
					else
					{
						showToast("Only coreblu ibeacons are configurable..");
					}
				}
				else
				{
					showToast("Only coreblu ibeacons are configurable..");
				}
				return true;
			}
		});

		//listv.setAdapter(mListAdapterIbeacon);
		//startScanIBeaconsInRegion();

		startScanningInRegion = (Button) findViewById(R.id.sinregion);
		startScanningInRegion.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//Another Scan is running
				if(scanningIbeacons)
				{
					showToast(stopIbeaconScan);
					return;
				}
				//Another Scan is running
				else if(scanningAllBeacons)
				{
					showToast(stopAllBeaconScan);
					return;
				}

				//Scan is already started
				if(scanningInRegion)
				{
					stopScan();
				}

				else
				{
					//Starting Scan

					listv.setAdapter(mListAdapterIbeacon);
					startScanIBeaconsInRegion();

				}
			}
		});






	}


	private void stopScan()
	{
		mCorebluDeviceManager.stopScan();




		if(scanningInRegion)
		{
			scanningInRegion = false;
			startScanningInRegion.setText("Start Tracking");
		}
	}



	private void startScanIBeaconsInRegion()
	{
		ArrayList<Region> regionsToMonitor = new ArrayList<Region>();
		
		regionsToMonitor.add(new Region("my room", null,2, null));
		
		scanningInRegion=true;
		startScanningInRegion.setText("Stop Tracking");
		mListAdapterIbeacon.clear();
		mCorebluDeviceManager.startiBeaconScan(new IBeaconsInRegionListner() {
			@Override
			public void IBeaconsInRegion(Collection<iBeacon> beacons , Region region) {
				// TODO Auto-generated method stub
				//Log.i("Beacons Found" ,"Beacon Count:"+beacons.size()+" Region:"+region.getName());
				for(iBeacon beacon: beacons)
				{
					if(beacon.getType().equals(Beacon.BEACON_TYPE_COREBLU_IBEACON))
					{
						//Log.i("Battery Voltage","MAC:"+beacon.getMacAddress()+" Voltage="+beacon.getbatteryVoltage());
					}

					
				}
				mListAdapterIbeacon.add(beacons);

			}

			@Override
			public void didExitRegion(Region region) {
				// TODO Auto-generated method stub
				//showToast("Exit Region:"+region.getName());
				
			}

			@Override
			public void didEnterRegion(Region region) {
				// TODO Auto-generated method stub
				//showToast("Enter Region:"+region.getName());
			}
			
			@Override
			public void onError(int errorType) {
				// TODO Auto-generated method stub
				handleError(errorType);
			}
			
		} ,regionsToMonitor);
	}

	private void showToast(String msg)
	{
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mCorebluDeviceManager.stopScan();
	}




	private void ShowConfigDialog(final iBeacon ib) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Configuration");
		builder.setMessage("Do you want to configure Device:"+ib.getMacAddress());
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		builder.setPositiveButton("Configure",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				Intent i = new Intent(MainActivity.this , WriteConfiguration.class);
				i.putExtra("device", ib.getDevice());
				startActivity(i);
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	private void handleError(int error){
		
//		switch (error){
//		case CorebluDeviceManager.ERROR_BLE_NOT_AVAILABLE:
//			showToast("Ble not available on device");
//			break;
//		case CorebluDeviceManager.ERROR_BLUETOOTH_DISABLED:
//			showToast("bluetooth disabled");
//			enableBle();
//			break;
//			default:
//				break;
//		}
	}
}
