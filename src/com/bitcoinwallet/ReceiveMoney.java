package com.bitcoinwallet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

public class ReceiveMoney extends Activity {

	private QRCodeEncoder qrCodeEncoder;
	int dimension;
	Address receiveAddress;
	String amount;
	EditText amountField;
	ApplicationState appState;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.receive_money);
		
		appState = ApplicationState.current;
		
		//prevent keyboard from opening
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		dimension = getScreenWidth() * 7 / 8;
		
		ECKey key = appState.wallet.keychain.get(0);
		receiveAddress = key.toAddress(appState.params);
		TextView addressField = (TextView) findViewById(R.id.address);
		addressField.setText(receiveAddress.toString());
		
		generateQRCode(generateBitCoinURI());
		
		amountField = (EditText) findViewById(R.id.receive_amount);
		amountField.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { 
            	if(amount != s.toString()){
        			amount = s.toString();
        			generateQRCode(generateBitCoinURI());
        		}
            }
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                //do nothing
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //do nothing
            }
        });
		
		Button emailButton = (Button)this.findViewById(R.id.email_button);
		emailButton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            	emailIntent.setType("text/plain");
            	emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Payment Request From: "+ receiveAddress);
            	String message = "Hello,\n\nYou've received a bitcoin payment request.  Please send funds to the following address.";
            	message += "\n\nAddress: "+ receiveAddress;
            	if(amount != null && amount != ""){
            		message += "\nAmount:  "+amount+" BTC";
            	}
            	message += "\n\nThank you!";
            	
            	emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message );
            	ReceiveMoney.this.startActivity(Intent.createChooser(emailIntent, "Send Bitcoin Address..."));
	        }
	    });
	}
	
	private int getScreenWidth(){
		Display display = getWindowManager().getDefaultDisplay(); 
		return display.getWidth();
	}
	
	private String generateBitCoinURI(){
		String uri = "bitcoin:";
		uri += receiveAddress.toString() + "?";
		if(amount != null && amount.length() > 0){
			uri += "amount=" + amount;
		}
		return uri;
	}
	
	private void generateQRCode(String data) {
		Intent intent = new Intent(Intents.Encode.ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
        intent.putExtra(Intents.Encode.DATA, data);
        intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
        
        qrCodeEncoder = new QRCodeEncoder(ReceiveMoney.this, intent);
        qrCodeEncoder.requestBarcode(handler, dimension);
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case R.id.encode_succeeded:
				Bitmap image = (Bitmap) message.obj;
				ImageView view = (ImageView) findViewById(R.id.qr_code);
				view.setImageBitmap(image);
				//TextView contents = (TextView) findViewById(R.id.contents_text_view);
				//contents.setText(qrCodeEncoder.getDisplayContents());
				break;
			case R.id.encode_failed:
				Toast.makeText(ReceiveMoney.this,
						"QR generation failed",
						Toast.LENGTH_LONG).show();
				qrCodeEncoder = null;
				break;
			}
		}
	};
}
