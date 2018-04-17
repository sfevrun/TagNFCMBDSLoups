package org.mbds.android.tagnfc;

import java.io.File;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * Attention, necessite au moins l'API 17 ou 18
 * Lecture ou ecriture d'un tag NFC au format NDEF
 */
public class NFCReadWriteActivity extends Activity {

	public static final String TAG = "NFCReaderActivity";

	public static final int REQUEST_CODE = 1000;
    public final static String MESSAGE = "message";
    // �criture de tag
    private static String message = "";
    private static NdefMessage ndefMsg = null;
    // d�tection de tag
    private NfcAdapter nfcAdapter = null;
 	private PendingIntent mPendingIntent;
	private IntentFilter ndefDetected;

    /**
     * Create the activity
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(TAG);
		setContentView(R.layout.nfcreading);
		//Initialiser l'adaptateur NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        File myFile = new File("");
		//Ne pas �craser un message qui n'a pas encore �t� �crit
		//en attente de d�tection du tag � encoder...
		if (message.equals("")) {
	        try {
	            Bundle bundle = this.getIntent().getExtras();
	            message = bundle.getString(MESSAGE, "");
	            //Cr�er le message � �crire
	    		//Utilisation de la m�thode cr�e pr�c�demment :
	            //On ins�re le domaine (URL) pour que le tag soit d�tect� 
	            //par cette appli en priorit� (cf. manifeste)=> dans notre 
	            //exemple, nous n'utiliserons pas le type mime...
	    		ndefMsg = createNdefMessage(message, "text/plain");
	    		//Passer le message Ndef, ainsi que l�activit� en cours � l�adaptateur :
	    		//Si un p�riph�rique NFC est en proximit�, le message sera envoy� en mode passif
	    		//(ne fonctionne pas pour un tag passif)
	    		nfcAdapter.setNdefPushMessage(ndefMsg, this);      
	    	} catch (Exception e) {
	            // pas de message :)
	    		// l'activit� a �t� activ�e suite � la d�tection 
	    		// d'un p�riph�rique NFC qui contient une URI http://www.mbds-fr.org
	        }
		}
	}
	
	/**
	 * Create Ndef message to write to a pasive tag or send by Beam
	 * @param text
	 * @param mimeType
	 * @return
	 */
	public NdefMessage createNdefMessage(String text, String mimeType)
	{
		//Message de type MIME270
			//NdefRecord.createMime(mimeType, text.getBytes())
			//Autre m�thode (qui fait la m�me chose)...
			//NdefRecord(NdefRecord.TNF_MIME_MEDIA,	mimeType.getBytes(), new byte[0], text.getBytes())
			//Message de type application :
			//permet de lancer l�application qui recevra le tag
			//NdefRecord.createApplicationRecord("com.mbds.android.tagnfc"))
			//Message de type URI
			NdefRecord record = createRecord("UTF-8", text);
			NdefMessage msg = new NdefMessage(record);
		return msg;
	}
	public NdefRecord createRecord(String charset, String message)
	{
		byte[] langBytes = Locale.ENGLISH.getLanguage().getBytes(Charset.forName("US-ASCII"));
		byte[] textBytes = message.getBytes(Charset.forName(charset));
		char status = (char) (langBytes.length);
		byte[] data = new byte[1 + langBytes.length + textBytes.length];
		data[0] = (byte) status;
		System.arraycopy(langBytes, 0, data, 1, langBytes.length);
		System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
		message=message.trim();
		System.out.println("je vais envoyer message="+message);
		NdefRecord rec;
		rec =new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
		return rec;
	}

	/**
	 * read the content of the tag
	 * @param intent
	 */
	public void readTag(Intent intent) {
		//Lecture
		//R�cup�ration des messages
		Parcelable[] rawMsgs = 
	            intent.getParcelableArrayExtra(
	                NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] msgs;
		//Les enregistrements peuvent �tre imbriqu�s, 
		//mais ce n'est pas notre utilisation
		String receivedMessages = "";
		if (rawMsgs != null) {
			try {
				msgs = new NdefMessage[rawMsgs.length];
					for (int i = 0; i < rawMsgs.length; i++) {
						msgs[i] = (NdefMessage) rawMsgs[i];
						NdefRecord record = msgs[i].getRecords()[i];
						//Infos sur le tag...
						//byte[] idRec = "".getBytes();
						//short tnf = 0;
						byte[] type = "".getBytes();
						try {
							//idRec = record.getId();
							//tnf = record.getTnf();
							type = record.getType();
						} catch (Exception e) {
							// Les infos du tag sont incompl�tes
						}
						//Message contenu sur le tag sous forme d'URI
						if (Arrays.equals(type, NdefRecord.RTD_SMART_POSTER) ||
							Arrays.equals(type, NdefRecord.RTD_URI) ||
							Arrays.equals(type, NdefRecord.RTD_TEXT)) {
							String str = new String(record.getPayload());
							receivedMessages += str.substring(6);
						}
					}
			} catch (Exception e) {
				// Le contenu du tag est mal form�
				Toast.makeText(this, "NDEF type not managed!..", 
						Toast.LENGTH_LONG).show();
			}
		}
		// Affichage dans l'activit� TagActivity
		Bundle bundle = new Bundle();
	    bundle.putString(MESSAGE, receivedMessages);
	    Intent main = new Intent(getBaseContext(), UIActivity.class);
	    main.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	    main.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    main.putExtras(bundle);
	    startActivity(main);
		finish();
	}

