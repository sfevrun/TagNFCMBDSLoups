package org.mbds.android.tagnfc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class UIActivity extends Activity {
	public static final String TAG = "TagActivity";
	private ListView mainListView ;
	private ArrayAdapter<String> listAdapter ;
	String[] messageNfcListe;
	ArrayList<String> arrayNfcList;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(TAG);
		setContentView(R.layout.activity_main);
        Button btnShare = (Button) findViewById(R.id.buttonShare);
        Button btnClear = (Button) findViewById(R.id.buttonClear);
        btnShare.setOnClickListener(new OnClickListener()
        {
        	public void onClick(View v)
        	{
              // Recuperer le texte saisi par l'utilisateur
        	   String message = ((EditText) findViewById(R.id.textViewTagNFC)
        			  ).getText().toString();
        	  if (message!=null & !message.trim().isEmpty()) {

				  Time time = new Time();
				  time.setToNow();
				   arrayNfcList.add("Me : " + message + " : Heure :"+ time.format("%H:%M:%S"));
	              Bundle bundle = new Bundle();
	              bundle.putString(NFCReadWriteActivity.MESSAGE, message);
	              Intent nfcReader = new Intent(getBaseContext(), NFCReadWriteActivity.class);
	              nfcReader.putExtras(bundle);
	              startActivity(nfcReader);

				  listAdapter.notifyDataSetChanged();
        	  } else {
        		  Toast msg = Toast.makeText(getBaseContext(),
                          "Vous devez renseigner le message, SVP !..", 
                          Toast.LENGTH_LONG);
        		  msg.show();
        	  }
        	}
        	                                
        });
        btnClear.setOnClickListener(new OnClickListener()
        {
        	public void onClick(View v)
        	{
                // Effacer la zone de texte
        	  EditText txt = (EditText) findViewById(R.id.textViewTagNFC);
        	  txt.setText("");
        	  txt.invalidate();
         	}
        	                                
        });

		messageNfcListe = new String[] { "Initialisation NFC"};
		mainListView = (ListView) findViewById( R.id.idListeView);
	   arrayNfcList = new ArrayList<String>();
		arrayNfcList.addAll(Arrays.asList(messageNfcListe));
		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayNfcList);
		mainListView.setAdapter(listAdapter );

	}
	

	@Override
	public void onResume() {
		super.onResume();
		//initialisation
		String message = "";
        try {
            Bundle bundle = this.getIntent().getExtras();
            message = bundle.getString(NFCReadWriteActivity.MESSAGE, "");
            if (!message.equals("")) {
				Time time = new Time();
				time.setToNow();
				arrayNfcList.add("You : " + message + " : Heure :"+ time.format("%H:%M:%S"));

				listAdapter.notifyDataSetChanged();
            	((EditText) findViewById(R.id.textViewTagNFC)
          			  ).setText(message);
            }
        } catch (Exception e) {
        	//Pas de message & afficher
        }


	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			setResult(RESULT_CANCELED);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