	// M�thode invoqu�e lors de la d�tection d'un p�riph�rique NFC
	// (Beam)
	@Override
	public void onNewIntent(Intent intent) {
		//D�tection d'un p�riph�rique NFC (tag ou autre)
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
			NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
			NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			resolveIntent(intent) ;
		}
	}
	
	/**
	 * un tag a �t� d�tect�, soit il faut �crire, soit il faut lire
	 * @param intent
	 */
	private void resolveIntent(Intent intent) {
		if (!message.equals("")) {
			writeTag(intent);
		} else {
			readTag(intent);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// - Soit il y a un message � �crire, 
		// - soit un tag NFC a �t� d�tect� !
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		// Intent filters 
		ndefDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		Intent intent = getIntent();
		//Lecture/Ecriture?
		resolveIntent(intent);
	}

	/**
	 * write the message on the detected tag
	 * @param msg
	 * @param tag
	 * @return
	 */
	public void writeTag(Intent intent) {
		boolean isWritable = false;
		Tag tag = null;
		Ndef ndef = null;
//		int content = -1;
//		String[] technologies = null;

		try {
			 //Infos sur le tag
			tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//			technologies = tag.getTechList();
//			content = tag.describeContents();
			ndef = Ndef.get(tag);
			isWritable = ndef.isWritable();
//			boolean canMakeReadOnly = ndef.canMakeReadOnly();
//			byte[] id =tag.getId();
		} catch (Exception e) {
			// il ne s'agit pas d'un tag.... (p�riph�rique NFC ?)
		}
		
		boolean done = false;
		
		if (tag!=null && ndefMsg!=null && isWritable) {
			try {
				int size = ndefMsg.toByteArray().length;
				if (ndef != null) {
					ndef.connect();
					if (!ndef.isWritable()) {
						// verrouill� en �criture
						Toast.makeText(this, "Le tag NFC est verrouill� en �criture !", Toast.LENGTH_LONG).show();
					} else if (ndef.getMaxSize() < size) {
						// Capacit� insuffisante
						Toast.makeText(this, "La capacit� du tag NFC est insuffisante !", Toast.LENGTH_LONG).show();
					} else {
						// �criture
						ndef.writeNdefMessage(ndefMsg);
						done = true;
						ndef.close();
					}
				} else {
					//Tags qui n�cessitent un formatage :
					NdefFormatable format =	NdefFormatable.get(tag);
					if (format != null) {
						//Inspir� de la source � cette URL : 
						//http://www.jessechen.net/blog/how-tonfc-on-the-android-platform
						try {
							format.connect();
							//Formatage et �criture du message:
							format.format(ndefMsg);
							done=true;
							//ou en verrouillant le tag en �criture :
							//formatable.formatReadOnly(message);
							format.close() ;
						} catch (IOException e) {
						}
					} else {
					}
				}
			} catch (Exception e) {
				Log.d("WriteTag Exception", e.getMessage());
			}
			if (done) {
				Toast.makeText(this, "Le message a �t� correctement �crit sur le tag", Toast.LENGTH_LONG).show();
			} else {
				// le message n'a pas �t� �crit
				Toast.makeText(this, "ERREUR : Le message n'a pas �t� �crit sur le tag !", Toast.LENGTH_LONG).show();
			}
			// R�affichage dans l'activit� TagActivity
			Intent main = new Intent(this, UIActivity.class);
			Bundle bundle = new Bundle();
		    bundle.putString(MESSAGE, message);
		    main.putExtras(bundle);
		    main.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		    main.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	        startActivity(main);
		    // initialisation : plus de message � �crire
			message = "";
	        finish();
		}
	}
	
	/**
	 * Cancellation: user has clicked the return button
	 */
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
